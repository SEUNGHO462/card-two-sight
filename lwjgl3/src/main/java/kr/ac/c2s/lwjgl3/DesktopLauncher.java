package kr.ac.c2s.lwjgl3;
import com.badlogic.gdx.backends.lwjgl3.*;
import kr.ac.c2s.C2SGame;
import java.util.Arrays;
public final class DesktopLauncher {
    public static void main(String[] args){
        Lwjgl3ApplicationConfiguration c=new Lwjgl3ApplicationConfiguration();c.setTitle("CARD TWO SIGHT · 카투사");c.setWindowedMode(1280,720);c.setWindowSizeLimits(960,540,-1,-1);c.useVsync(true);c.setForegroundFPS(60);
        c.disableAudio(Boolean.getBoolean("c2s.noAudio"));if(Arrays.asList(args).contains("--ui-preview")||Arrays.asList(args).contains("--intro-preview")||Arrays.asList(args).contains("--smoke")||Arrays.asList(args).contains("--shot-preview")||Arrays.asList(args).contains("--cylinder-preview"))c.setInitialVisible(false);
        new Lwjgl3Application(new C2SGame(args),c);
    }
}
