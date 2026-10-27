package kr.ac.c2s.ui;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import kr.ac.c2s.C2SGame;

/** Scale-aware plaque frame. All Korean text remains live UI text. */
public final class MenuDecor {
    private MenuDecor(){}
    public static NinePatchDrawable plaque(C2SGame app,Color tint){
        TextureRegion r=app.assets.trimmed("ui/menu/button-antique-v1.png");
        NinePatch patch=new NinePatch(r,Math.min(280,r.getRegionWidth()/4),Math.min(280,r.getRegionWidth()/4),Math.min(85,r.getRegionHeight()/3),Math.min(85,r.getRegionHeight()/3));
        patch.setLeftWidth(48);patch.setRightWidth(48);patch.setTopHeight(19);patch.setBottomHeight(19);patch.setColor(tint);
        NinePatchDrawable d=new NinePatchDrawable(patch);d.setMinWidth(0);d.setMinHeight(0);return d;
    }
}
