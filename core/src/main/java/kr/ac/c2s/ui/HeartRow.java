package kr.ac.c2s.ui;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

/** 목숨 3칸. 잃은 목숨은 어두운 빈 하트로 남는다. */
public final class HeartRow extends Group {
    private static final float SIZE=36,GAP=8;
    private static final Color LOST=new Color(.2f,.15f,.15f,.8f);
    private final Image[] hearts=new Image[3];
    public HeartRow(Texture heart,int life){
        setSize(SIZE*3+GAP*2,SIZE);setTouchable(Touchable.disabled);
        for(int i=0;i<3;i++){hearts[i]=new Image(heart);hearts[i].setBounds(i*(SIZE+GAP),0,SIZE,SIZE);addActor(hearts[i]);}
        setLife(life);
    }
    public void setLife(int life){for(int i=0;i<3;i++)hearts[i].setColor(i<life?Color.WHITE:LOST);}
    public void flash(){addAction(Actions.sequence(Actions.alpha(.3f,.08f),Actions.alpha(1,.3f)));}
}
