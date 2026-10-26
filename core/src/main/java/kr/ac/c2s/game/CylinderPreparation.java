package kr.ac.c2s.game;

/** Local presentation state. It never generates or changes ammunition positions. */
public final class CylinderPreparation {
    public enum Phase { LOADING, SPINNING, READY, ALIGNING, LOCKED }
    public static final float STEP=.30f, LOAD=3.1f, SPIN=1.45f, ALIGN=.95f;
    private Phase phase=Phase.LOADING;
    private float time;
    private int selected=-1;
    public Phase phase(){return phase;}
    public float time(){return time;}
    public int selected(){return selected;}
    public boolean canChoose(){return phase==Phase.READY;}
    public void advance(float delta){
        time+=Math.max(0,delta);
        while(true){
            if(phase==Phase.LOADING&&time>=LOAD){time-=LOAD;phase=Phase.SPINNING;}
            else if(phase==Phase.SPINNING&&time>=SPIN){time-=SPIN;phase=Phase.READY;}
            else if(phase==Phase.ALIGNING&&time>=ALIGN){time-=ALIGN;phase=Phase.LOCKED;}
            else break;
        }
    }
    public int loaded(){return phase==Phase.LOADING?Math.min(9,(int)Math.max(0,(time-.2f)/STEP)):9;}
    public float rotation(){
        if(phase==Phase.LOADING){float steps=Math.max(0,Math.min(9,(time-.2f)/STEP));int complete=(int)steps;float turn=Math.max(0,Math.min(1,((steps-complete)-.72f)/.28f));return -40*(complete+turn);}
        if(phase==Phase.SPINNING)return 1080*ease(time/SPIN);
        if(phase==Phase.ALIGNING)return (360+selected*40)*ease(time/ALIGN);
        if(phase==Phase.LOCKED)return selected*40;
        return 0;
    }
    private float ease(float t){t=Math.max(0,Math.min(1,t));return 1-(1-t)*(1-t)*(1-t);}
    public void align(int chamber){
        if(!canChoose()||chamber<0||chamber>8)throw new IllegalArgumentException("탄실을 먼저 선택하세요.");
        selected=chamber;phase=Phase.ALIGNING;time=0;
    }
    public void chooseAgain(){
        if(phase!=Phase.LOCKED)throw new IllegalArgumentException("아직 탄창이 정렬 중입니다.");
        phase=Phase.READY;time=0;
    }
    public void previewAt(Phase phase,float time,int selected){this.phase=phase;this.time=time;this.selected=selected;}
}
