package kr.ac.c2s;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Timer;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.screens.*;

/** Actual renderer/input checks for the antique menu and serif font. */
public final class MenuPreviewTour {
    private final C2SGame app;private final FileHandle dir;private int step;
    public MenuPreviewTour(C2SGame app){this.app=app;dir=Gdx.files.absolute(System.getProperty("c2s.smokeDir",".runtime/menu-preview"));dir.mkdirs();}
    private void click(String name){Actor a=((BaseScreen)app.getScreen()).stage.getRoot().findActor(name);if(!(a instanceof TextButton b)||b.isDisabled())throw new IllegalStateException(name);b.fire(new ChangeListener.ChangeEvent());}
    private void capture(String name){for(int frame=0;frame<8;frame++)app.getScreen().render(.05f);Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());PixmapIO.writePNG(dir.child(name+".png"),p,-1,true);p.dispose();}
    public void start(){Timer.schedule(new Timer.Task(){@Override public void run(){try{
        switch(step++){
            case 0->capture("01-antique-menu");
            case 1->{MainMenuScreen m=(MainMenuScreen)app.getScreen();m.holdShotForPreview(true);click("settings");
                if(!m.shotPending()||m.stage.getRoot().findActor("menu-impact")==null)throw new IllegalStateException("Click effect absent");
                if(m.stage.getRoot().findActor("settings-close")!=null)throw new IllegalStateException("Action ran before impact");capture("02-click-bullet-mark");m.holdShotForPreview(false);}
            case 2->{MainMenuScreen m=(MainMenuScreen)app.getScreen();for(int i=0;i<10;i++)m.render(.05f);
                if(m.stage.getRoot().findActor("settings-close")==null)throw new IllegalStateException("Delayed action did not run");capture("03-serif-settings");click("settings-close");}
            case 3->{click("start");MainMenuScreen m=(MainMenuScreen)app.getScreen();for(int i=0;i<10&&app.getScreen()==m;i++)m.render(.05f);
                if(!(app.getScreen() instanceof CharacterSelectScreen))throw new IllegalStateException("Start failed");capture("04-serif-characters");}
            case 4->{Snapshot s=new Snapshot();s.phase=Phase.LOADOUT_SELECT;s.matchId="card-art-preview";s.players[0]=new Player("플레이어","male_handsome");s.players[1]=new Player("연습 상대","female_leather");app.accept(s);capture("05-blood-drop-cards");}
            case 5->{Snapshot s=new Snapshot();s.phase=Phase.TILE_SELECT;s.matchId="card-help-preview";s.players[0]=new Player("플레이어","male_handsome");s.players[1]=new Player("연습 상대","female_leather");for(int i=1;i<=9;i++)s.hand.add(i);s.cards.add(Card.REVERSE);app.accept(s);click("view-REVERSE");capture("06-inset-card-dialog");}
            case 6->{Snapshot s=new Snapshot();s.phase=Phase.AMMO_LOAD;s.matchId="large-cylinder-preview";s.pendingAmmo=1;s.dangerDelta=1;s.players[0]=new Player("플레이어","male_handsome");s.players[1]=new Player("연습 상대","female_leather");s.me().danger=2;s.loadedChambers.add(1);app.accept(s);capture("07-large-cylinder");}
            case 7->{Snapshot s=new Snapshot();s.phase=Phase.FINAL;s.winner=1;s.reason="목숨 소진";s.players[0]=new Player("플레이어","male_handsome");s.players[1]=new Player("연습 상대","female_leather");s.me().life=0;s.me().wins=4;s.opponent().wins=1;app.records.wins=0;app.records.losses=3;app.records.draws=1;app.accept(s);capture("08-burgundy-record-panel");}
            default->{dir.child("PASS.txt").writeString("Antique menu, bullet mark before delayed action, settings, start, serif characters, card art, inset dialog and larger cylinder rendered. Audio invocation uses existing original shot WAV; device output not audited.",false,"UTF-8");Gdx.app.exit();cancel();}
        }
    }catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e.toString(),false,"UTF-8");Gdx.app.exit();cancel();}}},.8f,.6f);}
}
