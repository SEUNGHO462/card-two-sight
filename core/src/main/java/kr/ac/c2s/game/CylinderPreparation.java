package kr.ac.c2s.game;
import static kr.ac.c2s.common.Protocol.CHAMBERS;

/** Count-only loading view; the authority keeps actual ammunition positions private. */
public final class CylinderPreparation {
    public enum Phase { PLACING, LOADING, SPINNING, READY, ALIGNING, LOCKED }
    public static final float STEP=.85f, SPIN=1.65f, ALIGN=.95f;
    public static final float LOAD=1.75f;
    private Phase phase;
    private float time;
    private int selected=-1,loadTarget=-1;
    private final int before,added;
    public CylinderPreparation(){this(1,1);}
    public CylinderPreparation(int before,int added){
        this.before=Math.max(0,Math.min(CHAMBERS,before));
        this.added=Math.max(0,Math.min(CHAMBERS-this.before,added));
        phase=this.added>0?Phase.PLACING:Phase.SPINNING;
    }
    public Phase phase(){return phase;}
    public float time(){return time;}
    public int selected(){return selected;}
    public int before(){return before;}
    public int added(){return added;}
    public int loadTarget(){return loadTarget;}
    public boolean canPlace(){return phase==Phase.PLACING;}
    public void loadAt(int chamber){
        if(!canPlace()||chamber<0||chamber>=CHAMBERS)throw new IllegalArgumentException("장전할 빈 탄실을 선택하세요.");
        loadTarget=chamber;phase=Phase.LOADING;time=0;
    }
    public float loadDuration(){return .35f+added*STEP+.55f;}
    public boolean canChoose(){return phase==Phase.READY;}
    public void advance(float delta){
        if(phase==Phase.PLACING)return;
        time+=Math.max(0,delta);
        while(true){
            if(phase==Phase.LOADING&&time>=loadDuration()){time-=loadDuration();phase=Phase.SPINNING;}
            else if(phase==Phase.SPINNING&&time>=SPIN){time-=SPIN;phase=Phase.READY;}
            else if(phase==Phase.ALIGNING&&time>=ALIGN){time-=ALIGN;phase=Phase.LOCKED;}
            else break;
        }
    }
    public int loaded(){return phase==Phase.PLACING?before:phase==Phase.LOADING?before+Math.min(added,(int)Math.max(0,(time-.35f)/STEP)):before+added;}
    public float insertionProgress(){return Math.max(0,Math.min(1,(time-.35f)/STEP));}
    public float rotation(){
        if(phase==Phase.SPINNING)return CHAMBERS*3*(360f/CHAMBERS)*ease(time/SPIN);
        if(phase==Phase.ALIGNING)return (360+selected*(360f/CHAMBERS))*ease(time/ALIGN);
        if(phase==Phase.LOCKED)return selected*(360f/CHAMBERS);
        return 0;
    }
    private float ease(float t){t=Math.max(0,Math.min(1,t));return 1-(1-t)*(1-t)*(1-t);}
    public void align(int chamber){
        if(!canChoose()||chamber<0||chamber>=CHAMBERS)throw new IllegalArgumentException("탄실을 먼저 선택하세요.");
        selected=chamber;phase=Phase.ALIGNING;time=0;
    }
    public void chooseAgain(){if(phase!=Phase.LOCKED)throw new IllegalArgumentException("아직 탄창이 정렬 중입니다.");phase=Phase.READY;time=0;}
    public void previewAt(Phase phase,float time,int selected){this.phase=phase;this.time=time;this.selected=selected;}
}
