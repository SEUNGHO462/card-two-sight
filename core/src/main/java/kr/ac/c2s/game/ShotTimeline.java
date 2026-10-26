package kr.ac.c2s.game;

/** Presentation only: the server has already decided the outcome. */
public final class ShotTimeline {
    public static final float FIRE=.85f, LIFE=1.7f, END=3.15f;
    private float time;
    public float time(){return time;}
    public boolean advance(float delta){float before=time;time+=Math.max(0,delta);return before<FIRE&&time>=FIRE;}
    public boolean finished(){return time>=END;}
    public int visibleLife(int finalLife,boolean hit){return finalLife+(hit&&time<LIFE?1:0);}
    public int frame(boolean hit){
        if(hit){if(time<FIRE+.06f)return 0;if(time<1.07f)return 3;if(time<1.4f)return 4;return 5;}
        if(time<1.32f)return 0;if(time<1.78f)return 1;return 2;
    }
    public float flash(){return time>=FIRE&&time<FIRE+.09f?1-(time-FIRE)/.09f:0;}
    public float shake(){return time>=FIRE&&time<FIRE+.42f?(1-(time-FIRE)/.42f)*8:0;}
    /** Used by the explicitly requested --shot-preview render check only. */
    public void previewAt(float value){time=Math.max(0,value);}
}
