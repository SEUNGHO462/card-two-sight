package kr.ac.c2s.common;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static kr.ac.c2s.common.Protocol.*;

class GameEngineTest {
    private GameEngine engine(int seed){return new GameEngine(new Player("나","male_handsome"),new Player("상대","female_leather"),new Random(seed * 1_000_003L + 7));}
    private void tiles(GameEngine e,int a,int b){e.apply(0,Command.number(Action.TILE,a));e.apply(1,Command.number(Action.TILE,b));}
    private void specials(GameEngine e){e.apply(0,Command.special(null,0));e.apply(1,Command.special(null,0));}
    private void next(GameEngine e){e.apply(0,Command.of(Action.NEXT));e.apply(1,Command.of(Action.NEXT));if(e.phase()==Phase.AMMO_LOAD){for(int side=0;side<2;side++){Snapshot s=e.snapshot(side);if(s.pendingAmmo>0){int empty=java.util.stream.IntStream.range(0,CHAMBERS).filter(i->!s.loadedChambers.contains(i)).findFirst().orElseThrow();e.apply(side,Command.number(Action.LOAD_AMMO,empty));}}}}
    @Test void numericRulesAndReversePriority(){
        assertEquals(0,GameEngine.compare(8,3,false));assertEquals(1,GameEngine.compare(8,3,true));
        assertEquals(-1,GameEngine.compare(4,4,true));assertEquals(0,GameEngine.compare(1,9,false));
        assertEquals(0,GameEngine.compare(1,9,true));assertEquals(1,GameEngine.compare(9,1,true));
    }
    @Test void fullNineRoundDrawIsPlayableWithoutReusingTiles(){
        GameEngine e=new GameEngine(new Player("나","male_handsome"),new Player("상대","female_leather"),new Random(0){@Override public int nextInt(int bound){return 0;}});
        for(int n=1;n<=9;n++){
            tiles(e,n,n);specials(e);assertEquals(n,e.snapshot(0).draws);assertEquals(9-n,e.snapshot(0).hand.size());
            next(e);e.apply(0,Command.number(Action.CHAMBER,0));e.apply(1,Command.number(Action.CHAMBER,6));
            assertArrayEquals(new boolean[]{false,false},e.snapshot(0).hit);next(e);
        }
        assertEquals(Phase.FINAL,e.phase());assertEquals(-1,e.snapshot(0).winner);assertEquals(3,e.snapshot(0).me().life);
    }
    @Test void privateInformationAndHintPurchaseAreIsolated(){
        GameEngine e=engine(3);tiles(e,8,2);specials(e);next(e);
        e.apply(0,Command.hint("INSPECT",0));Snapshot own=e.snapshot(0),other=e.snapshot(1);
        assertEquals(4,own.me().chips);assertNotNull(own.knownChambers.get(0));assertTrue(other.knownChambers.isEmpty());assertTrue(other.privateInfo.isEmpty());
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint("INSPECT",1)));assertEquals(4,e.snapshot(0).me().chips);
        String publicJson=JSON.toJson(own.opponent());assertFalse(publicJson.contains("hand"));assertFalse(publicJson.contains("cards"));
        String all=JSON.toJson(own);assertFalse(all.contains("bullets"));assertFalse(all.contains("predictions"));
    }
    @Test void duplicateAndOutOfPhaseActionsDoNotCorruptState(){
        GameEngine e=engine(7);assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.CHAMBER,1)));
        e.apply(0,Command.number(Action.TILE,2));assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.TILE,3)));
        e.apply(1,Command.number(Action.TILE,2));specials(e);next(e);e.apply(0,Command.number(Action.CHAMBER,1));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint("RANGE",0)));
        e.apply(1,Command.number(Action.CHAMBER,1));next(e);assertFalse(e.snapshot(0).hand.contains(2));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.TILE,2)));
    }
    @Test void disguiseChangesOnlyPublishedParity(){
        GameEngine e=null;for(int seed=0;seed<100;seed++){GameEngine candidate=engine(seed);if(candidate.snapshot(0).cards.contains(Card.DISGUISE)){e=candidate;break;}}
        assertNotNull(e);tiles(e,8,3);e.apply(0,Command.special(Card.DISGUISE,0));e.apply(1,Command.special(null,0));
        assertEquals(0,e.snapshot(0).roundWinner);assertEquals("홀수",e.snapshot(1).opponentParity);
        assertTrue(e.snapshot(1).privateInfo.stream().noneMatch(t->t.contains("위장")));
        assertEquals(1,e.snapshot(0).cards.size());
    }
    @Test void bothReverseCardsCancelAndPredictionRewardExcludesUsedTile(){
        GameEngine both=null,predict=null;
        for(int seed=0;seed<1000;seed++){GameEngine e=engine(seed);if(e.snapshot(0).cards.contains(Card.REVERSE)&&e.snapshot(1).cards.contains(Card.REVERSE))both=engine(seed);if(e.snapshot(0).cards.contains(Card.PREDICT))predict=engine(seed);if(both!=null&&predict!=null)break;}
        assertNotNull(both);tiles(both,8,3);both.apply(0,Command.special(Card.REVERSE,0));both.apply(1,Command.special(Card.REVERSE,0));assertEquals(0,both.snapshot(0).roundWinner);
        assertNotNull(predict);tiles(predict,6,7);predict.apply(0,Command.special(Card.PREDICT,7));predict.apply(1,Command.special(null,0));
        assertTrue(predict.snapshot(0).privateInfo.get(0).startsWith("예측 성공"));assertFalse(predict.snapshot(0).privateInfo.get(0).contains("중 7 확인"));assertTrue(predict.snapshot(1).privateInfo.stream().noneMatch(t->t.contains("예측 성공")));
    }
    @Test void everyPlayerGetsTwoDistinctCardsAndForfeitEndsOnlyOnce(){
        GameEngine e=engine(8);assertEquals(2,new HashSet<>(e.snapshot(0).cards).size());e.forfeit(1);e.forfeit(0);assertEquals(0,e.snapshot(0).winner);assertEquals(Phase.FINAL,e.phase());
    }
    @Test void threeLiveHitsEndGameBeforeNineRounds(){
        Random fixed=new Random(0){@Override public int nextInt(int bound){return 0;}};
        GameEngine e=new GameEngine(new Player("첫째","male_punk"),new Player("둘째","female_vest"),fixed);
        for(int n=1;n<=3;n++){
            tiles(e,n,n+1);specials(e);assertEquals(n+1,e.snapshot(0).me().danger);next(e);
            e.apply(0,Command.number(Action.CHAMBER,1));e.apply(1,Command.number(Action.CHAMBER,6));
            assertEquals(3-n,e.snapshot(0).me().life);assertEquals(3,e.snapshot(1).me().life);
            assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.CHAMBER,1)));
            next(e);
        }
        assertEquals(Phase.FINAL,e.phase());assertEquals(1,e.snapshot(0).winner);assertEquals(3,e.snapshot(0).round);
    }
    @Test void scoutRangeIsVisibleOnlyToItsOwner(){
        GameEngine e=null;for(int seed=0;seed<100;seed++){GameEngine candidate=engine(seed);if(candidate.snapshot(0).cards.contains(Card.SCOUT)){e=candidate;break;}}
        assertNotNull(e);tiles(e,4,8);e.apply(0,Command.special(Card.SCOUT,0));e.apply(1,Command.special(null,0));
        assertTrue(e.snapshot(0).privateInfo.stream().anyMatch(t->t.contains("5~9")));
        assertTrue(e.snapshot(1).privateInfo.stream().noneMatch(t->t.contains("5~9")||t.contains("정찰")));
    }
    @Test void riskDeltaReportsLossOnlyAndWinsKeepAccumulatedCount(){
        GameEngine e=engine(31);tiles(e,8,2);specials(e);
        assertEquals(0,e.snapshot(0).dangerDelta);assertEquals(1,e.snapshot(1).dangerDelta);
        next(e);e.apply(0,Command.number(Action.CHAMBER,0));e.apply(1,Command.number(Action.CHAMBER,0));next(e);
        tiles(e,3,7);specials(e);assertEquals(1,e.snapshot(0).dangerDelta);assertEquals(0,e.snapshot(1).dangerDelta);assertEquals(2,e.snapshot(1).me().danger);
    }

}
