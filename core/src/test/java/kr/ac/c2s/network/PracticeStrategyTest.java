package kr.ac.c2s.network;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import kr.ac.c2s.common.Protocol.*;
class PracticeStrategyTest {
    @Test void difficultyChangesChoiceWithoutReadingOpponentChoice(){
        Snapshot s=new Snapshot();s.hand.addAll(List.of(1,2,3,4,5,6,7,8,9));
        for(int i=0;i<40;i++)assertTrue(PracticeStrategy.tile(s,PracticeDifficulty.EASY,new Random(i))<=3);
        assertEquals(6,PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(1)));
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
    private Snapshot fullHand(){
        Snapshot s=new Snapshot();s.hand.addAll(List.of(1,2,3,4,5,6,7,8,9));
        s.players[0]=new Player("AI","male_punk");s.players[1]=new Player("Other","male_veteran");
        return s;
    }
    private RoundObservation observation(int round,int known,int... candidates){
        RoundObservation o=new RoundObservation();o.round=round;o.knownRemainingTile=known;
        for(int n:candidates)o.possibleOpponentTiles.add(n);return o;
    }
    @Test void conservesStrongCardsEarlyButPrioritizesLateOrDangerousRounds(){
        Snapshot s=fullHand();
        assertEquals(6,PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(1)));
        // Eight and nine have the same immediate score against an unknown full hand.
        s.round=9;assertTrue(PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(1))>=8);
        s.round=1;s.me().life=1;assertTrue(PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(1))>=8);
        s.me().life=3;s.me().danger=9;assertTrue(PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(1))>=8);
    }
    @Test void uncertainEvidenceDoesNotPretendToKnowTheActualUsedTile(){
        Snapshot s=fullHand();s.opponentObservations.add(observation(1,0,7,8));
        double[] p=PracticeStrategy.opponentTileProbabilities(s);
        assertEquals(1.0/16,p[7],1e-9);assertEquals(1.0/16,p[8],1e-9);
        assertEquals(1.0/8,p[1],1e-9);assertEquals(1,Arrays.stream(p).sum(),1e-9);
        s.hand=List.of(1,8,9);s.round=9;
        s.opponentObservations=List.of(observation(1,0,1));
        // Once the rival's one is known to be used, nine no longer risks the 1-v-9 exception.
        assertEquals(9,PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(1)));
    }
    @Test void distinctUseAndPredictionEvidenceAreAppliedAtTheCorrectRound(){
        Snapshot s=fullHand();
        s.opponentObservations=List.of(observation(1,8,2,8));
        double[] p=PracticeStrategy.opponentTileProbabilities(s);
        assertEquals(0,p[2],1e-9);assertEquals(1.0/8,p[8],1e-9);
        s.opponentObservations=List.of(observation(1,8,2,8),observation(2,0,8));
        p=PracticeStrategy.opponentTileProbabilities(s);
        assertEquals(0,p[2],1e-9);assertEquals(0,p[8],1e-9);
        s.opponentObservations=List.of(observation(1,0,1,2),observation(2,0,1));
        p=PracticeStrategy.opponentTileProbabilities(s);
        assertEquals(0,p[1],1e-9);assertEquals(0,p[2],1e-9);
    }
    @Test void onlyAvailableCardsAreSelectedAndEasyNormalBehaviorIsUnchanged(){
        Snapshot s=fullHand();s.hand=List.of(2,4,7);
        for(int seed=0;seed<30;seed++){
            assertTrue(s.hand.contains(PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(seed))));
            assertEquals(s.hand.get(new Random(seed).nextInt(s.hand.size())),PracticeStrategy.tile(s,PracticeDifficulty.NORMAL,new Random(seed)));
        }
        s.hand=List.of();assertThrows(IllegalArgumentException.class,()->PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random()));
    }
    @Test void inconsistentOrOldEvidenceFallsBackWithoutReadingOtherSnapshotFields(){
        Snapshot s=fullHand();s.opponentObservations=List.of(observation(1,0,8),observation(2,0,8));
        double[] p=PracticeStrategy.opponentTileProbabilities(s);
        for(int n=1;n<=9;n++)assertEquals(1.0/9,p[n],1e-9);
        s.opponentObservations=null;int first=PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(123));
        s.opponentParity="홀수";s.opponent().selected=true;s.history.add("untrusted display text");
        assertEquals(first,PracticeStrategy.tile(s,PracticeDifficulty.HARD,new Random(123)));
    }
}
