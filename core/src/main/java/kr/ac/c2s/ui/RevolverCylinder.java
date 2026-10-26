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

/** Nine true interactive sockets. Only purchased inspections can reveal a socket. */
public final class RevolverCylinder extends Group {
    private final C2SGame app;
    private final CylinderArt art;
    private final CylinderPreparation flow;
    private final Group rotating=new Group();
    private final TextButton[] sockets=new TextButton[9];
    private final Image[] rings=new Image[9];
    private final float size;
    private final int selected,liveCount;
    private float pulse;
    public RevolverCylinder(C2SGame app,CylinderArt art,CylinderPreparation flow,float x,float y,float size,int selected,int liveCount,Map<Integer,Boolean> known,IntConsumer choose){
        this.app=app;this.art=art;this.flow=flow;this.size=size;this.selected=selected;this.liveCount=liveCount;
        setBounds(x,y,size,size);rotating.setSize(size,size);rotating.setOrigin(size/2,size/2);addActor(rotating);
        Image plate=new Image(app.assets.texture("revolver/wheel.png"));plate.setBounds(0,0,size,size);rotating.addActor(plate);
        for(int i=0;i<9;i++){
            final int n=i;float angle=(float)Math.toRadians(90-i*40),radius=size*.355f,b=size*.166f;
            float bx=size/2+(float)Math.cos(angle)*radius-b/2,by=size/2+(float)Math.sin(angle)*radius-b/2;
            Image ring=new Image(art.ring);ring.setBounds(bx-5,by-5,b+10,b+10);rings[i]=ring;rotating.addActor(ring);
            TextButton.TextButtonStyle style=new TextButton.TextButtonStyle();style.font=app.theme.skin.getFont("default");
            style.up=new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(art.bore));style.down=style.up;style.checked=style.up;style.disabled=style.up;
            style.fontColor=Theme.CREAM;style.disabledFontColor=Theme.CREAM;
            String text=String.valueOf(i+1);
            TextButton button=new TextButton(text,style);button.setProgrammaticChangeEvents(false);button.setBounds(bx,by,b,b);button.setName("chamber-"+i);
            if(known.containsKey(i)){button.setText((i+1)+"\n"+(known.get(i)?"실탄":"공탄"));button.getLabel().setFontScale(.73f);button.getLabel().setColor(known.get(i)?Theme.RED:Theme.MINT);}
            button.addListener(new ChangeListener(){@Override public void changed(ChangeEvent event,Actor actor){if(flow.canChoose()){app.sound("click");choose.accept(n);}}});
            sockets[i]=button;rotating.addActor(button);
        }
        // The fixed gold marker indicates the firing position after alignment.
        Label pointer=new Label("▼",app.theme.skin,"title");pointer.setColor(Theme.GOLD);pointer.setAlignment(com.badlogic.gdx.utils.Align.center);pointer.setBounds(size/2-28,size-8,56,42);addActor(pointer);
        sync();
    }
    private void sync(){
        rotating.setRotation(flow.rotation());
        boolean choosing=flow.canChoose();
        for(int i=0;i<9;i++){
            sockets[i].setDisabled(!choosing);sockets[i].getLabel().setVisible(choosing);
            rings[i].setVisible(i==selected&&(choosing||flow.phase()==CylinderPreparation.Phase.ALIGNING||flow.phase()==CylinderPreparation.Phase.LOCKED));
            rings[i].setColor(1,1,1,.62f+.38f*(float)Math.sin(pulse*3.5f)*.5f+.19f);
        }
    }
    @Override public void act(float delta){super.act(delta);pulse+=delta;sync();}
    @Override public void draw(Batch batch,float parentAlpha){
        sync();super.draw(batch,parentAlpha);
        if(flow.phase()!=CylinderPreparation.Phase.LOADING)return;
        float t=flow.time()-.2f;int index=(int)Math.floor(t/CylinderPreparation.STEP);if(index<0||index>=9)return;
        float f=(t-index*CylinderPreparation.STEP)/CylinderPreparation.STEP,approach=1-(1-f)*(1-f);
        // Loading is a count-only cutaway. Numbers appear after the concealed spin.
        float tx=size*.621f,ty=size*.166f;
        float sx=size+26,sy=size*.60f;
        float scale=1-.70f*approach;
        Color previous=new Color(batch.getColor());batch.setColor(1,1,1,Math.min(1,(1-f)*4)*parentAlpha);
        batch.draw(index<liveCount?art.live:art.blank,getX()+sx+(tx-sx)*approach-28*scale,getY()+sy+(ty-sy)*approach-66*scale,56*scale,132*scale);
        batch.setColor(previous);
    }
}
