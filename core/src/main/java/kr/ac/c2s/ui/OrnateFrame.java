package kr.ac.c2s.ui;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;

/** 장식 테두리 나인패치. 모서리 곡선은 박스 밖으로 살짝 나가고 가운데 보석은 늘리지 않고 따로 그린다. */
public final class OrnateFrame extends BaseDrawable {
    private final NinePatch patch;private final TextureRegion top,bottom;
    private final float rimLeft,rimRight,rimTop,rimBottom,natural;private final boolean fitHeight;
    private final Color tint;
    private TextureRegion fill;private float fillInset;
    /** 프레임 안쪽에 깔 질감. 가장자리가 투명하게 번져 있어 테두리 장식은 그대로 보인다. */
    public OrnateFrame filled(TextureRegion texture,float inset){fill=texture;fillInset=inset;return this;}
    public OrnateFrame(NinePatch patch,TextureRegion top,TextureRegion bottom,float rimLeft,float rimRight,float rimTop,float rimBottom,float natural,boolean fitHeight,Color tint){
        this.patch=new NinePatch(patch,tint);this.top=top;this.bottom=bottom;this.rimLeft=rimLeft;this.rimRight=rimRight;this.rimTop=rimTop;this.rimBottom=rimBottom;this.natural=natural;this.fitHeight=fitHeight;this.tint=new Color(tint);
    }
    public OrnateFrame tinted(Color next){OrnateFrame f=new OrnateFrame(patch,top,bottom,rimLeft,rimRight,rimTop,rimBottom,natural,fitHeight,next);f.fill=fill;f.fillInset=fillInset;f.setPadding(getTopHeight(),getLeftWidth(),getBottomHeight(),getRightWidth());return f;}
    public OrnateFrame padded(float vertical,float horizontal){setPadding(vertical,horizontal,vertical,horizontal);return this;}
    @Override public void draw(Batch batch,float x,float y,float width,float height){
        float s=fitHeight?Math.min(width/patch.getTotalWidth(),Math.max(.6f,Math.min(1.25f,height/natural))):Math.min(1,Math.min(width/patch.getTotalWidth(),height/natural));
        float left=x-rimLeft*s,bottomY=y-rimBottom*s,w=width+(rimLeft+rimRight)*s,h=height+(rimTop+rimBottom)*s;
        patch.draw(batch,left,bottomY,0,0,w/s,h/s,s,s,0);
        float r=batch.getColor().r,g=batch.getColor().g,b=batch.getColor().b,a=batch.getColor().a;
        batch.setColor(r*tint.r,g*tint.g,b*tint.b,a*tint.a);
        if(fill!=null)batch.draw(fill,x+fillInset,y+fillInset,width-fillInset*2,height-fillInset*2);
        if(top!=null)batch.draw(top,left+(w-top.getRegionWidth()*s)/2,bottomY+h-top.getRegionHeight()*s,top.getRegionWidth()*s,top.getRegionHeight()*s);
        if(bottom!=null)batch.draw(bottom,left+(w-bottom.getRegionWidth()*s)/2,bottomY,bottom.getRegionWidth()*s,bottom.getRegionHeight()*s);
        batch.setColor(r,g,b,a);
    }
}
