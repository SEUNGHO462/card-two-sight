package kr.ac.c2s.network;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import kr.ac.c2s.common.Protocol.*;
class PracticeStrategyTest {
    @Test void difficultyChangesChoiceWithoutReadingOpponentChoice(){
        Snapshot s=new Snapshot();s.hand.addAll(List.of(1,2,3,4,5,6,7,8,9));
        for(int i=0;i<40;i++)assertTrue(PracticeStrategy.tile(s,PracticeDifficulty.EASY,new Random(i))<=3);
        assertEquals(8,PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(1)));
        s.hand=List.of(1);assertEquals(1,PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random()));
        s.cards.add(Card.REVERSE);s.ownTile=3;
        assertNull(PracticeStrategy.special(s,PracticeDifficulty.EASY,new Random()));
        assertEquals(Card.REVERSE,PracticeStrategy.special(s,PracticeDifficulty.HARD,new Random()));
    }
    @Test void paidChamberKnowledgeAvoidsKnownLiveAndPrefersKnownBlank(){
        Snapshot s=new Snapshot();s.knownChambers.put(2,true);s.knownChambers.put(6,false);
        assertEquals(6,PracticeStrategy.chamber(s,new Random()));
        s.knownChambers.remove(6);for(int i=0;i<50;i++)assertNotEquals(2,PracticeStrategy.chamber(s,new Random(i)));
    }
}
