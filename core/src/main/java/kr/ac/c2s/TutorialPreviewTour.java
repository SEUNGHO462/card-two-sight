package kr.ac.c2s;
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Timer;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.screens.*;
import java.util.*;

/** --tutorial-preview: 메인 메뉴의 튜토리얼 버튼부터 한 판을 끝까지 진행하며, 말풍선이 바뀔 때마다 실제 화면을 캡처한다. */
public final class TutorialPreviewTour {
    private final C2SGame app;private final FileHandle dir;private int ticks,bubbles,skipStep;private boolean offered,started;private String lastText="";
    private final StringBuilder log=new StringBuilder();
    public TutorialPreviewTour(C2SGame app){this.app=app;dir=Gdx.files.absolute(System.getProperty("c2s.smokeDir",".runtime/tutorial-preview"));dir.mkdirs();dir.child("PASS.txt").delete();dir.child("FAILED.txt").delete();}
    public void start(){Timer.schedule(new Timer.Task(){@Override public void run(){try{tick();}catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e+"\n\n"+log,false,"UTF-8");Gdx.app.exit();cancel();}}},1.2f,.5f);}
    private void capture(String key){for(int i=0;i<12;i++)app.getScreen().render(.05f);Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());PixmapIO.writePNG(dir.child(key+".png"),p,-1,true);p.dispose();}
    private Actor find(String name){return ((BaseScreen)app.getScreen()).stage.getRoot().findActor(name);}
    private void click(String name){Actor a=find(name);if(a instanceof TextButton b&&!b.isDisabled())b.fire(new ChangeListener.ChangeEvent());else throw new IllegalStateException("Cannot click "+name);}
    private boolean enabled(String name){return find(name) instanceof TextButton b&&!b.isDisabled();}
    private boolean chosen(String name){return find(name) instanceof TextButton b&&b.getText().toString().startsWith("선택됨");}
    private void tick(){
        if(++ticks>1600)throw new IllegalStateException("Tutorial tour timed out");
        if(app.getScreen() instanceof MainMenuScreen&&skipStep==2){
            if(app.state!=null)throw new IllegalStateException("Practice game still connected after skip");capture("c2-main-after-skip");
            dir.child("PASS.txt").writeString("Tutorial round 1 guided with "+bubbles+" bubbles; skip cancel kept the guide; skip confirm returned to main menu.\n\n"+log,false,"UTF-8");Gdx.app.exit();return;
        }
        if(app.getScreen() instanceof MainMenuScreen){
            if(started)return;
            if(find("tutorial-offer")!=null){capture("00-offer");click("tutorial-start");started=true;}
            else if(!offered){offered=true;capture("00-menu");click("tutorial");}
            return;
        }
        if(app.tutorial==null)throw new IllegalStateException("Tutorial mode not active");
        for(int i=0;i<3;i++)app.getScreen().render(.05f);
        Actor bubble=find("tutorial-bubble");
        if(bubble!=null&&bubble.isVisible()&&find("tutorial-text") instanceof Label text&&!text.getText().toString().equals(lastText)){
            lastText=text.getText().toString();bubbles++;
            Snapshot s=app.state;String name=String.format("b%02d-r%d-%s",bubbles,s.round,s.phase.name().toLowerCase());
            log.append(name).append(" : ").append(lastText.replace('\n',' ')).append('\n');capture(name);return;
        }
        Snapshot s=app.state;if(s==null)return;
        // 2라운드 베팅에서 건너뛰기를 확인: 한 번은 취소, 한 번은 확정한 뒤 말풍선 레이어가 사라졌는지 본다.
        if(s.round>=2&&s.phase==Phase.BET_SELECT&&skipStep<2){
            switch(skipStep++){
                case 0->{click("tutorial-skip");capture("c1-skip-confirm");click("tutorial-skip-cancel");}
                case 1->{if(find("tutorial-guide")==null)throw new IllegalStateException("Cancel removed the guide");click("tutorial-skip");click("tutorial-skip-confirm");}
            }
            return;
        }
        switch(s.phase){
            case LOBBY->click(s.me().ready?"game-start":"ready");
            case LOADOUT_SELECT->{if(!s.me().loadoutReady){if(!chosen("loadout-REVERSE"))click("loadout-REVERSE");else if(!chosen("loadout-SCOUT"))click("loadout-SCOUT");else click("loadout-confirm");}}
            case BET_SELECT->{if(!s.me().betReady)click("bet-confirm");}
            case TILE_SELECT->{if(!s.me().selected){if(!enabled("tile-confirm"))click("tile-"+s.hand.get(0));else click("tile-confirm");}}
            case SPECIAL_SELECT->{if(!s.me().specialReady)click("special-skip");}
            case ROUND_RESULT->{if(!s.me().acknowledged)click("round-next");}
            case AMMO_LOAD->{RouletteScreen screen=(RouletteScreen)app.getScreen();if(screen.isAmmoPlacementReady()){
                if(!enabled("chamber-confirm")){int empty=java.util.stream.IntStream.range(0,9).filter(i->!s.loadedChambers.contains(i)).findFirst().orElseThrow();click("chamber-"+empty);}
                else click("chamber-confirm");}}
            case ROULETTE_SELECT->{RouletteScreen screen=(RouletteScreen)app.getScreen();
                if(!s.me().chamberReady&&screen.isPreparationReady()&&enabled("chamber-confirm")){if(s.round==1&&s.boughtHints.isEmpty()&&enabled("hint-RANGE"))click("hint-RANGE");else click("chamber-confirm");}}
            case ROULETTE_RESULT->{if(((RouletteScreen)app.getScreen()).isResultReady()&&!s.me().acknowledged)click("roulette-next");}
            case FINAL->{capture("99-final");dir.child("PASS.txt").writeString("Tutorial reached FINAL in "+s.round+" rounds with "+bubbles+" bubbles.\n\n"+log,false,"UTF-8");Gdx.app.exit();}
        }
    }
}
