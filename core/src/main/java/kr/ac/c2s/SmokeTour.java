package kr.ac.c2s;
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Timer;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.screens.*;
import java.util.*;

/** 배포 게임과 같은 버튼/상태 경로를 실행하는 명시적 --smoke 검증 모드. */
public final class SmokeTour {
    private final C2SGame app;private final Set<String> captured=new HashSet<>();private int ticks;
    private final FileHandle dir;
    public SmokeTour(C2SGame app){this.app=app;dir=Gdx.files.absolute(System.getProperty("c2s.smokeDir",".runtime/preview"));dir.mkdirs();dir.child("PASS.txt").delete();dir.child("FAILED.txt").delete();}
    public void start(){Timer.schedule(new Timer.Task(){@Override public void run(){try{tick();}catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e.toString(),false,"UTF-8");Gdx.app.exit();cancel();}}},1.2f,.65f);}
    private void capture(String key){if(captured.add(key)){for(int i=0;i<12;i++)app.getScreen().render(.05f);Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());PixmapIO.writePNG(dir.child(key+".png"),p,-1,true);p.dispose();}}
    private void click(String name){Actor a=((BaseScreen)app.getScreen()).stage.getRoot().findActor(name);if(a instanceof TextButton b&&!b.isDisabled())b.fire(new ChangeListener.ChangeEvent());else throw new IllegalStateException("Cannot click "+name);}
    private void tick(){
        if(++ticks>360)throw new IllegalStateException("Smoke tour timed out");
        if(app.getScreen() instanceof MainMenuScreen){capture("01-main");click("help");capture("01-help");click("help-close");click("start");return;}
        if(app.getScreen() instanceof CharacterSelectScreen){capture("02-character");click("character-confirm");return;}
        if(app.getScreen() instanceof ConnectionScreen){capture("03-connection");click("practice");return;}
        Snapshot s=app.state;if(s==null)return;
        switch(s.phase){
            case LOBBY->{capture("04-lobby");click(s.me().ready?"game-start":"ready");}
            case TILE_SELECT->{capture("05-game");if(!s.me().selected){int n=s.hand.get(s.hand.size()-1);click("tile-"+n);click("tile-confirm");}}
            case SPECIAL_SELECT->{capture("06-special");if(!s.me().specialReady)click("special-skip");}
            case ROUND_RESULT->{capture("07-round-result");if(!s.me().acknowledged)click("round-next");}
            case ROULETTE_SELECT->{
                RouletteScreen screen=(RouletteScreen)app.getScreen();
                if(s.me().chamberReady)capture("09-waiting");
                else if(screen.isFireDialogReady()){capture("09-shot-confirm");click("fire-confirm");}
                else if(screen.isPreparationReady()){capture("08-chamber");click("chamber-0");click("chamber-confirm");}
                else capture("08-"+screen.preparationPhase().name().toLowerCase());
            }
            case ROULETTE_RESULT->{if(((RouletteScreen)app.getScreen()).isResultReady()){capture("10-roulette-result");if(!s.me().acknowledged)click("roulette-next");}}
            case FINAL->{capture("11-final");dir.child("PASS.txt").writeString("Complete local game flow reached FINAL in "+s.round+" rounds.\nCaptured "+captured.size()+" real rendered screens.",false,"UTF-8");Gdx.app.exit();}
        }
    }
}
