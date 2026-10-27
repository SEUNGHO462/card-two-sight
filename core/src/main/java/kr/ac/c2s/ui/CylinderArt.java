package kr.ac.c2s.ui;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.utils.Disposable;

/** Code-drawn functional parts: deep bore, selection ring and distinct cartridges. */
public final class CylinderArt implements Disposable {
    public final Texture bore,ring,live,blank,plate,cap;
    public CylinderArt(){
        Pixmap p=new Pixmap(160,160,Pixmap.Format.RGBA8888);
        for(int y=0;y<160;y++)for(int x=0;x<160;x++){
            float dx=(x-79.5f)/78,dy=(y-79.5f)/78,r=(float)Math.sqrt(dx*dx+dy*dy);
            if(r>1)continue;
            float value=r>.90f?.22f:r>.80f?.08f:.015f+.035f*r;
            if(r>.82f&&dy<-.3f)value+=.15f;
            p.drawPixel(x,y,Color.rgba8888(value*.86f,value*.96f,value,Math.min(1,(1-r)*35)));
        }
        bore=texture(p);
        p=new Pixmap(180,180,Pixmap.Format.RGBA8888);
        for(int y=0;y<180;y++)for(int x=0;x<180;x++){
            float r=(float)Math.hypot(x-89.5,y-89.5)/88;
            if(r<1&&r>.90f)p.drawPixel(x,y,Color.rgba8888(1,.73f,.30f,Math.min(1,Math.min((1-r)*60,(r-.90f)*60))));
        }
        ring=texture(p);live=cartridge(true);blank=cartridge(false);plate=disc(false);cap=disc(true);
    }
    private Texture disc(boolean cartridgeBack){
        int size=cartridgeBack?160:640;Pixmap p=new Pixmap(size,size,Pixmap.Format.RGBA8888);
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            float dx=(x-size/2f)/(size/2f),dy=(y-size/2f)/(size/2f),r=(float)Math.hypot(dx,dy);if(r>1)continue;
            float noise=((x*37+y*71)%29)/600f,light=.08f*(1-dy)+noise;
            boolean gold=cartridgeBack||r>.92f||r<.15f;
            float v=gold?.45f+light:.20f+light;
            if(r>.97f||cartridgeBack&&r>.86f||r<.055f)v*=.45f;
            p.drawPixel(x,y,Color.rgba8888(gold?v:v*.85f,gold?v*.74f:v*.92f,gold?v*.32f:v,1));
        }
        return texture(p);
    }
    private Texture cartridge(boolean liveRound){
        Pixmap p=new Pixmap(72,176,Pixmap.Format.RGBA8888);
        // Cylindrical brass body with a visible side highlight and rim.
        for(int x=14;x<58;x++){
            float shine=(float)Math.sin((x-14)/44f*Math.PI);
            p.setColor(.30f+shine*.48f,.20f+shine*.40f,.08f+shine*.15f,1);p.fillRectangle(x,52,1,107);
        }
        p.setColor(.80f,.58f,.24f,1);p.fillRectangle(10,151,52,10);
        p.setColor(.32f,.21f,.10f,1);p.fillRectangle(10,161,52,5);
        if(liveRound){
            p.setColor(.71f,.34f,.16f,1);p.fillCircle(36,37,21);p.fillRectangle(15,36,42,26);
            p.setColor(.96f,.63f,.36f,1);p.fillRectangle(22,28,5,30);
        }else{
            p.setColor(.58f,.64f,.67f,1);p.fillRectangle(15,39,42,20);
            p.setColor(.14f,.17f,.19f,1);
            for(int i=0;i<5;i++)p.drawLine(18+i*8,40,22+i*8,51);
        }
        return texture(p);
    }
    private Texture texture(Pixmap p){Texture t=new Texture(p);p.dispose();t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);return t;}
    @Override public void dispose(){bore.dispose();ring.dispose();live.dispose();blank.dispose();plate.dispose();cap.dispose();}
}
