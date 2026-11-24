package kr.ac.c2s.common;
import org.junit.jupiter.api.Test;
import java.util.*;
import static kr.ac.c2s.common.Protocol.*;
import static org.junit.jupiter.api.Assertions.*;
class DisguiseChoiceTest {
    private GameEngine play(int tile, boolean odd){
        GameEngine e=new GameEngine(new Player("A","male_punk"),new Player("B","male_veteran"),new Random(4),true);
        e.apply(0,Command.loadout(List.of(Card.DISGUISE,Card.SCOUT)));
        e.apply(1,Command.loadout(List.of(Card.REVERSE,Card.PREDICT)));
        e.apply(0,Command.number(Action.TILE,tile));e.apply(1,Command.number(Action.TILE,2));
        Command c=JSON.fromJson(JSON.toJson(Command.disguise(odd)),Command.class);
        e.apply(0,c);e.apply(1,Command.special(null,0));return e;
    }
    @Test void bothParitiesMayTellTheTruthOrLieWithoutChangingWinner(){
        for(int tile:new int[]{7,8})for(boolean odd:new boolean[]{false,true}){
            GameEngine e=play(tile,odd);
            assertEquals(odd?"홀수":"짝수",e.snapshot(1).opponentParity);
            assertEquals(tile,e.snapshot(0).ownTile);assertEquals(0,e.snapshot(0).roundWinner);
            assertFalse(e.snapshot(0).cards.contains(Card.DISGUISE));
            assertEquals(List.of(3,4,5,6,7,8,9),e.snapshot(1).opponentObservations.get(0).possibleOpponentTiles);
            assertEquals(1,e.snapshot(1).opponent().specialCount);
        }
    }
}
