package kr.ac.c2s;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Timer;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.game.CylinderPreparation.Phase;
import kr.ac.c2s.screens.RouletteScreen;

/** Deterministic rendering check, enabled only by --cylinder-preview. */
public final class CylinderPreviewTour {
    private final C2SGame app;private final FileHandle dir;private int index;
    private final Phase[] phases={Phase.LOADING,Phase.LOADING,Phase.LOADING,Phase.SPINNING,Phase.READY,Phase.READY,Phase.ALIGNING,Phase.LOCKED};
    private final float[] times={0,.30f,1.19f,.43f,0,0,.4f,0};
    public CylinderPreviewTour(C2SGame app){this.app=app;dir=Gdx.files.absolute(System.getProperty("c2s.smokeDir",".runtime/cylinder-preview"));dir.mkdirs();dir.child("PASS.txt").delete();dir.child("FAILED.txt").delete();}
    public void start(){
        Snapshot s=new Snapshot();s.phase=kr.ac.c2s.common.Protocol.Phase.ROULETTE_SELECT;s.matchId="cylinder-preview";s.round=3;s.roundWinner=1;
        s.players[0]=new Player("플레이어","male_handsome");s.players[1]=new Player("상대","female_leather");s.players[0].danger=3;s.dangerDelta=1;
        s.history.add("R3 나 4 · 상대 홀수 · 패배");app.accept(s);
        Timer.schedule(new Timer.Task(){@Override public void run(){try{
            RouletteScreen screen=(RouletteScreen)app.getScreen();
            if(index==phases.length){dir.child("PASS.txt").writeString("Loading, cartridges, concealed spin, selection, alignment and confirmation rendered.",false,"UTF-8");Gdx.app.exit();cancel();return;}
            screen.previewPreparationAt(phases[index],times[index],index<5?-1:2);
            if(index==7)screen.previewFireDialog();
            for(int i=0;i<12;i++)app.getScreen().render(.05f);
            Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(dir.child("cylinder-"+index+".png"),p,-1,true);p.dispose();
            screen.accept(s);if(screen.preparationPhase()!=phases[index])throw new IllegalStateException("Animation reset after state update");
            index++;
        }catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e.toString(),false,"UTF-8");Gdx.app.exit();cancel();}}},.5f,.45f);
    }
}
