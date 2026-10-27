package kr.ac.c2s.common;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static kr.ac.c2s.common.Protocol.*;
class ManualAmmoTest {
    private GameEngine game(){return new GameEngine(new Player("나","male_handsome"),new Player("연습 상대","female_vest"),new Random(0){@Override public int nextInt(int b){return 0;}});}
    private void round(GameEngine e,int a,int b){e.apply(0,Command.number(Action.TILE,a));e.apply(1,Command.number(Action.TILE,b));e.apply(0,Command.special(null,0));e.apply(1,Command.special(null,0));}
    private void next(GameEngine e){e.apply(0,Command.of(Action.NEXT));e.apply(1,Command.of(Action.NEXT));}
    @Test void chosenEmptySocketControlsRealAmmoAndInvalidLoadsAreRejected(){
        GameEngine e=game();round(e,1,2);next(e);assertEquals(Phase.AMMO_LOAD,e.phase());
        assertEquals(List.of(1),e.snapshot(0).loadedChambers);assertEquals(1,e.snapshot(0).pendingAmmo);assertEquals(0,e.snapshot(1).pendingAmmo);
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.LOAD_AMMO,1)));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.LOAD_AMMO,9)));
        assertThrows(IllegalArgumentException.class,()->e.apply(1,Command.number(Action.LOAD_AMMO,7)));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.CHAMBER,8)));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint("INSPECT",7)));
        e.apply(0,Command.number(Action.LOAD_AMMO,7));assertEquals(Phase.ROULETTE_SELECT,e.phase());
        assertTrue(e.snapshot(0).loadedChambers.isEmpty());assertTrue(e.snapshot(1).loadedChambers.isEmpty());assertEquals(-1,e.snapshot(1).ammoChoice);
        e.apply(0,Command.hint("INSPECT",7));assertEquals(true,e.snapshot(0).knownChambers.get(7));
        e.apply(0,Command.hint("RANGE",1));assertTrue(e.snapshot(0).privateInfo.stream().anyMatch(t->t.contains("실탄 1발")));
        e.apply(0,Command.number(Action.CHAMBER,8));e.apply(1,Command.number(Action.CHAMBER,8));next(e);
        round(e,9,1);next(e);assertEquals(Set.of(1,7),new HashSet<>(e.snapshot(0).loadedChambers));assertEquals(List.of(1),e.snapshot(1).loadedChambers);
        e.apply(0,Command.number(Action.LOAD_AMMO,3));assertEquals(3,e.snapshot(0).me().danger);
    }
    @Test void leadingInNumericWinsStillLosesWhenOwnLivesReachZero(){
        GameEngine e=game();
        for(int n=1;n<=3;n++){
            round(e,n+1,n);next(e);
            e.apply(1,Command.number(Action.LOAD_AMMO,n+2));
            e.apply(0,Command.number(Action.CHAMBER,1));e.apply(1,Command.number(Action.CHAMBER,8));next(e);
        }
        Snapshot a=e.snapshot(0),b=e.snapshot(1);assertEquals(Phase.FINAL,a.phase);assertEquals(1,a.winner);assertEquals(0,a.me().life);assertEquals(3,a.me().wins);assertEquals(0,a.opponent().wins);
        assertNotEquals(a.you,a.winner);assertEquals(b.you,b.winner);assertEquals("목숨 소진",a.reason);
    }
}
