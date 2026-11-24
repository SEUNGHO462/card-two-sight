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
    private final FreeTypeFontGenerator generator,boldGenerator,displayGenerator;
    private final OrnateFrame panelFrame,plateFrame,slimFrame;
    public final OrnateFrame banner;
    public Theme() {
        generator=new FreeTypeFontGenerator(Gdx.files.internal("fonts/GowunBatang-Regular.ttf"));
        boldGenerator=new FreeTypeFontGenerator(Gdx.files.internal("fonts/GowunBatang-Bold.ttf"));
        // 송명체는 기호와 일부 한글이 없어 닉네임이 들어가지 않는 고정 문구에만 쓴다.
        displayGenerator=new FreeTypeFontGenerator(Gdx.files.internal("fonts/SongMyung-Regular.ttf"));
        for(String spec:new String[]{"tiny:16","small:19","body:22","default:23","title:34","heading:40","large:50","hero:74"}) {
            String[] a=spec.split(":");FreeTypeFontGenerator.FreeTypeFontParameter p=new FreeTypeFontGenerator.FreeTypeFontParameter();
            boolean display=a[0].equals("heading")||a[0].equals("large")||a[0].equals("hero");
            p.size=Integer.parseInt(a[1]);p.incremental=true;p.characters=FreeTypeFontGenerator.DEFAULT_CHARS+(display?"":"♥♡◆·→←…");
            p.minFilter=Texture.TextureFilter.Linear;p.magFilter=Texture.TextureFilter.Linear;
            if(a[0].equals("body")){p.renderCount=3;p.spaceY=2;}
            p.color=Color.WHITE;p.borderWidth=0;p.shadowOffsetX=0;p.shadowOffsetY=0;
            if(!a[0].equals("body"))p.renderCount=2;
            BitmapFont font=(display?displayGenerator:a[0].equals("title")||a[0].equals("default")?boldGenerator:generator).generateFont(p);font.setUseIntegerPositions(false);skin.add(a[0],font);
            skin.add(a[0],new Label.LabelStyle(font,CREAM));
        }
        box("panel",Color.valueOf("17120fe8"),GOLD,2);
        panelFrame=frame("panel",65,45,0,0,18,7,70,false,true,true).padded(4,4).filled(region("panel-fill"),4);
        slimFrame=frame("slim",31,22,0,0,0,0,46,false,false,false).padded(6,12);
        plateFrame=frame("plate",37,28,1,0,8,7,46,true,true,true).padded(4,24);
        OrnateFrame plate=plateFrame;
        OrnateFrame goldPlate=frame("plate-gold",37,28,1,0,8,7,46,true,true,true).padded(4,24);
        OrnateFrame crest=frame("crest",52,31,1,1,9,1,58,true,true,false).padded(4,24);
        banner=frame("banner",42,21,3,5,5,0,41,true,true,false);
        Color rest=new Color(.9f,.9f,.9f,1),pressed=new Color(.7f,.7f,.7f,1),muted=new Color(.45f,.42f,.4f,.85f),ink=Color.valueOf("2a1a10");
        skin.add("resultRecord",banner,Drawable.class);
        Drawable up=box("up",Color.valueOf("2c1d13f5"),Color.valueOf("a87a37"),2);
        Drawable gold=box("primary",Color.valueOf("9c6422"),GOLD,3);
        TextButton.TextButtonStyle normal=new TextButton.TextButtonStyle(plate.tinted(rest),goldPlate,goldPlate,skin.getFont("default"));
        normal.over=plate;normal.fontColor=CREAM;normal.downFontColor=ink;normal.checkedFontColor=ink;normal.disabled=plate.tinted(muted);normal.disabledFontColor=Color.valueOf("8f887a");
        skin.add("default",normal);
        TextButton.TextButtonStyle primary=new TextButton.TextButtonStyle(normal);primary.up=crest.tinted(new Color(.9f,.9f,.9f,1));primary.over=crest;primary.down=crest.tinted(pressed);primary.checked=primary.down;primary.disabled=crest.tinted(muted);primary.downFontColor=null;primary.checkedFontColor=null;skin.add("primary",primary);
        TextButton.TextButtonStyle small=new TextButton.TextButtonStyle(normal);small.font=skin.getFont("small");skin.add("small",small);
        TextButton.TextButtonStyle hint=new TextButton.TextButtonStyle(small);hint.up=slimFrame;hint.over=slimFrame.tinted(new Color(1.2f,1.1f,.9f,1));hint.down=slimFrame.tinted(pressed);hint.checked=hint.up;hint.disabled=slimFrame.tinted(muted);hint.downFontColor=null;hint.checkedFontColor=null;skin.add("hint",hint);
        TextButton.TextButtonStyle chosen=new TextButton.TextButtonStyle(small);chosen.font=skin.getFont("default");chosen.up=goldPlate;chosen.over=goldPlate;chosen.fontColor=ink;chosen.overFontColor=ink;skin.add("chosen",chosen);
        TextButton.TextButtonStyle odd=new TextButton.TextButtonStyle(normal);odd.up=box("odd",Color.valueOf("e8d1a6"),GOLD,3);odd.font=skin.getFont("large");odd.fontColor=Color.valueOf("251c16");odd.checkedFontColor=GOLD;skin.add("odd",odd);
        TextButton.TextButtonStyle even=new TextButton.TextButtonStyle(normal);even.font=skin.getFont("large");skin.add("even",even);
        TextButton.TextButtonStyle circle=new TextButton.TextButtonStyle(normal);circle.up=circle("circle",Color.valueOf("18130f"),GOLD);circle.over=circle("circleOver",Color.valueOf("47311c"),GOLD);circle.checked=circle("circleChecked",Color.valueOf("916020"),CREAM);circle.down=circle.checked;skin.add("circle",circle);
        TextField.TextFieldStyle field=new TextField.TextFieldStyle();field.font=skin.getFont("default");field.fontColor=CREAM;field.messageFontColor=Color.GRAY;field.background=slimFrame;
        field.cursor=box("cursor",GOLD,GOLD,0);field.selection=box("selection",Color.valueOf("6a4b2e"),GOLD,0);skin.add("default",field);
        Window.WindowStyle window=new Window.WindowStyle(skin.getFont("title"),GOLD,panelFrame);window.stageBackground=box("modalShade",new Color(0,0,0,.7f),new Color(0,0,0,.7f),0);skin.add("default",window);
        ScrollPane.ScrollPaneStyle scroll=new ScrollPane.ScrollPaneStyle();scroll.vScrollKnob=gold;skin.add("default",scroll);
        Slider.SliderStyle slider=new Slider.SliderStyle();slider.background=up;slider.knob=gold;slider.knob.setMinWidth(20);slider.knob.setMinHeight(24);skin.add("default-horizontal",slider);
    }
    private TextureRegion region(String path){
        Texture t=new Texture(Gdx.files.internal("ui/frames/"+path+".png"));t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);skin.add(path+"Frame",t);return new TextureRegion(t);
    }
    private OrnateFrame frame(String name,int capX,int capY,float rimLeft,float rimRight,float rimTop,float rimBottom,float natural,boolean fitHeight,boolean top,boolean bottom){
        NinePatch patch=new NinePatch(region(name).getTexture(),capX,capX,capY,capY);
        return new OrnateFrame(patch,top?region(name+"-top"):null,bottom?region(name+"-bottom"):null,rimLeft,rimRight,rimTop,rimBottom,natural,fitHeight,Color.WHITE);
    }
    /** 박스 비율에 맞는 테두리를 고른다. */
    public Drawable frame(float w,float h){return h<=72?plateFrame:panelFrame;}
    private Drawable box(String key,Color fill,Color edge,int width) {
        Pixmap p=new Pixmap(16,16,Pixmap.Format.RGBA8888);p.setColor(edge);p.fill();p.setColor(fill);p.fillRectangle(width,width,16-width*2,16-width*2);
        Texture t=new Texture(p);p.dispose();skin.add(key+"Texture",t);return new NinePatchDrawable(new NinePatch(t,Math.max(4,width+1),Math.max(4,width+1),Math.max(4,width+1),Math.max(4,width+1)));
    }
    private Drawable circle(String key,Color fill,Color edge) {
        Pixmap p=new Pixmap(80,80,Pixmap.Format.RGBA8888);p.setColor(0,0,0,0);p.fill();p.setColor(edge);p.fillCircle(40,40,39);p.setColor(fill);p.fillCircle(40,40,35);
        Texture t=new Texture(p);p.dispose();skin.add(key+"Texture",t);return new TextureRegionDrawable(new TextureRegion(t));
    }
    public Drawable panel() { return skin.get("panelTexture",Texture.class)==null?null:new NinePatchDrawable(new NinePatch(skin.get("panelTexture",Texture.class),4,4,4,4)); }
    @Override public void dispose() { skin.dispose();generator.dispose();boldGenerator.dispose();displayGenerator.dispose(); }
}
