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
    /** 게임 전체의 강조(primary) 버튼을 메인 메뉴 '테이블에 앉기'와 같은 명판 배경으로 바꾼다. */
    public static void applyToPrimary(C2SGame app){
        var style=app.theme.skin.get("primary",com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle.class);
        style.up=plaque(app,Color.WHITE);
        style.over=plaque(app,new Color(1,.93f,.80f,1));
        style.down=plaque(app,new Color(.82f,.70f,.60f,1));style.checked=style.down;
        style.disabled=plaque(app,new Color(.42f,.38f,.36f,.9f));
        style.fontColor=Theme.CREAM;style.overFontColor=Color.WHITE;style.downFontColor=null;style.checkedFontColor=null;
    }
}
