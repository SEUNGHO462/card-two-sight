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
        TextButton b=new TextButton(text,app.theme.skin,style);b.setProgrammaticChangeEvents(false);b.setBounds(x,y,w,h);b.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor actor){if(!b.isDisabled()){app.sound("click");action.run();}}});content.addActor(b);return b;
    }
    protected Label label(String text,float x,float y,float w,float h,String style,int align) {
        Label l=new Label(text,app.theme.skin,style);l.setBounds(x,y,w,h);l.setAlignment(align);content.addActor(l);return l;
    }
    protected Label label(String text,float x,float y,float w,float h){return label(text,x,y,w,h,"default",Align.center);}
    protected Label gold(String text,float x,float y,float w,float h,String style){Label l=label(text,x,y,w,h,style,Align.center);l.setColor(GOLD);if(l.getPrefWidth()>w)l.setFontScale(w/l.getPrefWidth());return l;}
    protected Image panel(float x,float y,float w,float h){Image i=new Image(app.theme.panel());i.setBounds(x,y,w,h);content.addActor(i);return i;}
    protected Image image(String path,float x,float y,float w,float h){Image i=new Image(app.assets.texture(path));i.setScaling(Scaling.fit);i.setBounds(x,y,w,h);content.addActor(i);return i;}
    protected Image portrait(String id,float x,float y,float w,float h,boolean crop) {
        Texture t=app.assets.texture("characters/"+id+".png");TextureRegion r=new TextureRegion(t,0,0,t.getWidth(),crop?(int)(t.getHeight()*.73f):t.getHeight());
        Image i=new Image(r);i.setScaling(Scaling.fit);i.setBounds(x,y,w,h);content.addActor(i);return i;
    }
    protected TextField field(String initial,String hint,float x,float y,float w,float h){TextField f=new TextField(initial,app.theme.skin);f.setMessageText(hint);f.setBounds(x,y,w,h);content.addActor(f);return f;}
    protected void paragraph(String text,float x,float y,float w,float h){Label l=label(text,x,y,w,h,"small",Align.topLeft);l.setWrap(true);}
    protected void scrollText(String text,float x,float y,float w,float h){Label l=new Label(text,app.theme.skin,"small");l.setWrap(true);Table t=new Table();t.top().left();t.add(l).width(w-24).top().left();ScrollPane s=new ScrollPane(t,app.theme.skin);s.setFadeScrollBars(false);s.setScrollingDisabled(true,false);s.setBounds(x,y,w,h);content.addActor(s);}
    protected void heading(String title,String sub){gold("CARD TWO SIGHT",35,844,300,36,"small");gold(title,440,800,720,64,"title");if(sub!=null)label(sub,350,758,900,36,"small",Align.center);}
    protected String hearts(int life){return "♥".repeat(Math.max(0,life))+"♡".repeat(Math.max(0,3-life));}
    protected Label heartLabel(int life,float x,float y,float w){Label l=label(hearts(life),x,y,w,42,"title",Align.center);l.setColor(RED);return l;}
    public Dialog titledDialog(String title,float width){
        Dialog d=new Dialog("",app.theme.skin);d.setMovable(false);d.getTitleTable().setVisible(false);
        d.getContentTable().pad(28,30,18,30);
        Label heading=new Label(title,app.theme.skin,"title");heading.setColor(GOLD);
        d.getContentTable().add(heading).width(width).left().padBottom(24).row();
        d.getButtonTable().defaults().minWidth(180).minHeight(58).pad(10);
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
        d.getContentTable().add(heading).width(880).left().padBottom(24).row();
        Label body=new Label(text,app.theme.skin,"body");
        body.setColor(Color.valueOf("fff2dc"));
        body.setWrap(true);
        body.setAlignment(Align.topLeft);
        d.getContentTable().add(body).width(880).left();
        d.getButtonTable().defaults().minWidth(210).minHeight(60).pad(10);
        d.button("확인");
        d.getButtonTable().getChildren().peek().setName("help-close");
        d.getButtonTable().pad(10,20,20,20);
        d.show(stage);
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
