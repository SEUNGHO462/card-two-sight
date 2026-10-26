package kr.ac.c2s;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Timer;
import kr.ac.c2s.screens.*;
public final class IntroPreviewTour {
    private final C2SGame app;private int index;
    private final float[] moments={.4f,1.2f,2.8f,4.8f,6.8f};
    public IntroPreviewTour(C2SGame app){this.app=app;}
    public void start(){
        FileHandle dir=Gdx.files.absolute(System.getProperty("c2s.smokeDir",".runtime/intro-preview"));dir.mkdirs();
        Timer.schedule(new Timer.Task(){@Override public void run(){try{
            if(index<moments.length){
                IntroScreen s=(IntroScreen)app.getScreen();s.previewAt(moments[index]);s.render(0);
                Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
                PixmapIO.writePNG(dir.child("intro-"+index+".png"),p,-1,true);p.dispose();index++;return;
            }
            ((IntroScreen)app.getScreen()).previewAt(8.5f);app.getScreen().render(0);
            if(!(app.getScreen() instanceof MainMenuScreen))throw new IllegalStateException("Intro did not reach menu");
            app.go(new IntroScreen(app));Gdx.input.getInputProcessor().keyDown(Input.Keys.ENTER);
            if(!(app.getScreen() instanceof MainMenuScreen))throw new IllegalStateException("Enter skip failed");
            app.go(new IntroScreen(app));Gdx.input.getInputProcessor().touchDown(100,100,0,0);
            if(!(app.getScreen() instanceof MainMenuScreen))throw new IllegalStateException("Click skip failed");
            dir.child("PASS.txt").writeString("Five intro frames rendered; automatic transition, Enter skip and click skip passed.",false,"UTF-8");
            Gdx.app.exit();cancel();
        }catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e.toString(),false,"UTF-8");Gdx.app.exit();cancel();}}},.5f,.4f);
    }
}
