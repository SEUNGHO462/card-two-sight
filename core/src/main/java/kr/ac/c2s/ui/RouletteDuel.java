package kr.ac.c2s.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.math.Interpolation;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.common.Protocol.Snapshot;
import kr.ac.c2s.game.ShotTimeline;

/** Six illustrated poses per character, independent of the authoritative game state. */
public final class RouletteDuel extends Actor {
    public final ShotTimeline timeline=new ShotTimeline();
    private final C2SGame app;
    private final Snapshot state;
    private final TextureRegion[][] frames=new TextureRegion[2][6];
    private boolean frozen;

    public RouletteDuel(C2SGame app,Snapshot state){
        this.app=app;this.state=state;
        for(int side=0;side<2;side++){
            int player=side==0?state.you:1-state.you;
            String posePath="characters/roulette/"+state.players[player].characterId+"-poses-v3.png";
            if(!com.badlogic.gdx.Gdx.files.internal(posePath).exists())posePath="characters/roulette/"+state.players[player].characterId+"-poses-v2.png";
            if(!com.badlogic.gdx.Gdx.files.internal(posePath).exists())posePath="characters/roulette/"+state.players[player].characterId+"-poses-v1.png";
            Texture texture=app.assets.texture(posePath);
            for(int frame=0;frame<6;frame++){
                // Use the gun-away-from-head shoulder pose for these two sheets.
                boolean shoulder=state.players[player].characterId.equals("male_handsome")||state.players[player].characterId.equals("female_leather");
                int source=frame==0&&shoulder?1:frame;
                int left=Math.round((source%3)*texture.getWidth()/3f);
                int right=Math.round((source%3+1)*texture.getWidth()/3f);
                int top=(source/3)*texture.getHeight()/2;
                // Generated poses can extend a few pixels into the next cell.
                // Sample an inset to keep neighbouring elbows out of this pose.
                int cellWidth=right-left;left+=Math.round(cellWidth*.06f);right-=Math.round(cellWidth*.025f);
                frames[side][frame]=new TextureRegion(texture,left,top,right-left,texture.getHeight()/2);
            }
        }
        setBounds(0,0,1600,900);setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    }
    @Override public void act(float delta){
        super.act(delta);
        if(!frozen&&timeline.advance(delta)&&app.records.sound){
            for(int side=0;side<2;side++){
                int p=side==0?state.you:1-state.you;
                app.assets.stereoSound(state.hit[p]?"trigger-shot-v2":"trigger-empty-v2",app.records.volume*.8f,side==0?-.65f:.65f);
            }
        }
    }
    public boolean hasHit(){return state.hit[0]||state.hit[1];}
    public void previewAt(float time){frozen=true;timeline.previewAt(time);}
    @Override public void draw(Batch batch,float parentAlpha){
        Color original=new Color(batch.getColor());float t=timeline.time();
        batch.setColor(.9f,.65f,.30f,.72f*parentAlpha);batch.draw(app.white,798,170,3,568);
        for(int side=0;side<2;side++){
            int p=side==0?state.you:1-state.you;boolean hit=state.hit[p];
            float x=side==0?120:940,y=185,size=540;
            float lean=hit&&t>ShotTimeline.FIRE?Interpolation.pow2Out.apply(Math.min(1,(t-ShotTimeline.FIRE)/.8f)):0;
            float rotation=(side==0?7:-7)*lean;
            y-=lean*24;
            int frame=timeline.frame(hit);
            batch.setColor(1,1,1,parentAlpha);
            batch.draw(frames[side][frame],x,y,size/2,70,size,size,1,1,rotation);
            // Brief cross-fades connect the three arm-lowering poses.
            float transition=frame==1?(t-1.32f)/.18f:frame==2?(t-1.78f)/.20f:1;
            if(!hit&&frame>0&&transition<1){
                batch.setColor(1,1,1,(1-transition)*parentAlpha);
                batch.draw(frames[side][frame-1],x,y,size,size);
            }
        }
        if(hasHit()&&timeline.flash()>0){batch.setColor(1,.88f,.66f,timeline.flash()*.27f*parentAlpha);batch.draw(app.white,0,0,1600,900);}
        batch.setColor(original);
    }
}
