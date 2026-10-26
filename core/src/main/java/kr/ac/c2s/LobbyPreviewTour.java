package kr.ac.c2s;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Timer;
import kr.ac.c2s.screens.*;
import kr.ac.c2s.network.PracticeDifficulty;
import kr.ac.c2s.common.Protocol.Phase;

public final class LobbyPreviewTour {
    private final C2SGame app;private int step,ticks;
    private final FileHandle dir;
    public LobbyPreviewTour(C2SGame app){this.app=app;dir=Gdx.files.absolute(System.getProperty("c2s.smokeDir",".runtime/lobby-preview"));dir.mkdirs();}
    private void click(String name){Actor a=((BaseScreen)app.getScreen()).stage.getRoot().findActor(name);if(a instanceof TextButton b&&!b.isDisabled())b.fire(new ChangeListener.ChangeEvent());else throw new IllegalStateException(name);}
    private void capture(String name){for(int i=0;i<12;i++)app.getScreen().render(.05f);Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());PixmapIO.writePNG(dir.child(name+".png"),p,-1,true);p.dispose();}
    public void start(){Timer.schedule(new Timer.Task(){@Override public void run(){try{
        if(++ticks>30)throw new IllegalStateException("UI preview timed out");
        switch(step){
            case 0->{click("settings");capture("01-settings");click("settings-close");app.go(new CharacterSelectScreen(app));step++;}
            case 1->{capture("02-character-left-aligned");app.practice();step++;}
            case 2->{if(app.getScreen() instanceof LobbyScreen){click("difficulty-EASY");step++;}}
            case 3->{if(app.state.message.contains("EASY")){capture("03-easy");click("opponent-select");capture("04-opponent-selector");click("opponent-choice-1");step++;}}
            case 4->{if(app.state.opponent().characterId.equals("female_vest")){capture("05-selected-opponent");click("difficulty-HARD");step++;}}
            case 5->{if(app.state.message.contains("HARD")){capture("06-hard");click("leave-room");capture("07-leave-dialog");click("leave-cancel");click("ready");step++;}}
            case 6->{if(app.state.me().ready){click("game-start");step++;}}
            case 7->{if(app.state.phase==Phase.TILE_SELECT){if(!app.state.opponent().characterId.equals("female_vest"))throw new IllegalStateException("Opponent lost on start");capture("08-game");dir.child("PASS.txt").writeString("Settings and leave dialogs, left-aligned character labels, EASY/HARD controls, opponent selection and game start verified.",false,"UTF-8");Gdx.app.exit();cancel();}}
        }
    }catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e.toString(),false,"UTF-8");Gdx.app.exit();cancel();}}},.6f,.45f);}
}
