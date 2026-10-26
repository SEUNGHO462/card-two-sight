package kr.ac.c2s.ui;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Disposable;

/** 기능성 버튼·패널은 코드로 그려 해상도와 입력 상태에 맞게 표시한다. */
public final class Theme implements Disposable {
    public static final Color GOLD=Color.valueOf("e6b663"), CREAM=Color.valueOf("f3dfbd"), RED=Color.valueOf("e66c61"), MINT=Color.valueOf("8bceb0");
    public final Skin skin=new Skin();
    private final FreeTypeFontGenerator generator;
    public Theme() {
        generator=new FreeTypeFontGenerator(Gdx.files.internal("fonts/NanumGothic-Regular.ttf"));
        for(String spec:new String[]{"tiny:16","small:19","body:26","default:23","title:34","large:50","hero:74"}) {
            String[] a=spec.split(":");FreeTypeFontGenerator.FreeTypeFontParameter p=new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size=Integer.parseInt(a[1]);p.incremental=true;p.characters=FreeTypeFontGenerator.DEFAULT_CHARS+"♥♡◆·→←…✓";
            p.minFilter=Texture.TextureFilter.Linear;p.magFilter=Texture.TextureFilter.Linear;
            if(a[0].equals("body")){p.renderCount=3;p.spaceY=6;}
            p.color=Color.WHITE;p.borderWidth=0;p.shadowOffsetX=0;p.shadowOffsetY=0;
            BitmapFont font=generator.generateFont(p);font.setUseIntegerPositions(false);skin.add(a[0],font);
            skin.add(a[0],new Label.LabelStyle(font,CREAM));
        }
        Drawable panel=box("panel",Color.valueOf("17120fe8"),GOLD,2);
        Drawable up=box("up",Color.valueOf("2c1d13f5"),Color.valueOf("a87a37"),2);
        Drawable over=box("over",Color.valueOf("4e321cf8"),GOLD,3);
        Drawable down=box("down",Color.valueOf("765025"),GOLD,3);
        Drawable disabled=box("disabled",Color.valueOf("1d1a17de"),Color.valueOf("675642"),2);
        Drawable gold=box("primary",Color.valueOf("9c6422"),GOLD,3);
        TextButton.TextButtonStyle normal=new TextButton.TextButtonStyle(up,down,down,skin.getFont("default"));
        normal.over=over;normal.fontColor=CREAM;normal.disabled=disabled;normal.disabledFontColor=Color.valueOf("8f887a");
        skin.add("default",normal);
        TextButton.TextButtonStyle primary=new TextButton.TextButtonStyle(normal);primary.up=gold;skin.add("primary",primary);
        TextButton.TextButtonStyle small=new TextButton.TextButtonStyle(normal);small.font=skin.getFont("small");skin.add("small",small);
        TextButton.TextButtonStyle odd=new TextButton.TextButtonStyle(normal);odd.up=box("odd",Color.valueOf("e8d1a6"),GOLD,3);odd.font=skin.getFont("large");odd.fontColor=Color.valueOf("251c16");odd.checkedFontColor=GOLD;skin.add("odd",odd);
        TextButton.TextButtonStyle even=new TextButton.TextButtonStyle(normal);even.font=skin.getFont("large");skin.add("even",even);
        TextButton.TextButtonStyle circle=new TextButton.TextButtonStyle(normal);circle.up=circle("circle",Color.valueOf("18130f"),GOLD);circle.over=circle("circleOver",Color.valueOf("47311c"),GOLD);circle.checked=circle("circleChecked",Color.valueOf("916020"),CREAM);circle.down=circle.checked;skin.add("circle",circle);
        TextField.TextFieldStyle field=new TextField.TextFieldStyle();field.font=skin.getFont("default");field.fontColor=CREAM;field.messageFontColor=Color.GRAY;field.background=panel;
        field.cursor=box("cursor",GOLD,GOLD,0);field.selection=box("selection",Color.valueOf("6a4b2e"),GOLD,0);skin.add("default",field);
        Window.WindowStyle window=new Window.WindowStyle(skin.getFont("title"),GOLD,panel);window.stageBackground=box("modalShade",new Color(0,0,0,.7f),new Color(0,0,0,.7f),0);skin.add("default",window);
        ScrollPane.ScrollPaneStyle scroll=new ScrollPane.ScrollPaneStyle();scroll.vScrollKnob=gold;skin.add("default",scroll);
        Slider.SliderStyle slider=new Slider.SliderStyle();slider.background=up;slider.knob=gold;slider.knob.setMinWidth(20);slider.knob.setMinHeight(24);skin.add("default-horizontal",slider);
    }
    private Drawable box(String key,Color fill,Color edge,int width) {
        Pixmap p=new Pixmap(16,16,Pixmap.Format.RGBA8888);p.setColor(edge);p.fill();p.setColor(fill);p.fillRectangle(width,width,16-width*2,16-width*2);
        Texture t=new Texture(p);p.dispose();skin.add(key+"Texture",t);return new NinePatchDrawable(new NinePatch(t,4,4,4,4));
    }
    private Drawable circle(String key,Color fill,Color edge) {
        Pixmap p=new Pixmap(80,80,Pixmap.Format.RGBA8888);p.setColor(0,0,0,0);p.fill();p.setColor(edge);p.fillCircle(40,40,39);p.setColor(fill);p.fillCircle(40,40,35);
        Texture t=new Texture(p);p.dispose();skin.add(key+"Texture",t);return new TextureRegionDrawable(new TextureRegion(t));
    }
    public Drawable panel() { return skin.get("panelTexture",Texture.class)==null?null:new NinePatchDrawable(new NinePatch(skin.get("panelTexture",Texture.class),4,4,4,4)); }
    @Override public void dispose() { skin.dispose();generator.dispose(); }
}
