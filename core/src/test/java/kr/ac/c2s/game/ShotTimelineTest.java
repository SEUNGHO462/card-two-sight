package kr.ac.c2s.game;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShotTimelineTest {
    @Test void lifeChangeWaitsUntilImpactAndTriggerOnlyFiresOnce(){
        ShotTimeline t=new ShotTimeline();assertEquals(3,t.visibleLife(2,true));assertEquals(3,t.visibleLife(3,false));
        assertFalse(t.advance(.8f));assertTrue(t.advance(.1f));assertFalse(t.advance(.1f));
        assertEquals(3,t.visibleLife(2,true));assertFalse(t.finished());
        t.advance(1);assertEquals(2,t.visibleLife(2,true));assertEquals(3,t.visibleLife(3,false));
        t.advance(2);assertTrue(t.finished());assertFalse(t.advance(1));
    }
    @Test void emptyAndLiveUseDifferentPoseSequencesAndFinishAtDifferentFrames(){
        ShotTimeline t=new ShotTimeline();t.advance(.95f);assertEquals(0,t.frame(false));assertEquals(3,t.frame(true));
        t.advance(.5f);assertEquals(1,t.frame(false));assertEquals(5,t.frame(true));
        t.advance(2);assertEquals(2,t.frame(false));assertEquals(5,t.frame(true));assertEquals(0,t.visibleLife(0,true));
    }
}
