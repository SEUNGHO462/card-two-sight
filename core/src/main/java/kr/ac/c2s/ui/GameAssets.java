package kr.ac.c2s.ui;
import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import java.util.*;

public final class GameAssets implements Disposable {
    private final Map<String,Texture> textures=new HashMap<>();
    private final Map<String,Sound> sounds=new HashMap<>();
    public Texture texture(String path) {
        return textures.computeIfAbsent(path,p->{ Texture t=new Texture(Gdx.files.internal(p));t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);return t; });
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
    @Override public void dispose() { textures.values().forEach(Texture::dispose);sounds.values().forEach(Sound::dispose); }
}
