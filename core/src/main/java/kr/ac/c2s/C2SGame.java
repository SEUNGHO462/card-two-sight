package kr.ac.c2s;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.game.RecordStore;
import kr.ac.c2s.network.*;
import kr.ac.c2s.screens.*;
import kr.ac.c2s.server.GameServer;
import kr.ac.c2s.ui.*;
import java.util.Arrays;
import static kr.ac.c2s.common.Protocol.*;

public final class C2SGame extends Game {
    public GameAssets assets;public Theme theme;public RecordStore records;public Texture white;
    public Snapshot state;public String toast="";public float toastTime;
    private GameGateway gateway;private GameServer ownedServer;
    public PracticeDifficulty practiceDifficulty=PracticeDifficulty.NORMAL;
    public String practiceOpponent="female_leather";
    public boolean isPractice(){return gateway instanceof LocalGameGateway;}
    public void configurePractice(PracticeDifficulty difficulty,String character){
        if(gateway instanceof LocalGameGateway local){practiceDifficulty=difficulty;practiceOpponent=kr.ac.c2s.common.Protocol.cleanCharacter(character);local.configure(difficulty,practiceOpponent);}
    }
    public boolean connecting;private long connectionGeneration;
    private final String[] args;
    public C2SGame(String[] args){this.args=args;}
    @Override public void create(){
        assets=new GameAssets();theme=new Theme();records=new RecordStore();
        if(!Arrays.asList(CHARACTER_IDS).contains(records.character))records.character="male_handsome";
        Pixmap p=new Pixmap(1,1,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);p.fill();white=new Texture(p);p.dispose();
        go(Arrays.asList(args).contains("--ui-preview") || Arrays.asList(args).contains("--smoke") || Arrays.asList(args).contains("--shot-preview") || Arrays.asList(args).contains("--cylinder-preview") ? new MainMenuScreen(this) : new IntroScreen(this));
        if(Arrays.asList(args).contains("--ui-preview"))new LobbyPreviewTour(this).start();
        else if(Arrays.asList(args).contains("--intro-preview"))new IntroPreviewTour(this).start();
        else if(Arrays.asList(args).contains("--cylinder-preview"))new CylinderPreviewTour(this).start();
        else if(Arrays.asList(args).contains("--shot-preview"))new ShotPreviewTour(this).start();
        else if(Arrays.asList(args).contains("--smoke"))new SmokeTour(this).start();
    }
    public void go(BaseScreen next){Screen old=getScreen();setScreen(next);if(old!=null)old.dispose();}
    public void toast(String text){toast=text==null?"처리할 수 없습니다.":text;toastTime=5;}
    public void sound(String name){if(records.sound)assets.sound(name,records.volume);}
    public void send(Command c){if(gateway!=null)gateway.send(c);}
    public void practice(){disconnect();long id=connectionGeneration;gateway=new LocalGameGateway(records.nickname,records.character,practiceOpponent,practiceDifficulty,Gdx.app::postRunnable);bind(id);send(Command.of(Action.CREATE));}
    public void online(String host,int port,String password,boolean create,boolean startServer){
        disconnect();connecting=true;long id=connectionGeneration;
        try{
            if(startServer && create){
                if(ownedServer!=null && ownedServer.port()!=port){ownedServer.close();ownedServer=null;}
                if(ownedServer==null){ownedServer=new GameServer(port);ownedServer.start();}
            }
            gateway=new SocketGameGateway(host,port,Gdx.app::postRunnable);bind(id);
            send(Command.enter(create?Action.CREATE:Action.JOIN,records.nickname,records.character,password));
            toast("서버에 연결하는 중입니다…");
        }catch(Exception e){connecting=false;toast("서버를 열지 못했습니다. 포트가 이미 사용 중인지 확인하세요.");}
    }
    private void bind(long generation){gateway.setListener(event->{if(generation!=connectionGeneration)return;if("STATE".equals(event.type)){connecting=false;accept(event.state);}else{toast(event.message);connecting=false;if("CLOSED".equals(event.type)){disconnect();go(new ConnectionScreen(this));}}});}
    public void accept(Snapshot next){
        if(next==null || next.players[next.you]==null)return;state=next;
        if(next.phase==Phase.FINAL){records.record(next);if(!records.lastError.isEmpty())toast(records.lastError);}
        BaseScreen target;
        if(next.phase==Phase.LOBBY){if(getScreen() instanceof LobbyScreen s){s.accept(next);return;}target=new LobbyScreen(this,next);}
        else if(next.phase==Phase.LOADOUT_SELECT){if(getScreen() instanceof CardSelectScreen s){s.accept(next);return;}target=new CardSelectScreen(this,next);}
        else if(next.phase==Phase.ROULETTE_SELECT || next.phase==Phase.ROULETTE_RESULT){if(getScreen() instanceof RouletteScreen s){s.accept(next);return;}target=new RouletteScreen(this,next);}
        else if(next.phase==Phase.FINAL){if(getScreen() instanceof FinalResultScreen s){s.accept(next);return;}target=new FinalResultScreen(this,next);}
        else{if(getScreen() instanceof GameScreen s){s.accept(next);return;}target=new GameScreen(this,next);}
        go(target);
    }
    public void disconnect(){connectionGeneration++;connecting=false;if(gateway!=null){gateway.close();gateway=null;}state=null;}
    public void home(){disconnect();go(new MainMenuScreen(this));}
    public void confirmLeave(){
        if(getScreen() instanceof MainMenuScreen){Gdx.app.exit();return;}
        if(state==null){go(new MainMenuScreen(this));return;}
        var screen=(BaseScreen)getScreen();
        var dialog=screen.titledDialog("테이블 나가기",580);
        var message=new com.badlogic.gdx.scenes.scene2d.ui.Label("진행 중인 방을 나가시겠습니까?",theme.skin,"body");
        dialog.getContentTable().add(message).width(580).left().padBottom(12);
        var leave=new com.badlogic.gdx.scenes.scene2d.ui.TextButton("나가기",theme.skin);
        leave.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener(){@Override public void changed(ChangeEvent e,com.badlogic.gdx.scenes.scene2d.Actor actor){home();}});
        dialog.getButtonTable().add(leave).minWidth(220).minHeight(60).pad(10);
        dialog.button("계속하기");dialog.getButtonTable().getChildren().peek().setName("leave-cancel");dialog.show(screen.stage);
    }
    public void toggleFullscreen(){if(Gdx.graphics.isFullscreen())Gdx.graphics.setWindowedMode(1280,720);else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());}
    @Override public void dispose(){disconnect();if(ownedServer!=null)ownedServer.close();if(getScreen()!=null)getScreen().dispose();assets.dispose();theme.dispose();white.dispose();}
}
