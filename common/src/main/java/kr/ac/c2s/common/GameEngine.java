package kr.ac.c2s.common;

import java.util.*;
import static kr.ac.c2s.common.Protocol.*;

/** 서버가 소유하는 게임 상태. Swing/libGDX와 무관해 독립적으로 검증할 수 있다. */
public final class GameEngine {
    private final Random random;
    private final Player[] players;
    private final boolean[][] used = new boolean[2][10], bullets = new boolean[2][CHAMBERS];
    private final List<Card>[] cards;
    private final List<String>[] history, info, hints, hintHistory;
    private final Map<Integer, Boolean>[] known;
    private final int[] dangerDeltas = new int[2], pendingAmmo=new int[2], ammoChoices={-1,-1};
    private final int[] tiles = new int[2], predictions = new int[2], chambers = {-1, -1};
    private final Card[] active = new Card[2];
    private final boolean[] specialReady = new boolean[2], ack = new boolean[2], hit = new boolean[2];
    private final boolean[] loadoutReady = new boolean[2];
    private final String[] parity = {"?", "?"};
    private Phase phase = Phase.TILE_SELECT;
    private final String matchId = UUID.randomUUID().toString();
    private int round = 1, draws, winner = -2, roundWinner = -2;
    private String reason = "";

    public GameEngine(Player first, Player second, Random random) { this(first,second,random,false); }
    @SuppressWarnings("unchecked")
    public GameEngine(Player first, Player second, Random random, boolean chooseCards) {
        this.random = Objects.requireNonNull(random);
        players = new Player[]{new Player(first.name, first.characterId), new Player(second.name, second.characterId)};
        cards = new List[]{new ArrayList<Card>(), new ArrayList<Card>()};
        history = new List[]{new ArrayList<String>(), new ArrayList<String>()};
        info = new List[]{new ArrayList<String>(), new ArrayList<String>()};
        hintHistory = new List[]{new ArrayList<String>(),new ArrayList<String>()};
        hints = new List[]{new ArrayList<String>(), new ArrayList<String>()};
        known = new Map[]{new HashMap<Integer,Boolean>(), new HashMap<Integer,Boolean>()};
        for (int p = 0; p < 2; p++) {
            bullets[p][1]=true;
            List<Card> deck = new ArrayList<>(Arrays.asList(Card.values())); Collections.shuffle(deck, random);
            if(!chooseCards){cards[p].addAll(deck.subList(0, 2));loadoutReady[p]=true;}
        }
        if(chooseCards)phase=Phase.LOADOUT_SELECT;
    }
    public synchronized Phase phase() { return phase; }
    public synchronized String matchId() { return matchId; }
    private void require(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
    private void phase(Phase expected) { require(phase == expected, "현재 단계에서 사용할 수 없습니다."); }
    public synchronized void apply(int p, Command c) {
        require(p == 0 || p == 1, "플레이어 정보가 올바르지 않습니다.");
        require(c != null && c.action != null, "잘못된 명령입니다.");
        switch (c.action) {
            case LOADOUT -> selectLoadout(p,c.loadout);
            case TILE -> selectTile(p, c.number);
            case SPECIAL -> useCard(p, c.card, c.number);
            case NEXT -> next(p);
            case LOAD_AMMO -> loadAmmo(p,c.number);
            case HINT -> buyHint(p, c.hint, c.number);
            case CHAMBER -> selectChamber(p, c.number);
            default -> throw new IllegalArgumentException("게임 명령이 아닙니다.");
        }
    }
    private void selectLoadout(int p,List<Card> chosen){
        phase(Phase.LOADOUT_SELECT);
        require(!loadoutReady[p],"이미 특수카드를 확정했습니다.");
        require(chosen!=null&&chosen.size()==2,"특수카드를 정확히 두 장 선택하세요.");
        require(chosen.stream().allMatch(Objects::nonNull)&&new HashSet<>(chosen).size()==2,"서로 다른 카드 두 장을 선택하세요.");
        cards[p].addAll(chosen);loadoutReady[p]=true;
        if(loadoutReady[0]&&loadoutReady[1])phase=Phase.TILE_SELECT;
    }
    private void selectTile(int p, int tile) {
        phase(Phase.TILE_SELECT);
        require(tiles[p] == 0, "이미 타일을 확정했습니다.");
        require(tile >= 1 && tile <= 9 && !used[p][tile], "남아 있는 타일을 선택하세요.");
        tiles[p] = tile;
        if (tiles[0] != 0 && tiles[1] != 0) phase = Phase.SPECIAL_SELECT;
    }
    private void useCard(int p, Card card, int prediction) {
        phase(Phase.SPECIAL_SELECT);
        require(!specialReady[p], "이미 특수카드 선택을 확정했습니다.");
        if (card != null) {
            require(cards[p].contains(card), "보유하지 않은 특수카드입니다.");
            if (card == Card.PREDICT) require(prediction >= 1 && prediction <= 9, "예측할 숫자 1~9를 고르세요.");
            cards[p].remove(card); active[p] = card; predictions[p] = prediction;
        }
        specialReady[p] = true;
        if (specialReady[0] && specialReady[1]) resolveRound();
    }
    /** 1 대 9 예외는 역전보다 우선하며, 양쪽 역전은 상쇄한다. */
    public static int compare(int first, int second, boolean reverse) {
        if (first == second) return -1;
        if (first == 1 && second == 9) return 0;
        if (first == 9 && second == 1) return 1;
        return (reverse ? first < second : first > second) ? 0 : 1;
    }
    private void resolveRound() {
        boolean reverse = (active[0] == Card.REVERSE) ^ (active[1] == Card.REVERSE);
        roundWinner = compare(tiles[0], tiles[1], reverse);
        if (roundWinner < 0) draws++;
        else {
            int beforeWinner=players[roundWinner].danger, beforeLoser=players[1-roundWinner].danger;
            players[roundWinner].wins++;
            // Wins keep the accumulated ammunition count.
            players[1-roundWinner].danger = Math.min(CHAMBERS, players[1-roundWinner].danger + 1);
            dangerDeltas[roundWinner]=players[roundWinner].danger-beforeWinner;
            dangerDeltas[1-roundWinner]=players[1-roundWinner].danger-beforeLoser;
        }
        for (int p = 0; p < 2; p++) used[p][tiles[p]] = true;
        for (int p = 0; p < 2; p++) {
            boolean odd = tiles[1-p] % 2 != 0;
            if (active[1-p] == Card.DISGUISE) odd = !odd;
            parity[p] = odd ? "홀수" : "짝수";
            if (active[p] == Card.SCOUT) info[p].add("정찰: 상대 숫자는 " + (tiles[1-p] <= 4 ? "1~4" : "5~9") + " 범위입니다.");
            if (active[p] == Card.PREDICT) {
                if (predictions[p] == tiles[1-p]) {
                    List<Integer> remaining = hand(1-p);
                    info[p].add("예측 성공!" + (remaining.isEmpty() ? " 상대의 남은 타일이 없습니다." : " 상대의 남은 타일 중 " + remaining.get(random.nextInt(remaining.size())) + " 확인."));
                } else info[p].add("예측 실패. 정확한 숫자는 공개되지 않습니다.");
            }
            if (active[1-p] != null) info[p].add("상대가 특수카드를 사용했습니다. 종류는 비공개입니다.");
            history[p].add("R"+round+"  나 "+tiles[p]+" · 상대 "+parity[p]+" · "+(roundWinner<0?"무승부":roundWinner==p?"승리":"패배"));
        }
        phase = Phase.ROUND_RESULT;
    }
    private void next(int p) {
        require(phase == Phase.ROUND_RESULT || phase == Phase.ROULETTE_RESULT, "결과 확인 단계가 아닙니다.");
        require(!ack[p], "상대의 확인을 기다리는 중입니다."); ack[p] = true;
        if (!ack[0] || !ack[1]) return;
        Arrays.fill(ack, false);
        if (phase == Phase.ROUND_RESULT) { prepareRoulette(); return; }
        if (players[0].life <= 0 || players[1].life <= 0) {
            winner = players[0].life <= 0 && players[1].life <= 0 ? -1 : players[0].life > 0 ? 0 : 1;
            reason = "목숨 소진"; phase = Phase.FINAL; return;
        }
        if (round == 9) {
            winner = players[0].wins == players[1].wins ? -1 : players[0].wins > players[1].wins ? 0 : 1;
            reason = "9라운드 종료"; phase = Phase.FINAL; return;
        }
        round++; roundWinner=-2; Arrays.fill(dangerDeltas,0); Arrays.fill(tiles, 0); Arrays.fill(active, null); Arrays.fill(specialReady, false);
        Arrays.fill(chambers, -1); Arrays.fill(hit, false); Arrays.fill(parity, "?");
        for (int i=0;i<2;i++) { info[i].clear(); hints[i].clear(); known[i].clear(); }
        phase = Phase.TILE_SELECT;
    }
    private void prepareRoulette() {
        for (int p=0;p<2;p++) {
            pendingAmmo[p]=Math.max(0,dangerDeltas[p]);ammoChoices[p]=-1;
            chambers[p] = -1; hints[p].clear(); known[p].clear();
        }
        if(pendingAmmo[0]>0||pendingAmmo[1]>0)phase=Phase.AMMO_LOAD;else spinAmmunition();
    }
    private void loadAmmo(int p,int chamber){
        phase(Phase.AMMO_LOAD);
        require(pendingAmmo[p]>0,"추가 장전할 실탄이 없습니다.");
        require(chamber>=0&&chamber<CHAMBERS,"탄실 1~9 중 빈칸을 선택하세요.");
        require(!bullets[p][chamber],"이미 실탄이 있는 칸입니다. 빈 탄실을 선택하세요.");
        bullets[p][chamber]=true;ammoChoices[p]=chamber;pendingAmmo[p]=0;
        if(pendingAmmo[0]==0&&pendingAmmo[1]==0)spinAmmunition();
    }
    private void spinAmmunition(){
        // Rotate each real layout by a private offset, preserving the selected placement.
        for(int p=0;p<2;p++){boolean[] before=bullets[p].clone();int offset=random.nextInt(CHAMBERS);
            for(int i=0;i<CHAMBERS;i++)bullets[p][(i+offset)%CHAMBERS]=before[i];}
        phase=Phase.ROULETTE_SELECT;
    }
    private void buyHint(int p, String hint, int selected) {
        phase(Phase.ROULETTE_SELECT);
        require(chambers[p] < 0, "이미 탄실을 확정했습니다.");
        require(selected >= 0 && selected < CHAMBERS, "탄실을 먼저 선택하세요.");
        int price=hintCost(hint);
        require(!hints[p].contains(hint), "이 힌트는 이번 라운드에 이미 구매했습니다.");
        require(players[p].chips >= price, "칩이 부족합니다. 이 힌트는 "+price+"칩이 필요합니다.");
        players[p].chips -= price; hints[p].add(hint);
        int group = selected / 3 * 3;
        switch (hint) {
            case "RANGE" -> info[p].add("범위 탐색: "+(group+1)+"~"+Math.min(CHAMBERS,group+3)+"번에 실탄 "+count(p,group,Math.min(3,CHAMBERS-group))+"발.");
            case "INSPECT" -> { known[p].put(selected, bullets[p][selected]); info[p].add("탄실 검사: "+(selected+1)+"번은 "+(bullets[p][selected]?"실탄":"빈칸")+"입니다."); }
        }
        hintHistory[p].add("R"+round+" · "+info[p].get(info[p].size()-1));
    }
    private int count(int p, int start, int len) { int n=0; for(int i=0;i<len;i++) if(bullets[p][(start+i)%CHAMBERS]) n++; return n; }
    private void selectChamber(int p, int n) {
        phase(Phase.ROULETTE_SELECT);
        require(chambers[p] < 0, "이미 탄실을 확정했습니다.");
        require(n >= 0 && n < CHAMBERS, "탄실 1~9 중 하나를 선택하세요."); chambers[p] = n;
        if (chambers[0] < 0 || chambers[1] < 0) return;
        for (int i=0;i<2;i++) {
            hit[i] = bullets[i][chambers[i]];
            if (hit[i]) players[i].life--;
        }
        phase = Phase.ROULETTE_RESULT;
    }
    public synchronized void forfeit(int disconnected) {
        if (phase == Phase.FINAL) return;
        winner = 1-disconnected; reason = "상대 연결 종료"; phase = Phase.FINAL;
    }
    private List<Integer> hand(int p) { List<Integer> r = new ArrayList<>(); for(int n=1;n<=9;n++) if(!used[p][n]) r.add(n); return r; }
    /** 상대 패·상대 특수카드 종류·실탄 배치는 전송하지 않는다. */
    public synchronized Snapshot snapshot(int viewer) {
        Snapshot s = new Snapshot(); s.matchId=matchId; s.you=viewer; s.phase=phase; s.round=round;
        s.pendingAmmo=pendingAmmo[viewer];s.ammoChoice=ammoChoices[viewer];
        if(phase==Phase.AMMO_LOAD)for(int i=0;i<CHAMBERS;i++)if(bullets[viewer][i])s.loadedChambers.add(i);
        s.dangerDelta=dangerDeltas[viewer];s.draws=draws; s.winner=winner; s.reason=reason; s.roundWinner=roundWinner;
        for(int p=0;p<2;p++) {
            Player v=players[p].copy(); v.remaining=hand(p).size(); v.specialCount=cards[p].size();
            v.loadoutReady=loadoutReady[p]; v.selected=tiles[p]!=0; v.specialReady=specialReady[p]; v.acknowledged=ack[p]; v.chamberReady=chambers[p]>=0;
            s.players[p]=v;
        }
        s.hand=hand(viewer); s.cards=new ArrayList<>(cards[viewer]); s.ownTile=tiles[viewer];
        s.ownChamber=chambers[viewer]; s.opponentParity=parity[viewer]; s.hit=hit.clone();
        s.history=new ArrayList<>(history[viewer]); s.privateInfo=new ArrayList<>(info[viewer]);
        s.hintHistory=new ArrayList<>(hintHistory[viewer]);
        s.boughtHints=new ArrayList<>(hints[viewer]); s.knownChambers=new HashMap<>(known[viewer]);
        return s;
    }
}
