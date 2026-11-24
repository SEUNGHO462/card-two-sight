package kr.ac.c2s.network;
import kr.ac.c2s.common.*;
import kr.ac.c2s.common.Protocol.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** 같은 게임 엔진을 사용하는 연습 상대. 온라인 모드와 명령/응답 형식이 같다. */
public final class LocalGameGateway implements GameGateway {
    private final Consumer<Runnable> dispatch;
    private Consumer<Event> listener=e->{};
    private final Random random=new Random();
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"c2s-practice");t.setDaemon(true);return t;});
    private final kr.ac.c2s.server.JdbcMatchStore database;
    private final ExecutorService dbWorker=Executors.newSingleThreadExecutor(r->new Thread(r,"c2s-practice-database"));
    private String savedMatch="";
    private static kr.ac.c2s.server.JdbcMatchStore configuredDatabase(){try{return kr.ac.c2s.server.JdbcMatchStore.configured();}catch(java.io.IOException e){return null;}}
    private final Player[] identities;
    private boolean ready, rematch, closed;
    private GameEngine engine;
    private PracticeDifficulty difficulty;
    public LocalGameGateway(String name,String character,Consumer<Runnable> dispatch) {
        this(name,character,"female_leather",PracticeDifficulty.NORMAL,dispatch);
    }
    public LocalGameGateway(String name,String character,String opponent,PracticeDifficulty difficulty,Consumer<Runnable> dispatch){
        this(name,character,opponent,difficulty,dispatch,configuredDatabase());
    }
    public LocalGameGateway(String name,String character,String opponent,PracticeDifficulty difficulty,Consumer<Runnable> dispatch,kr.ac.c2s.server.JdbcMatchStore database){
        this.database=database;
        this.dispatch=dispatch;this.difficulty=Objects.requireNonNull(difficulty);
        identities=new Player[]{new Player(name,Protocol.cleanCharacter(character)),new Player("연습 상대",Protocol.cleanCharacter(opponent))};
    }
    public void configure(PracticeDifficulty mode,String opponent){
        Objects.requireNonNull(mode);String id=Protocol.cleanCharacter(opponent);
        if(closed)return;
        executor.execute(()->{if(closed||engine!=null)return;difficulty=mode;identities[1].characterId=id;ready=false;emit();});
    }
    @Override public void setListener(Consumer<Event> listener) { this.listener=listener; }
    private void emit() {
        Snapshot s;
        if(engine==null) { s=new Snapshot();s.players[0]=identities[0].copy();s.players[1]=identities[1].copy();s.players[0].ready=ready;s.players[1].ready=true; }
        else { s=engine.snapshot(0);s.players[0].rematch=rematch; }
        if(s.phase==Phase.FINAL&&!savedMatch.equals(s.matchId)){
            savedMatch=s.matchId;Snapshot result=s;String mode=difficulty.name();
            if(database!=null)dbWorker.execute(()->{try{database.initialize();database.save(result,"AI",mode);}catch(java.sql.SQLException e){publish(Event.error("연습 경기 DB 저장에 실패했습니다. DB 실행 상태를 확인하세요."));}});
            else publish(Event.error("DB가 설정되지 않아 연습 경기는 JSON 누적 기록에만 반영됩니다."));
        }
        s.password="PRACTICE";s.message="연습 모드 · "+difficulty.name();
        dispatch.accept(()->{if(!closed)listener.accept(Event.state(s));});
    }
    private void publish(Event event){dispatch.accept(()->{if(!closed)listener.accept(event);});}
    @Override public void send(Command c) {
        if(closed) return;
        executor.execute(()->{
            try {
                if(c.action==Action.HISTORY){
                    if(database==null){publish(Event.error("이 PC의 DB 접속이 설정되지 않았습니다."));return;}
                    dbWorker.execute(()->{try{database.initialize();publish(Event.history(database.recent(20)));}catch(java.sql.SQLException e){publish(Event.error("경기 기록 DB에 연결하지 못했습니다."));}});return;
                }
                if(c.action==Action.CREATE) { emit();return; }
                if(c.action==Action.READY && engine==null) ready=!ready;
                else if(c.action==Action.START && engine==null) { if(!ready)throw new IllegalArgumentException("준비하기를 먼저 눌러주세요.");engine=new GameEngine(identities[0],identities[1],random,true,true); }
                else if(c.action==Action.REMATCH && engine!=null && engine.phase()==Phase.FINAL) engine=new GameEngine(identities[0],identities[1],random,true,true);
                else if(engine!=null) engine.apply(0,c);
                emit();executor.schedule(this::bot,650,TimeUnit.MILLISECONDS);
            } catch(IllegalArgumentException e) { dispatch.accept(()->{if(!closed)listener.accept(Event.error(e.getMessage()));}); }
        });
    }
    private void bot() {
        if(closed || engine==null) return;
        Snapshot b=engine.snapshot(1);
        try {
            switch(b.phase) {
                case LOADOUT_SELECT -> {
                    if(!b.me().loadoutReady){List<Card> deck=new ArrayList<>(Arrays.asList(Card.values()));Collections.shuffle(deck,random);engine.apply(1,Command.loadout(deck.subList(0,2)));}
                }
                case BET_SELECT -> { if(!b.me().betReady)engine.apply(1,Command.number(Action.BET,b.me().chips==0?0:1+random.nextInt(Math.min(3,b.me().chips)))); }
                case TILE_SELECT -> { if(!b.me().selected) engine.apply(1,Command.number(Action.TILE,PracticeStrategy.tile(b,difficulty,random))); }
                case SPECIAL_SELECT -> {
                    if(!b.me().specialReady) {
                        Card card=PracticeStrategy.special(b,difficulty,random);
                        engine.apply(1,card==Card.DISGUISE?Command.disguise(random.nextBoolean()):Command.special(card,1+random.nextInt(9)));
                    }
                }
                case ROUND_RESULT, ROULETTE_RESULT -> { if(!b.me().acknowledged) engine.apply(1,Command.of(Action.NEXT)); }
                case AMMO_LOAD -> {
                    if(b.pendingAmmo>0){List<Integer> empty=new ArrayList<>();for(int i=0;i<Protocol.CHAMBERS;i++)if(!b.loadedChambers.contains(i))empty.add(i);
                        engine.apply(1,Command.number(Action.LOAD_AMMO,empty.get(random.nextInt(empty.size()))));}
                }
                case ROULETTE_SELECT -> { if(!b.me().chamberReady){
                    int chosen=PracticeStrategy.chamber(b,random);
                    if(difficulty==PracticeDifficulty.HARD&&(b.me().danger>=3||b.me().life==1&&b.me().danger>0)&&b.me().chips>=Protocol.hintCost("INSPECT")&&!b.boughtHints.contains("INSPECT")){
                        engine.apply(1,Command.hint("INSPECT",chosen));b=engine.snapshot(1);chosen=PracticeStrategy.chamber(b,random);
                    }
                    engine.apply(1,Command.number(Action.CHAMBER,chosen));
                } }
                default -> { return; }
            }
            emit(); if(engine.phase()!=b.phase) executor.schedule(this::bot,650,TimeUnit.MILLISECONDS);
        } catch(IllegalArgumentException ignored) {}
    }
    @Override public void close() { closed=true;executor.shutdownNow();dbWorker.shutdown(); }
}
