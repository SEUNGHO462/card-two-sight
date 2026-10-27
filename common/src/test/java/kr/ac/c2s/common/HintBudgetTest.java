package kr.ac.c2s.common;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static kr.ac.c2s.common.Protocol.*;
import static org.junit.jupiter.api.Assertions.*;
class HintBudgetTest {
    private GameEngine game(){return new GameEngine(new Player("나","male_handsome"),new Player("상대","female_vest"),new Random(0){@Override public int nextInt(int b){return 0;}});}
    private void roulette(GameEngine e,int tile){e.apply(0,Command.number(Action.TILE,tile));e.apply(1,Command.number(Action.TILE,tile));e.apply(0,Command.special(null,0));e.apply(1,Command.special(null,0));e.apply(0,Command.of(Action.NEXT));e.apply(1,Command.of(Action.NEXT));}
    @Test void onlyTwoHintsAreAcceptedAndWholeMatchBudgetDoesNotRefill(){
        GameEngine e=game();roulette(e,1);
        for(String retired:new String[]{"COMPARE","ADJACENT",null})assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint(retired,0)));
        assertEquals(12,e.snapshot(0).me().chips);assertTrue(e.snapshot(0).hintHistory.isEmpty());
        e.apply(0,Command.hint("RANGE",0));assertEquals(8,e.snapshot(0).me().chips);
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint("RANGE",3)));assertEquals(8,e.snapshot(0).me().chips);
        e.apply(0,Command.hint("INSPECT",0));assertEquals(0,e.snapshot(0).me().chips);assertEquals(2,e.snapshot(0).hintHistory.size());assertTrue(e.snapshot(1).hintHistory.isEmpty());
        e.apply(0,Command.number(Action.CHAMBER,8));e.apply(1,Command.number(Action.CHAMBER,8));e.apply(0,Command.of(Action.NEXT));e.apply(1,Command.of(Action.NEXT));
        roulette(e,2);assertEquals(0,e.snapshot(0).me().chips);assertTrue(e.snapshot(0).boughtHints.isEmpty());
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint("RANGE",0)));assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint("INSPECT",0)));
        assertEquals(0,e.snapshot(0).me().chips);assertEquals(2,e.snapshot(0).hintHistory.size());
    }
    @Test void inspectionFirstLeavesExactlyEnoughForOneRange(){
        GameEngine e=game();roulette(e,1);e.apply(0,Command.hint("INSPECT",0));assertEquals(4,e.snapshot(0).me().chips);
        e.apply(0,Command.hint("RANGE",0));assertEquals(0,e.snapshot(0).me().chips);
    }
}
