package kr.ac.c2s.screens;
import kr.ac.c2s.*;
import kr.ac.c2s.ui.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import static kr.ac.c2s.ui.Theme.*;

public abstract class BaseScreen extends ScreenAdapter {
    public static final float W=1600,H=900;
    public final Stage stage=new Stage(new FitViewport(W,H));
    protected final C2SGame app;
    protected final Group content=new Group();
    protected float time;
    protected float dim=.32f;
    private final Label toast;
    protected BaseScreen(C2SGame app) {
        this.app=app;stage.addActor(content);
        toast=new Label("",app.theme.skin,"small");toast.setAlignment(Align.center);toast.setBounds(200,10,1200,34);toast.setColor(GOLD);stage.addActor(toast);
    }
    @Override public void show() {
        Gdx.input.setInputProcessor(new InputMultiplexer(stage,new InputAdapter(){
            @Override public boolean keyDown(int key){if(key==Input.Keys.F11){app.toggleFullscreen();return true;}if(key==Input.Keys.ESCAPE){back();return true;}return false;}
        }));stage.getViewport().update(Gdx.graphics.getWidth(),Gdx.graphics.getHeight(),true);
    }
    protected void back(){app.confirmLeave();}
    protected void clear(){content.clearChildren();}
    public TextButton button(String text,float x,float y,float w,float h,String style,Runnable action) {
        TextButton b=new TextButton(text,app.theme.skin,style);b.setProgrammaticChangeEvents(false);b.setBounds(x,y,w,h);fitText(b);b.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor actor){if(!b.isDisabled()){app.sound("click");action.run();}}});content.addActor(b);return b;
    }
    /** 버튼 글자가 테두리 장식 안쪽 폭을 넘으면 줄인다. */
    public static void fitText(TextButton b){
        Label l=b.getLabel();l.setFontScale(1);float room=b.getWidth()-b.getPadLeft()-b.getPadRight();
        if(l.getPrefWidth()>room&&room>0)l.setFontScale(room/l.getPrefWidth());
    }
    protected Label label(String text,float x,float y,float w,float h,String style,int align) {
        Label l=new Label(text,app.theme.skin,style);l.setBounds(x,y,w,h);l.setAlignment(align);content.addActor(l);return l;
    }
    protected Label label(String text,float x,float y,float w,float h){return label(text,x,y,w,h,"default",Align.center);}
    protected Label gold(String text,float x,float y,float w,float h,String style){Label l=label(text,x,y,w,h,style,Align.center);l.setColor(GOLD);if(l.getPrefWidth()>w)l.setFontScale(w/l.getPrefWidth());return l;}
    protected Image panel(float x,float y,float w,float h){Image i=new Image(app.theme.frame(w,h));i.setBounds(x,y,w,h);content.addActor(i);return i;}
    protected Image image(String path,float x,float y,float w,float h){Image i=new Image(app.assets.texture(path));i.setScaling(Scaling.fit);i.setBounds(x,y,w,h);content.addActor(i);return i;}
    protected Image portrait(String id,float x,float y,float w,float h,boolean crop) {
        Texture t=app.assets.texture("characters/"+id+".png");TextureRegion r=new TextureRegion(t,0,0,t.getWidth(),crop?(int)(t.getHeight()*.73f):t.getHeight());
        Image i=new Image(r);i.setScaling(Scaling.fit);i.setBounds(x,y,w,h);content.addActor(i);return i;
    }
    protected TextField field(String initial,String hint,float x,float y,float w,float h){TextField f=new TextField(initial,app.theme.skin);f.setMessageText(hint);f.setBounds(x,y,w,h);content.addActor(f);return f;}
    /** 패널 제목 아래 얇은 장식선. */
    protected void rule(float x,float y,float w){image("ui/frames/divider-thin.png",x,y,w,20).setTouchable(Touchable.disabled);}
    /** 빨간 배너와 그 안의 글자. */
    protected Label banner(String text,float x,float y,float w,float h){
        Image i=new Image(app.theme.banner);i.setBounds(x,y,w,h);i.setTouchable(Touchable.disabled);content.addActor(i);
        return gold(text,x+70,y+8,w-140,h-16,"title");
    }
    /** 찢어진 양피지 태그와 그 위의 짙은 글자. */
    protected Label paperTag(String text,float x,float y,float w,float h){
        image("ui/frames/paper.png",x,y,w,h).setScaling(Scaling.stretch);
        Label l=label(text,x+56,y+4,w-112,h-8,"default",Align.center);l.setColor(Color.valueOf("2a1a10"));if(l.getPrefWidth()>w-112)l.setFontScale((w-112)/l.getPrefWidth());return l;
    }
    protected void paragraph(String text,float x,float y,float w,float h){Label l=label(text,x,y,w,h,"small",Align.topLeft);l.setWrap(true);}
    protected void scrollText(String text,float x,float y,float w,float h){Label l=new Label(text,app.theme.skin,"small");l.setWrap(true);Table t=new Table();t.top().left();t.add(l).width(w-40).top().left();ScrollPane s=new ScrollPane(t,app.theme.skin);s.setFadeScrollBars(false);s.setScrollingDisabled(true,false);s.setBounds(x+8,y+14,w-16,h-14);content.addActor(s);}
    protected void heading(String title,String sub){
        Image logo=new Image(new TextureRegionDrawable(app.assets.trimmed("ui/menu/logo-antique-v1.png")));logo.setScaling(Scaling.fit);logo.setAlign(Align.left);logo.setBounds(26,796,264,92);logo.setTouchable(Touchable.disabled);logo.setName("corner-brand-logo");content.addActor(logo);
        // 천장 조명 위에서도 제목과 안내문이 읽히도록 뒤를 어둡게 깐다.
        Image shade=image("ui/frames/heading-shade.png",270,735,1060,165);shade.setScaling(Scaling.stretch);shade.setTouchable(Touchable.disabled);
        gold(title,440,822,720,64,"heading");
        image("ui/frames/divider.png",510,796,580,30).setTouchable(Touchable.disabled);if(sub!=null){Label l=label(sub,350,756,900,36,"default",Align.center);l.setColor(Color.valueOf("fff2dc"));if(l.getPrefWidth()>900)l.setFontScale(900/l.getPrefWidth());}}
    protected HeartRow heartRow(int life,float x,float y,float w){HeartRow row=new HeartRow(app.assets.texture("ui/frames/heart.png"),life);row.setPosition(x+(w-row.getWidth())/2,y+4);content.addActor(row);return row;}
    /** 칩 그림을 왼쪽에 붙인 금색 글자. */
    protected Label chipLabel(String text,float x,float y,float w,float h,String style){
        Label l=gold(text,x+22,y,w-22,h,style);float left=x+w/2+11-l.getPrefWidth()/2;
        image("ui/frames/chips.png",left-46,y+h/2-18,42,36).setTouchable(Touchable.disabled);return l;
    }
    /** 패널 제목 뒤의 붉은 띠. */
    protected void headerBand(float x,float y,float w,float h){Image i=image("ui/frames/header-band.png",x,y,w,h);i.setScaling(Scaling.stretch);i.setTouchable(Touchable.disabled);}
    protected void matchHistory(java.util.List<String> entries,float x,float y,float w,float h){
        Label text=new Label(entries.isEmpty()?"완료된 라운드가 없습니다.":String.join("\n\n",entries),app.theme.skin,"tiny");
        text.setName("match-history-text");text.setWrap(true);text.setAlignment(Align.topLeft);
        Table table=new Table();table.top().left();table.add(text).width(w-20).top().left();
        ScrollPane pane=new ScrollPane(table,app.theme.skin);pane.setName("match-history-scroll");
        pane.setFadeScrollBars(false);pane.setScrollingDisabled(true,false);pane.setBounds(x,y,w,h);content.addActor(pane);
    }
    protected String hearts(int life){return "♥".repeat(Math.max(0,life))+"♡".repeat(Math.max(0,3-life));}
    protected Label heartLabel(int life,float x,float y,float w){Label l=label(hearts(life),x,y,w,42,"title",Align.center);l.setColor(RED);return l;}
    public Dialog titledDialog(String title,float width){
        Dialog d=new Dialog("",app.theme.skin);d.setMovable(false);d.getTitleTable().setVisible(false);
        d.getContentTable().pad(28,30,18,30);
        Label heading=new Label(title,app.theme.skin,"title");heading.setColor(GOLD);
        heading.setAlignment(Align.center);
        d.getContentTable().add(heading).width(width).center().padBottom(24).row();
        d.getButtonTable().defaults().width(220).height(60).pad(10);
        d.getButtonTable().pad(0,20,18,20);return d;
    }
    public void modal(String title,String text){
        // Put the heading inside the panel instead of on its top border.
        Dialog d=new Dialog("",app.theme.skin);
        d.setMovable(false);
        d.getTitleTable().setVisible(false);
        d.getContentTable().pad(30,36,12,36);
        Label heading=new Label(title,app.theme.skin,"title");
        heading.setColor(GOLD);
        d.getContentTable().add(heading).width(1000).left().padBottom(18).row();
        Label body=new Label(text,app.theme.skin,"body");
        body.setColor(Color.valueOf("fff2dc"));
        body.setWrap(true);
        body.setAlignment(Align.topLeft);
        // 글이 길어도 창이 화면을 넘지 않도록 본문 높이를 묶고 넘치면 스크롤한다.
        ScrollPane scroll=new ScrollPane(body,app.theme.skin);scroll.setFadeScrollBars(false);scroll.setScrollingDisabled(true,false);
        body.setWidth(1000);
        d.getContentTable().add(scroll).width(1000).height(Math.min(body.getPrefHeight(),620)).left();
        d.getButtonTable().defaults().minWidth(210).minHeight(60).pad(10);
        d.button("확인");
        d.getButtonTable().getChildren().peek().setName("help-close");
        d.getButtonTable().pad(10,20,20,20);
        d.show(stage);stage.setScrollFocus(scroll);
    }
    @Override public void render(float delta) {
        time+=delta;app.toastTime=Math.max(0,app.toastTime-delta);toast.setText(app.toastTime>0?app.toast:"");
        Gdx.gl.glClearColor(.025f,.015f,.01f,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);stage.getViewport().apply();
        Batch batch=stage.getBatch();batch.setProjectionMatrix(stage.getCamera().combined);batch.begin();batch.setColor(Color.WHITE);batch.draw(app.assets.texture("backgrounds/casino.png"),0,0,W,H);
        batch.setColor(0,0,0,dim+(float)Math.sin(time*.65f)*.015f);batch.draw(app.white,0,0,W,H);batch.setColor(Color.WHITE);batch.end();
        stage.act(Math.min(delta,.05f));stage.draw();
    }
    @Override public void resize(int width,int height){stage.getViewport().update(width,height,true);}
    @Override public void dispose(){stage.dispose();}
}
