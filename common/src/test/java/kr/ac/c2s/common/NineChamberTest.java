package kr.ac.c2s.common;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static kr.ac.c2s.common.Protocol.*;
class NineChamberTest {
    private GameEngine game(){return new GameEngine(new Player("나","male_punk"),new Player("상대","female_vest"),new Random(0){@Override public int nextInt(int bound){return 0;}});}
    private void result(GameEngine e,int a,int b){e.apply(0,Command.number(Action.TILE,a));e.apply(1,Command.number(Action.TILE,b));e.apply(0,Command.special(null,0));e.apply(1,Command.special(null,0));}
    private void next(GameEngine e){e.apply(0,Command.of(Action.NEXT));e.apply(1,Command.of(Action.NEXT));if(e.phase()==Phase.AMMO_LOAD){for(int side=0;side<2;side++){Snapshot s=e.snapshot(side);if(s.pendingAmmo>0){int empty=java.util.stream.IntStream.range(0,CHAMBERS).filter(i->!s.loadedChambers.contains(i)).findFirst().orElseThrow();e.apply(side,Command.number(Action.LOAD_AMMO,empty));}}}}
    @Test void oneLiveInitiallyLossesAccumulateWinsKeepCountAndCapAtNine(){
        GameEngine e=game();assertEquals(1,e.snapshot(0).me().danger);
        for(int n=1;n<=9;n++){
            result(e,n,n==9?1:n+1);assertEquals(Math.min(9,n+1),e.snapshot(0).me().danger);assertEquals(1,e.snapshot(1).me().danger);
            assertEquals(n<9?1:0,e.snapshot(0).dangerDelta);assertEquals(0,e.snapshot(1).dangerDelta);
            next(e);e.apply(0,Command.number(Action.CHAMBER,8));e.apply(1,Command.number(Action.CHAMBER,8));next(e);
        }
    }
    @Test void hintReceiptsRemainAfterRoundChangeAndCannotLeakToOpponent(){
        GameEngine e=game();result(e,1,2);next(e);
        e.apply(0,Command.hint("RANGE",6));e.apply(0,Command.hint("INSPECT",6));
        Snapshot s=e.snapshot(0);assertEquals(2,s.hintHistory.size());assertTrue(s.hintHistory.get(0).contains("7~9"));assertTrue(s.hintHistory.get(1).contains("7번"));
        assertTrue(e.snapshot(1).hintHistory.isEmpty());assertEquals(0,s.me().chips);
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint("INSPECT",9)));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.CHAMBER,9)));
        e.apply(0,Command.number(Action.CHAMBER,8));e.apply(1,Command.number(Action.CHAMBER,8));next(e);
        assertEquals(2,e.snapshot(0).hintHistory.size());assertTrue(e.snapshot(0).boughtHints.isEmpty());assertEquals(0,e.snapshot(0).me().chips);
    }
}
