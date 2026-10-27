package kr.ac.c2s.ui;
import kr.ac.c2s.C2SGame;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

/** Runtime layers preserve readable card typography; no source image is painted over. */
public final class CardArt extends Group {
    public CardArt(C2SGame app,String path,float x,float y,float w,float h){
        setBounds(x,y,w,h);setTouchable(Touchable.enabled);
        Image base=new Image(app.assets.texture(path));base.setScaling(Scaling.fit);base.setBounds(0,0,w,h);addActor(base);
        if(path.contains("specials/")){
            addDrop(app,w*.065f,h*.77f,w*.23f,w*.23f,14);
            addDrop(app,w*.80f,h*.045f,w*.15f,w*.15f,-32);
        }
    }
    private void addDrop(C2SGame app,float x,float y,float w,float h,float rotation){
        Image blood=new Image(new TextureRegionDrawable(app.assets.trimmed("ui/menu/blood-splatter-v1.png")));
        blood.setBounds(x,y,w,h);blood.setOrigin(w/2,h/2);blood.setRotation(rotation);blood.setColor(.68f,.55f,.55f,.9f);blood.setTouchable(Touchable.disabled);addActor(blood);
    }
}
