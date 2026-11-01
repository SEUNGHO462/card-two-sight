package kr.ac.c2s.ui;
import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.util.*;

public final class GameAssets implements Disposable {
    private final Map<String,Texture> textures=new HashMap<>();
    private final Map<String,Sound> sounds=new HashMap<>();
    private final Map<String,TextureRegion> trimmed=new HashMap<>();
    private Music bgm;
    private String resolved(String path){
        if(path.equals("cards/blank.png"))return "cards/blank-droplets-v2.png";
        if(path.startsWith("cards/specials/")&&!path.contains("-droplets")){
            String revised=path.replace(".png","-droplets-v2.png");if(Gdx.files.internal(revised).exists())return revised;
        }
        return path;
    }
    public TextureRegion trimmed(String path){
        return trimmed.computeIfAbsent(path,key->{
            Pixmap pix=new Pixmap(Gdx.files.internal(resolved(key)));int minX=pix.getWidth(),minY=pix.getHeight(),maxX=-1,maxY=-1;
            for(int y=0;y<pix.getHeight();y++)for(int x=0;x<pix.getWidth();x++)if((pix.getPixel(x,y)&255)>16){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
            pix.dispose();return maxX<0?new TextureRegion(texture(key)):new TextureRegion(texture(key),minX,minY,maxX-minX+1,maxY-minY+1);
        });
    }
    public Texture texture(String path) {
        return textures.computeIfAbsent(path,p->{ Texture t=new Texture(Gdx.files.internal(resolved(p)));t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);return t; });
    }
    public void sound(String name,float volume) {
        if(volume<=0 || Boolean.getBoolean("c2s.noAudio")) return;
        try { sounds.computeIfAbsent(name,n->Gdx.audio.newSound(Gdx.files.internal("audio/"+n+".wav"))).play(volume); }
        catch(RuntimeException ignored) { /* 무음 장치에서도 게임을 계속한다. */ }
    }
    public void stereoSound(String name,float volume,float pan){
        if(volume<=0||Boolean.getBoolean("c2s.noAudio"))return;
        try{sounds.computeIfAbsent(name,n->Gdx.audio.newSound(Gdx.files.internal("audio/"+n+".wav"))).play(volume,1,pan);}
        catch(RuntimeException ignored){}
    }
    private FileHandle musicFile(){FileHandle file=Gdx.files.internal("audio/bgm.ogg");return file.exists()?file:Gdx.files.internal("audio/bgm.mp3");}
    public boolean hasMusic(){return musicFile().exists();}
    /** audio/bgm.ogg 또는 bgm.mp3를 반복 재생한다. 파일이 없으면 음악 없이 진행한다. */
    public void music(float volume){
        if(Boolean.getBoolean("c2s.noAudio")||bgm==null&&volume<=0||!hasMusic())return;
        try{
            if(bgm==null){bgm=Gdx.audio.newMusic(musicFile());bgm.setLooping(true);}
            bgm.setVolume(volume);
            if(volume<=0)bgm.pause();else if(!bgm.isPlaying())bgm.play();
        }catch(RuntimeException ignored){}
    }
    @Override public void dispose() { textures.values().forEach(Texture::dispose);sounds.values().forEach(Sound::dispose);if(bgm!=null)bgm.dispose(); }
}
