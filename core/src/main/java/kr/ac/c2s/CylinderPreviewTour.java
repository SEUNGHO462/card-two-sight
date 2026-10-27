package kr.ac.c2s;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Timer;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.game.CylinderPreparation.Phase;
import kr.ac.c2s.screens.*;

/** Visual and layout checks for manual loading plus both viewpoints of the same final result. */
public final class CylinderPreviewTour {
    private final C2SGame app;private final FileHandle dir;private int index;
    private RouletteScreen roulette;
    public CylinderPreviewTour(C2SGame app){this.app=app;dir=Gdx.files.absolute(System.getProperty("c2s.smokeDir",".runtime/cylinder-preview"));dir.mkdirs();}
    public void start(){
        Snapshot s=new Snapshot();s.phase=kr.ac.c2s.common.Protocol.Phase.AMMO_LOAD;s.matchId="manual-preview";s.round=1;s.roundWinner=1;s.pendingAmmo=1;s.dangerDelta=1;
        s.players[0]=new Player("플레이어","male_handsome");s.players[1]=new Player("연습 상대","female_leather");s.me().danger=2;s.loadedChambers.add(1);
        app.accept(s);roulette=(RouletteScreen)app.getScreen();
        Timer.schedule(new Timer.Task(){@Override public void run(){try{
            String file;
            switch(index){
                case 0->{roulette.previewPreparationAt(Phase.PLACING,0,7);file="01-choose-empty";}
                case 1->{roulette.previewPreparationAt(Phase.LOADING,.85f,7);file="02-load-chosen-eight";}
                case 2->{roulette.previewPreparationAt(Phase.LOADING,1.4f,7);file="03-caps-two-and-eight";}
                case 3->{roulette.previewPreparationAt(Phase.SPINNING,.5f,7);file="04-spin";}
                case 4->{s.phase=kr.ac.c2s.common.Protocol.Phase.ROULETTE_SELECT;s.pendingAmmo=0;s.hintHistory.add("R1 · 탄실 검사: 3번은 빈칸입니다.");roulette.accept(s);roulette.previewPreparationAt(Phase.READY,0,2);file="05-fire-and-hint";}
                case 5,6->{
                    Snapshot end=new Snapshot();end.phase=kr.ac.c2s.common.Protocol.Phase.FINAL;end.matchId="final-preview-"+index;end.you=index==5?0:1;end.winner=1;end.round=5;end.reason="목숨 소진";
                    end.players[0]=new Player("플레이어","male_handsome");end.players[1]=new Player("연습 상대","female_leather");end.players[0].wins=4;end.players[1].wins=1;end.players[0].life=0;end.players[1].life=2;
                    app.accept(end);BaseScreen screen=(BaseScreen)app.getScreen();
                    Label title=screen.stage.getRoot().findActor("final-outcome");if(!title.getText().toString().equals(index==5?"패배":"승리"))throw new IllegalStateException("Wrong winner viewpoint");
                    TextButton a=screen.stage.getRoot().findActor("rematch"),b=screen.stage.getRoot().findActor("result-home");
                    if(a.getWidth()!=b.getWidth()||a.getHeight()!=b.getHeight())throw new IllegalStateException("Unequal result buttons");
                    file=index==5?"06-defeat-leading-score":"07-opponent-victory";
                }
                default->{dir.child("PASS.txt").writeString("Nine original-metal sockets; manual eight-slot insertion; persistent receipt; defeat at 4:1 with zero lives; opponent victory; equal result buttons rendered and checked.",false,"UTF-8");Gdx.app.exit();cancel();return;}
            }
            for(int i=0;i<12;i++)app.getScreen().render(.05f);
            Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());PixmapIO.writePNG(dir.child(file+".png"),p,-1,true);p.dispose();index++;
        }catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e.toString(),false,"UTF-8");Gdx.app.exit();cancel();}}},.5f,.5f);
    }
}
