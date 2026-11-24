package kr.ac.c2s.common;

import java.util.*;
import static kr.ac.c2s.common.Protocol.*;

/** 서버가 소유하는 게임 상태. Swing/libGDX와 무관해 독립적으로 검증할 수 있다. */
public final class GameEngine {
    private final Random random;
    private final boolean bettingEnabled;
    private final List<String>[] chipHistory = new List[]{new ArrayList<String>(),new ArrayList<String>()};
    private final int[] bets = new int[2];
    private final boolean[] betReady = new boolean[2];
    private final Player[] players;
    private final boolean[][] used = new boolean[2][10], bullets = new boolean[2][CHAMBERS];
    private final List<Card>[] cards;
    private final List<String>[] history, info, hints, hintHistory;
    private final Map<Integer, Boolean>[] known;
    private final List<RoundObservation>[] observations;
    private final int[] dangerDeltas = new int[2], pendingAmmo=new int[2], ammoChoices={-1,-1};
    private final int[] tiles = new int[2], predictions = new int[2], chambers = {-1, -1};
    private final Card[] active = new Card[2];
    private final Boolean[] disguiseOdd = new Boolean[2];
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
        this(first,second,random,chooseCards,false);
    }
    public GameEngine(Player first, Player second, Random random, boolean chooseCards, boolean bettingEnabled) {
        this.bettingEnabled=bettingEnabled;
        this.random = Objects.requireNonNull(random);
        players = new Player[]{new Player(first.name, first.characterId), new Player(second.name, second.characterId)};
        if(bettingEnabled)for(int p=0;p<2;p++){players[p].chips=30;chipHistory[p].add("시작 보유 칩 30개");}
        cards = new List[]{new ArrayList<Card>(), new ArrayList<Card>()};
        observations = new List[]{new ArrayList<RoundObservation>(), new ArrayList<RoundObservation>()};
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
        if(chooseCards)phase=Phase.LOADOUT_SELECT;else if(bettingEnabled)phase=Phase.BET_SELECT;
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
            case BET -> placeBet(p,c.number);
            case TILE -> selectTile(p, c.number);
            case SPECIAL -> useCard(p, c.card, c.number, c.disguiseOdd);
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
        if(loadoutReady[0]&&loadoutReady[1])phase=bettingEnabled?Phase.BET_SELECT:Phase.TILE_SELECT;
    }
    private void placeBet(int p,int amount) {
        phase(Phase.BET_SELECT);
        require(!betReady[p],"이미 베팅을 확정했습니다.");
        require((amount>=1&&amount<=3)||(amount==0&&players[p].chips==0),"베팅은 1~3칩, 보유 칩이 없으면 0칩입니다.");
        require(players[p].chips>=amount,"베팅 칩이 부족합니다.");
        int before=players[p].chips;
        bets[p]=amount;betReady[p]=true;players[p].chips-=amount;
        chipHistory[p].add("R"+round+" 베팅: "+before+" - "+amount+" = "+players[p].chips+"칩 (맡긴 칩 "+amount+")");
        if(betReady[0]&&betReady[1])phase=Phase.TILE_SELECT;
    }
    private void settleBets() {
        if(!bettingEnabled)return;
        int matched=Math.min(bets[0],bets[1]);
        for(int p=0;p<2;p++){
            int refund=roundWinner<0?bets[p]:bets[p]-matched;
            int payout=roundWinner==p?2*matched:0;
            int before=players[p].chips;
            players[p].chips+=refund+payout;
            chipHistory[p].add("R"+round+" 정산: "+before+" + 반환 "+refund+" + 획득 "+payout+" = "+players[p].chips+"칩");
            info[p].add("베팅: 나 "+bets[p]+"칩 / 상대 "+bets[1-p]+"칩 · "+(roundWinner<0?"무승부, 전액 반환":roundWinner==p?"순이익 +"+matched+"칩":"손실 -"+matched+"칩")+(refund>0?" · 반환 "+refund+"칩":""));
        }
    }
    private void selectTile(int p, int tile) {
        phase(Phase.TILE_SELECT);
        require(tiles[p] == 0, "이미 타일을 확정했습니다.");
        require(tile >= 1 && tile <= 9 && !used[p][tile], "남아 있는 타일을 선택하세요.");
        tiles[p] = tile;
        if (tiles[0] != 0 && tiles[1] != 0) phase = Phase.SPECIAL_SELECT;
    }
    private void useCard(int p, Card card, int prediction, Boolean displayedOdd) {
        phase(Phase.SPECIAL_SELECT);
        require(!specialReady[p], "이미 특수카드 선택을 확정했습니다.");
        if (card != null) {
            require(cards[p].contains(card), "보유하지 않은 특수카드입니다.");
            if (card == Card.PREDICT) require(prediction >= 1 && prediction <= 9, "예측할 숫자 1~9를 고르세요.");
            cards[p].remove(card); active[p] = card; predictions[p] = prediction;
            if (card == Card.DISGUISE) disguiseOdd[p] = displayedOdd;
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
        settleBets();
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
            if (active[1-p] == Card.DISGUISE) odd = disguiseOdd[1-p] == null ? !odd : disguiseOdd[1-p];
            parity[p] = odd ? "홀수" : "짝수";
            if (active[p] == Card.SCOUT) info[p].add("정찰: 상대 숫자는 " + (tiles[1-p] <= 4 ? "1~4" : "5~9") + " 범위입니다.");
            int revealedRemaining = 0;
            if (active[p] == Card.PREDICT) {
                if (predictions[p] == tiles[1-p]) {
                    List<Integer> remaining = hand(1-p);
                    if (!remaining.isEmpty()) revealedRemaining = remaining.get(random.nextInt(remaining.size()));
                    info[p].add("예측 성공!" + (remaining.isEmpty() ? " 상대의 남은 타일이 없습니다." : " 상대의 남은 타일 중 " + revealedRemaining + " 확인."));
                } else info[p].add("예측 실패. 정확한 숫자는 공개되지 않습니다.");
            }
            RoundObservation observation = new RoundObservation();
            observation.round = round;
            observation.knownRemainingTile = revealedRemaining;
            int relativeWinner = roundWinner < 0 ? -1 : roundWinner == p ? 0 : 1;
            for (int candidate = 1; candidate <= 9; candidate++) {
                boolean displayedOdd = candidate % 2 != 0;
                // Disguise may now tell the truth or lie: its public parity cannot narrow candidates.
                if ((active[1-p] != Card.DISGUISE && displayedOdd != odd) || compare(tiles[p], candidate, reverse) != relativeWinner) continue;
                if (active[p] == Card.SCOUT && (candidate <= 4) != (tiles[1-p] <= 4)) continue;
                if (active[p] == Card.PREDICT && predictions[p] == tiles[1-p] && candidate != predictions[p]) continue;
                observation.possibleOpponentTiles.add(candidate);
            }
            observations[p].add(observation);
            if (active[1-p] != null) info[p].add("상대 사용 특수카드: "+active[1-p].label);
            history[p].add("R"+round+" · 나 "+tiles[p]+" · 상대 "+parity[p]+" · "+(roundWinner<0?"무승부":roundWinner==p?"승리":"패배")
                +"\n나 특수: "+(active[p]==null?"미사용":active[p].label)
                +"\n상대 특수: "+(active[1-p]==null?"미사용":active[1-p].label));
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
        round++; roundWinner=-2; Arrays.fill(dangerDeltas,0); Arrays.fill(tiles, 0); Arrays.fill(active, null); Arrays.fill(disguiseOdd, null); Arrays.fill(specialReady, false);
        Arrays.fill(chambers, -1); Arrays.fill(hit, false); Arrays.fill(parity, "?");
        for (int i=0;i<2;i++) { info[i].clear(); hints[i].clear(); known[i].clear(); }
        Arrays.fill(bets,0);Arrays.fill(betReady,false);
        phase = bettingEnabled?Phase.BET_SELECT:Phase.TILE_SELECT;
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
        require(hints[p].isEmpty(), "힌트는 라운드당 둘 중 하나만 구매할 수 있습니다.");
        require(players[p].chips >= price, "칩이 부족합니다. 이 힌트는 "+price+"칩이 필요합니다.");
        int before=players[p].chips;
        players[p].chips -= price; hints[p].add(hint);
        chipHistory[p].add("R"+round+" 힌트: "+before+" - "+price+" = "+players[p].chips+"칩");
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
        if(bettingEnabled&&phase==Phase.BET_SELECT)for(int p=0;p<2;p++)players[p].chips+=bets[p];
        if(bettingEnabled&&(phase==Phase.TILE_SELECT||phase==Phase.SPECIAL_SELECT)){int matched=Math.min(bets[0],bets[1]);for(int p=0;p<2;p++)players[p].chips+=bets[p]-matched;players[1-disconnected].chips+=2*matched;}
        winner = 1-disconnected; reason = "상대 연결 종료"; phase = Phase.FINAL;
    }
    private List<Integer> hand(int p) { List<Integer> r = new ArrayList<>(); for(int n=1;n<=9;n++) if(!used[p][n]) r.add(n); return r; }
    /** 상대 패·미사용 특수카드·실탄 배치는 비공개. 사용한 특수카드는 라운드 판정 후 기록에 공개한다. */
    public synchronized Snapshot snapshot(int viewer) {
        Snapshot s = new Snapshot(); s.matchId=matchId; s.you=viewer; s.phase=phase; s.round=round;
        s.chipHistory=new ArrayList<>(chipHistory[viewer]);
        s.bettingEnabled=bettingEnabled;s.ownBet=bets[viewer];
        s.opponentBet=betReady[0]&&betReady[1]?bets[1-viewer]:0;s.matchedBet=Math.min(s.ownBet,s.opponentBet);
        s.pendingAmmo=pendingAmmo[viewer];s.ammoChoice=ammoChoices[viewer];
        if(phase==Phase.AMMO_LOAD)for(int i=0;i<CHAMBERS;i++)if(bullets[viewer][i])s.loadedChambers.add(i);
        s.dangerDelta=dangerDeltas[viewer];s.draws=draws; s.winner=winner; s.reason=reason; s.roundWinner=roundWinner;
        for(int p=0;p<2;p++) {
            Player v=players[p].copy(); v.remaining=hand(p).size(); v.specialCount=cards[p].size();
            v.betReady=betReady[p];v.bet=p==viewer||betReady[0]&&betReady[1]?bets[p]:0;
            // Hide an unconfirmed rival wager even through their remaining balance.
            if(p!=viewer&&!(betReady[0]&&betReady[1]))v.chips+=bets[p];
            v.loadoutReady=loadoutReady[p]; v.selected=tiles[p]!=0; v.specialReady=specialReady[p]; v.acknowledged=ack[p]; v.chamberReady=chambers[p]>=0;
            s.players[p]=v;
        }
        s.hand=hand(viewer); s.cards=new ArrayList<>(cards[viewer]); s.ownTile=tiles[viewer];
        s.ownChamber=chambers[viewer]; s.opponentParity=parity[viewer]; s.hit=hit.clone();
        for (RoundObservation observation : observations[viewer]) s.opponentObservations.add(observation.copy());
        s.history=new ArrayList<>(history[viewer]); s.privateInfo=new ArrayList<>(info[viewer]);
        s.hintHistory=new ArrayList<>(hintHistory[viewer]);
        s.boughtHints=new ArrayList<>(hints[viewer]); s.knownChambers=new HashMap<>(known[viewer]);
        return s;
    }
}
