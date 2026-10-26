package kr.ac.c2s.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Align;
import kr.ac.c2s.C2SGame;

/** Eight-second visual opening. Light timing is deliberately slow, not a strobe. */
public final class IntroScreen extends BaseScreen {
    private final Texture[] frames=new Texture[4];
    private final Texture glow;
    private float elapsed;
    private boolean finished,frozen;
    public IntroScreen(C2SGame app){
        super(app);
        String[] names={"01-police-lights","02-money-bag-arrival","03-open-bag","04-title"};
        for(int i=0;i<4;i++)frames[i]=app.assets.texture("intro/"+names[i]+".png");
        Pixmap p=new Pixmap(256,256,Pixmap.Format.RGBA8888);
        for(int y=0;y<256;y++)for(int x=0;x<256;x++){
            float r=(float)Math.sqrt((x-127.5f)*(x-127.5f)+(y-127.5f)*(y-127.5f))/128;
            p.setColor(1,1,1,(float)Math.pow(Math.max(0,1-r),2));p.drawPixel(x,y);
        }
        glow=new Texture(p);glow.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);p.dispose();
        label("클릭 또는 Enter · 건너뛰기",1040,24,520,36,"small",Align.right).setColor(.8f,.76f,.68f,.8f);
    }
    @Override public void show(){
        super.show();
        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean keyDown(int key){
                if(key==Input.Keys.F11){app.toggleFullscreen();return true;}
                if(key==Input.Keys.ENTER||key==Input.Keys.SPACE||key==Input.Keys.ESCAPE){finish();return true;}
                return false;
            }
            @Override public boolean touchDown(int x,int y,int pointer,int button){finish();return true;}
        });
    }
    private void finish(){if(!finished){finished=true;app.go(new MainMenuScreen(app));}}
    public void previewAt(float seconds){frozen=true;elapsed=seconds;}
    @Override public void render(float delta){
        if(finished)return;
        if(!frozen)elapsed+=Math.min(delta,.1f);
        if(elapsed>=8.4f){finish();return;}
        Gdx.gl.glClearColor(0,0,0,1);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);stage.getViewport().apply();
        Batch b=stage.getBatch();b.setProjectionMatrix(stage.getCamera().combined);b.begin();
        int scene=Math.min(3,(int)(elapsed/2));
        b.setColor(Color.WHITE);b.draw(frames[Math.max(0,scene-1)],0,0,W,H);
        b.setColor(1,1,1,Math.min(1,(elapsed-scene*2)/.45f));b.draw(frames[scene],0,0,W,H);
        // Fade down the baked-in reflections, then illuminate alternate windows.
        float strength=Math.max(0,1-elapsed/6);
        float red=(float)Math.pow(Math.max(0,Math.sin(elapsed*Math.PI*1.25)),2);
        float blue=(float)Math.pow(Math.max(0,-Math.sin(elapsed*Math.PI*1.25)),2);
        b.setColor(0,0,0,strength*(.58f-.1f*Math.max(red,blue)));b.draw(app.white,0,0,W,H);
        b.setColor(1,.035f,.04f,.8f*red*strength);b.draw(glow,-190,180,1000,1000);
        b.setColor(.02f,.18f,1,.85f*blue*strength);b.draw(glow,780,160,1050,1050);
        b.setColor(1,.02f,.03f,.035f*red*strength);b.draw(app.white,0,0,W,H);
        b.setColor(.02f,.12f,1,.035f*blue*strength);b.draw(app.white,0,0,W,H);
        float fade=elapsed<.65f?1-elapsed/.65f:elapsed>7.9f?(elapsed-7.9f)/.5f:0;
        b.setColor(0,0,0,Math.min(1,fade));b.draw(app.white,0,0,W,H);b.setColor(Color.WHITE);b.end();
        stage.act(Math.min(delta,.05f));stage.draw();
    }
    @Override public void dispose(){glow.dispose();super.dispose();}
}
