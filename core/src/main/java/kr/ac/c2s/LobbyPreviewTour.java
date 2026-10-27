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
        if(++ticks>100)throw new IllegalStateException("UI preview timed out");
        switch(step){
            case 0->{click("settings");capture("01-settings");click("settings-close");app.go(new CharacterSelectScreen(app));step++;}
            case 1->{capture("02-character-left-aligned");app.practice();step++;}
            case 2->{if(app.getScreen() instanceof LobbyScreen){click("difficulty-EASY");step++;}}
            case 3->{if(app.state.message.contains("EASY")){capture("03-easy");click("opponent-select");capture("04-opponent-selector");click("opponent-choice-1");step++;}}
            case 4->{if(app.state.opponent().characterId.equals("female_vest")){capture("05-selected-opponent");click("difficulty-HARD");step++;}}
            case 5->{if(app.state.message.contains("HARD")){capture("06-hard");click("leave-room");capture("07-leave-dialog");click("leave-cancel");click("ready");step++;}}
            case 6->{if(app.state.me().ready){click("game-start");step++;}}
            case 7->{if(app.state.phase==Phase.LOADOUT_SELECT&&!app.state.me().loadoutReady){capture("08-card-selection-empty");click("loadout-REVERSE");click("loadout-SCOUT");capture("08-card-selection");click("loadout-confirm");return;}if(app.state.phase==Phase.TILE_SELECT){if(!app.state.opponent().characterId.equals("female_vest"))throw new IllegalStateException("Opponent lost on start");if(!app.state.cards.equals(java.util.List.of(kr.ac.c2s.common.Protocol.Card.REVERSE,kr.ac.c2s.common.Protocol.Card.SCOUT)))throw new IllegalStateException("Chosen loadout lost");capture("08-game");click("tile-9");click("tile-confirm");step++;}}
            case 8->{if(app.state.phase==Phase.SPECIAL_SELECT&&!app.state.me().specialReady){click("special-skip");step++;}}
            case 9->{if(app.state.phase==Phase.ROUND_RESULT&&!app.state.me().acknowledged){click("round-next");step++;}}
            case 10->{if(app.state.phase==Phase.AMMO_LOAD&&app.state.pendingAmmo>0&&((RouletteScreen)app.getScreen()).isAmmoPlacementReady()){
                int empty=java.util.stream.IntStream.range(0,9).filter(i->!app.state.loadedChambers.contains(i)).findFirst().orElseThrow();click("chamber-"+empty);click("chamber-confirm");return;
            }if(app.state.phase==Phase.ROULETTE_SELECT&&((RouletteScreen)app.getScreen()).isPreparationReady()){click("chamber-0");click("hint-INSPECT");step++;}}
            case 11->{if(!app.state.hintHistory.isEmpty()){
                if(app.state.me().chips!=4||!app.state.knownChambers.containsKey(0))throw new IllegalStateException("Hint receipt missing");
                capture("09-real-hint-purchase");click("chamber-confirm");step++;
            }}
            case 12->{if(app.state.phase==Phase.ROULETTE_RESULT){capture("10-direct-shot");dir.child("PASS.txt").writeString("Card selection, nine sockets, actual hint purchase receipt (12 to 4 chips), and direct shot reached result.",false,"UTF-8");Gdx.app.exit();cancel();}}
        }
    }catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e.toString(),false,"UTF-8");Gdx.app.exit();cancel();}}},.6f,.45f);}
}
