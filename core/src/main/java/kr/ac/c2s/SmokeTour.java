package kr.ac.c2s;
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.screens.*;
import kr.ac.c2s.ui.OrnateFrame;
import java.util.*;

/** 배포 게임과 같은 버튼/상태 경로를 실행하는 명시적 --smoke 검증 모드. */
public final class SmokeTour {
    private final C2SGame app;private final Set<String> captured=new HashSet<>(),issues=new TreeSet<>();private int ticks;private long nextFrame;
    private final FileHandle dir;
    public SmokeTour(C2SGame app){this.app=app;dir=Gdx.files.absolute(System.getProperty("c2s.smokeDir",".runtime/preview"));dir.mkdirs();dir.child("PASS.txt").delete();dir.child("FAILED.txt").delete();}
    public void start(){Timer.schedule(new Timer.Task(){@Override public void run(){try{tick();}catch(Throwable e){e.printStackTrace();dir.child("FAILED.txt").writeString(e.toString(),false,"UTF-8");Gdx.app.exit();cancel();}}},1.2f,.65f);}
    private void capture(String key){if(captured.add(key)){for(int i=0;i<12;i++)app.getScreen().render(.05f);Pixmap p=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());PixmapIO.writePNG(dir.child(key+".png"),p,-1,true);p.dispose();}}
    private void click(String name){Actor a=((BaseScreen)app.getScreen()).stage.getRoot().findActor(name);if(a instanceof TextButton b&&!b.isDisabled())b.fire(new ChangeListener.ChangeEvent());else throw new IllegalStateException("Cannot click "+name);}
    private static void collect(Actor a,List<Actor> out){out.add(a);if(a instanceof Group g&&!(a instanceof ScrollPane))for(Actor c:g.getChildren())collect(c,out);}
    private static Rectangle stageRect(Actor a,float x,float y,float w,float h){Vector2 p=a.localToStageCoordinates(new Vector2(x,y));return new Rectangle(p.x,p.y,w,h);}
    private static Rectangle textRect(Label l){
        if(l.getParent() instanceof TextButton b)b.validate();
        l.validate();l.layout();float w=l.getGlyphLayout().width,h=l.getGlyphLayout().height;int align=l.getLabelAlign();
        float x=(align&Align.left)!=0?0:(align&Align.right)!=0?l.getWidth()-w:(l.getWidth()-w)/2,y=(align&Align.top)!=0?l.getHeight()-h:(align&Align.bottom)!=0?0:(l.getHeight()-h)/2;
        return stageRect(l,x,y,w,h);
    }
    /** 글자가 테두리 안쪽과 모서리 장식을 침범하는지 기록한다. */
    private void audit(){
        List<Actor> all=new ArrayList<>();collect(((BaseScreen)app.getScreen()).stage.getRoot(),all);
        List<Actor> frames=new ArrayList<>();
        for(Actor a:all)if(a.isVisible()&&(a instanceof Window||a instanceof Image i&&i.getDrawable() instanceof OrnateFrame))frames.add(a);
        for(Actor a:all)if(a instanceof Window&&a.isVisible()&&(a.getX()<10||a.getY()<10||a.getTop()>BaseScreen.H-22||a.getRight()>BaseScreen.W-10))issues.add(app.getScreen().getClass().getSimpleName()+" | off-screen dialog | "+(int)a.getX()+","+(int)a.getY()+" "+(int)a.getWidth()+"x"+(int)a.getHeight());
        for(int i=0;i<frames.size();i++)for(int j=i+1;j<frames.size();j++){
            Actor a=frames.get(i),b=frames.get(j);if(a instanceof Window||b instanceof Window)continue;
            Rectangle ra=stageRect(a,0,0,a.getWidth(),a.getHeight()),rb=stageRect(b,0,0,b.getWidth(),b.getHeight());
            if(ra.overlaps(rb)&&!ra.contains(rb)&&!rb.contains(ra))issues.add(app.getScreen().getClass().getSimpleName()+" | frames overlap | "+(int)ra.x+","+(int)ra.y+" "+(int)ra.width+"x"+(int)ra.height+" / "+(int)rb.x+","+(int)rb.y+" "+(int)rb.width+"x"+(int)rb.height);
        }
        for(Actor a:all){
            if(!a.isVisible()||!a.ascendantsVisible())continue;
            Rectangle text;String name;Actor box=null;float insetX=14,insetY=8,corner=30;
            if(a instanceof ScrollPane){text=stageRect(a,0,0,a.getWidth(),a.getHeight());name="scroll";}
            else if(a instanceof Label l&&l.getText().length()>0&&l.getColor().a>0){
                text=textRect(l);name=l.getText().toString().replace('\n',' ');if(name.length()>26)name=name.substring(0,26);
                if(l.getParent() instanceof TextButton b){if(!(b.getStyle().up instanceof OrnateFrame))continue;box=b;insetX=20;insetY=0;corner=0;}
            }else continue;
            if(box==null){float best=Float.MAX_VALUE;for(Actor f:frames){Rectangle r=stageRect(f,0,0,f.getWidth(),f.getHeight());if(r.contains(text.x+text.width/2,text.y+text.height/2)&&r.area()<best&&r.area()>text.area()/2){best=r.area();box=f;}}}
            if(box==null)continue;
            Rectangle r=stageRect(box,0,0,box.getWidth(),box.getHeight());String problem=null;
            if(text.x<r.x+insetX||text.x+text.width>r.x+r.width-insetX)problem="sides";
            else if(text.y<r.y+insetY||text.y+text.height>r.y+r.height-insetY)problem="top/bottom";
            else if(corner>0&&(text.x<r.x+corner||text.x+text.width>r.x+r.width-corner)&&(text.y<r.y+corner||text.y+text.height>r.y+r.height-corner))problem="corner";
            if(problem!=null)issues.add(app.getScreen().getClass().getSimpleName()+" | "+problem+" | "+name+" | text "+(int)text.x+","+(int)text.y+" "+(int)text.width+"x"+(int)text.height+" in box "+(int)r.x+","+(int)r.y+" "+(int)r.width+"x"+(int)r.height);
        }
    }
    private void tick(){
        // 캡처가 오래 걸리면 밀린 틱이 연달아 돌므로, 게임이 응답할 프레임을 기다린다.
        if(Gdx.graphics.getFrameId()<nextFrame)return;
        nextFrame=Gdx.graphics.getFrameId()+20;
        audit();dir.child("AUDIT.txt").writeString(String.join("\n",issues),false,"UTF-8");
        if(++ticks>360)throw new IllegalStateException("Smoke tour timed out");
        if(app.getScreen() instanceof MainMenuScreen menu){
            if(menu.shotPending())return;
            capture("01-main");
            if(captured.add("help-opened")){click("help");return;}
            if(captured.add("help-closed")){capture("01-help");click("help-close");return;}
            click("start");return;
        }
        if(app.getScreen() instanceof CharacterSelectScreen){capture("02-character");click("character-confirm");return;}
        if(app.getScreen() instanceof ConnectionScreen){capture("03-connection");click("practice");return;}
        Snapshot s=app.state;if(s==null)return;
        switch(s.phase){
            case LOBBY->{capture("04-lobby");click(s.me().ready?"game-start":"ready");}
            case LOADOUT_SELECT->{capture("04-card-selection");if(!s.me().loadoutReady){click("loadout-REVERSE");click("loadout-SCOUT");capture("04-card-selection-two");click("loadout-confirm");}}
            case TILE_SELECT->{capture("05-game");if(!s.me().selected){int n=s.hand.get(s.hand.size()-1);click("tile-"+n);click("tile-confirm");}}
            case SPECIAL_SELECT->{capture("06-special");if(!s.me().specialReady)click("special-skip");}
            case ROUND_RESULT->{capture("07-round-result");if(!s.me().acknowledged)click("round-next");}
            case AMMO_LOAD->{RouletteScreen screen=(RouletteScreen)app.getScreen();capture("08-manual-load");if(screen.isAmmoPlacementReady()){
                int empty=java.util.stream.IntStream.range(0,9).filter(i->!s.loadedChambers.contains(i)).findFirst().orElseThrow();click("chamber-"+empty);click("chamber-confirm");}}
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
