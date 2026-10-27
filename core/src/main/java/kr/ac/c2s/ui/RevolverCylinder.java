package kr.ac.c2s.ui;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.game.CylinderPreparation;
import java.util.Map;
import java.util.function.IntConsumer;
import static kr.ac.c2s.common.Protocol.CHAMBERS;

/** Seven sockets. Visible caps during loading show counts, not the hidden firing layout. */
public final class RevolverCylinder extends Group {
    private final CylinderArt art;
    private final CylinderPreparation flow;
    private final Group rotating=new Group();
    private final TextButton[] sockets=new TextButton[CHAMBERS];
    private final Image[] rings=new Image[CHAMBERS],caps=new Image[CHAMBERS];
    private final float size;
    private final int selected;
    private final java.util.Set<Integer> occupied;
    private float pulse;
    public RevolverCylinder(C2SGame app,CylinderArt art,CylinderPreparation flow,float x,float y,float size,int selected,int liveCount,Map<Integer,Boolean> known,IntConsumer choose){
        this(app,art,flow,x,y,size,selected,liveCount,known,choose,java.util.stream.IntStream.range(0,flow.before()).boxed().toList());
    }
    public RevolverCylinder(C2SGame app,CylinderArt art,CylinderPreparation flow,float x,float y,float size,int selected,int liveCount,Map<Integer,Boolean> known,IntConsumer choose,java.util.List<Integer> occupied){
        this.art=art;this.flow=flow;this.size=size;this.selected=selected;this.occupied=new java.util.HashSet<>(occupied);
        setBounds(x,y,size,size);rotating.setSize(size,size);rotating.setOrigin(size/2,size/2);addActor(rotating);
        Image plate=new Image(app.assets.texture("revolver/wheel.png"));plate.setBounds(0,0,size,size);rotating.addActor(plate);
        for(int i=0;i<CHAMBERS;i++){
            final int n=i;float angle=(float)Math.toRadians(90-i*360f/CHAMBERS),radius=size*.355f,b=size*.166f;
            float bx=size/2+(float)Math.cos(angle)*radius-b/2,by=size/2+(float)Math.sin(angle)*radius-b/2;
            Image ring=new Image(art.ring);ring.setBounds(bx-5,by-5,b+10,b+10);rings[i]=ring;rotating.addActor(ring);
            TextButton.TextButtonStyle style=new TextButton.TextButtonStyle();style.font=app.theme.skin.getFont("default");
            style.up=new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(art.bore));style.down=style.up;style.checked=style.up;style.disabled=style.up;
            style.fontColor=Theme.CREAM;style.disabledFontColor=Theme.CREAM;
            TextButton button=new TextButton(String.valueOf(i+1),style);button.setProgrammaticChangeEvents(false);button.setBounds(bx,by,b,b);button.setName("chamber-"+i);
            if(known.containsKey(i)){button.setText((i+1)+"\n"+(known.get(i)?"실탄":"빈칸"));button.getLabel().setFontScale(.73f);button.getLabel().setColor(known.get(i)?Theme.RED:Theme.MINT);}
            button.addListener(new ChangeListener(){@Override public void changed(ChangeEvent event,Actor actor){if(flow.canChoose()||flow.canPlace()&&!occupied.contains(n)){app.sound("click");choose.accept(n);}}});
            sockets[i]=button;rotating.addActor(button);
            Image cap=new Image(art.cap);cap.setBounds(bx+5,by+5,b-10,b-10);cap.setTouchable(Touchable.disabled);caps[i]=cap;rotating.addActor(cap);
        }
        Label pointer=new Label("▼",app.theme.skin,"title");pointer.setColor(Theme.GOLD);pointer.setAlignment(com.badlogic.gdx.utils.Align.center);pointer.setBounds(size/2-28,size-43,56,42);addActor(pointer);
        sync();
    }
    private void sync(){
        rotating.setRotation(flow.rotation());boolean choosing=flow.canChoose();
        boolean showLoaded=flow.canPlace()||flow.phase()==CylinderPreparation.Phase.LOADING||flow.phase()==CylinderPreparation.Phase.SPINNING&&flow.time()<.20f;
        for(int i=0;i<CHAMBERS;i++){
            sockets[i].setDisabled(!(choosing||flow.canPlace()&&!occupied.contains(i)));sockets[i].getLabel().setVisible(choosing||flow.canPlace());
            caps[i].setVisible(showLoaded&&(occupied.contains(i)||i==flow.loadTarget()&&flow.loaded()>flow.before()));
            rings[i].setVisible(i==selected&&(choosing||flow.canPlace()));
            rings[i].setColor(1,1,1,.75f+.25f*(float)Math.sin(pulse*3));
        }
    }
    @Override public void act(float delta){super.act(delta);pulse+=delta;sync();}
    @Override public void draw(Batch batch,float parentAlpha){
        sync();super.draw(batch,parentAlpha);
        if(flow.phase()!=CylinderPreparation.Phase.LOADING||flow.loaded()>=flow.before()+flow.added()||flow.time()<.35f)return;
        float f=flow.insertionProgress(),ease=1-(1-f)*(1-f);
        float a=(float)Math.toRadians(90-flow.loadTarget()*360f/CHAMBERS);
        float tx=size/2+(float)Math.cos(a)*size*.355f,ty=size/2+(float)Math.sin(a)*size*.355f;
        float sx=size+45,sy=size*.66f,scale=1-.55f*ease;
        Color previous=new Color(batch.getColor());batch.setColor(1,1,1,parentAlpha);
        batch.draw(art.live,getX()+sx+(tx-sx)*ease-28*scale,getY()+sy+(ty-sy)*ease-66*scale,56*scale,132*scale);batch.setColor(previous);
    }
}
