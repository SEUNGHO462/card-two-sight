package kr.ac.c2s.common;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static kr.ac.c2s.common.Protocol.*;
class LoadoutTest {
    private GameEngine game(){return new GameEngine(new Player("나","male_punk"),new Player("상대","female_vest"),new Random(7),true);}
    @Test void validatesTwoDistinctCardsAndKeepsOpponentChoicePrivate(){
        GameEngine e=game();assertEquals(Phase.LOADOUT_SELECT,e.phase());assertTrue(e.snapshot(0).cards.isEmpty());
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.loadout(null)));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.loadout(List.of(Card.SCOUT))));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.loadout(List.of(Card.SCOUT,Card.SCOUT))));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.loadout(Arrays.asList(Card.SCOUT,null))));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.loadout(List.of(Card.SCOUT,Card.REVERSE,Card.PREDICT))));
        assertTrue(e.snapshot(0).cards.isEmpty());assertFalse(e.snapshot(0).me().loadoutReady);
        e.apply(0,Command.loadout(List.of(Card.REVERSE,Card.SCOUT)));
        assertEquals(Phase.LOADOUT_SELECT,e.phase());assertTrue(e.snapshot(1).cards.isEmpty());assertTrue(e.snapshot(1).opponent().loadoutReady);
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.loadout(List.of(Card.PREDICT,Card.DISGUISE))));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.TILE,7)));
        e.apply(1,Command.loadout(List.of(Card.PREDICT,Card.DISGUISE)));
        assertEquals(Phase.TILE_SELECT,e.phase());assertEquals(List.of(Card.REVERSE,Card.SCOUT),e.snapshot(0).cards);
        assertEquals(List.of(Card.PREDICT,Card.DISGUISE),e.snapshot(1).cards);
    }
    @Test void chosenCardIsUsableAndConsumedOnce(){
        GameEngine e=game();e.apply(0,Command.loadout(List.of(Card.REVERSE,Card.SCOUT)));e.apply(1,Command.loadout(List.of(Card.PREDICT,Card.DISGUISE)));
        e.apply(0,Command.number(Action.TILE,3));e.apply(1,Command.number(Action.TILE,8));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.special(Card.PREDICT,8)));
        e.apply(0,Command.special(Card.REVERSE,0));e.apply(1,Command.special(null,0));
        assertEquals(0,e.snapshot(0).roundWinner);assertEquals(List.of(Card.SCOUT),e.snapshot(0).cards);
    }
}
