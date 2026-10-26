package kr.ac.c2s;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Timer;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.screens.RouletteScreen;

/** Explicit --shot-preview only. Does not join rooms or record fake matches. */
public final class ShotPreviewTour {
    private final C2SGame app;
    private final FileHandle dir;
    private int index;
    private final float[] times={.35f,.87f,1.5f,2.05f,3.5f};
    public ShotPreviewTour(C2SGame app){this.app=app;dir=Gdx.files.absolute(System.getProperty("c2s.smokeDir",".runtime/shot-preview"));dir.mkdirs();dir.child("PASS.txt").delete();dir.child("FAILED.txt").delete();}
    public void start(){
        Snapshot s=new Snapshot();s.phase=Phase.ROULETTE_RESULT;s.you=0;s.round=3;
        s.players[0]=new Player("생존 확인",kr.ac.c2s.common.Protocol.cleanCharacter(System.getProperty("c2s.preview.left","male_handsome")));
        s.players[1]=new Player("피격 확인",kr.ac.c2s.common.Protocol.cleanCharacter(System.getProperty("c2s.preview.right","female_leather")));
        s.hit=new boolean[]{false,true};s.players[1].life=2;app.accept(s);
        Timer.schedule(new Timer.Task(){@Override public void run(){try{
            RouletteScreen screen=(RouletteScreen)app.getScreen();
            if(index>=times.length){dir.child("PASS.txt").writeString("Survival, screen flash, recoil, delayed life and result frames rendered.",false,"UTF-8");Gdx.app.exit();cancel();return;}
            screen.previewResultAt(times[index]);
            app.getScreen().render(0);
            Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(dir.child("shot-"+index+".png"),p,-1,true);p.dispose();
            // Repeated network acknowledgments must not restart the shot sequence.
            screen.accept(s);if(Math.abs(screen.resultTime()-times[index])>.001f)throw new IllegalStateException("Shot sequence restarted");
            index++;
        }catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e.toString(),false,"UTF-8");Gdx.app.exit();cancel();}}},.5f,.4f);
    }
}
