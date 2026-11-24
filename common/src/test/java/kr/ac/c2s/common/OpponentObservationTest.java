package kr.ac.c2s.common;

import org.junit.jupiter.api.Test;
import java.util.*;
import static kr.ac.c2s.common.Protocol.*;
import static org.junit.jupiter.api.Assertions.*;

class OpponentObservationTest {
    private GameEngine round(Card special,int prediction){
        GameEngine e=new GameEngine(new Player("A","male_punk"),new Player("B","male_veteran"),new Random(7),true);
        e.apply(0,Command.loadout(List.of(Card.SCOUT,Card.PREDICT)));
        e.apply(1,Command.loadout(List.of(Card.REVERSE,Card.DISGUISE)));
        e.apply(0,Command.number(Action.TILE,2));
        assertTrue(e.snapshot(1).opponentObservations.isEmpty());
        e.apply(1,Command.number(Action.TILE,8));
        e.apply(0,Command.special(special,prediction));e.apply(1,Command.special(null,0));return e;
    }
    @Test void ordinaryEvidenceDoesNotDiscloseTheHiddenOpponentNumber(){
        GameEngine e=round(null,0);
        assertEquals(List.of(4,6,8),e.snapshot(0).opponentObservations.get(0).possibleOpponentTiles);
        assertEquals(List.of(2,4,6),e.snapshot(1).opponentObservations.get(0).possibleOpponentTiles);
    }
    @Test void scoutAndSuccessfulPredictionEvidenceStayPrivateToTheirOwner(){
        GameEngine scout=round(Card.SCOUT,0);
        assertEquals(List.of(6,8),scout.snapshot(0).opponentObservations.get(0).possibleOpponentTiles);
        assertEquals(List.of(2,4,6),scout.snapshot(1).opponentObservations.get(0).possibleOpponentTiles);
        GameEngine prediction=round(Card.PREDICT,8);
        RoundObservation own=prediction.snapshot(0).opponentObservations.get(0);
        assertEquals(List.of(8),own.possibleOpponentTiles);
        assertTrue(own.knownRemainingTile>=1&&own.knownRemainingTile<=9);
        assertNotEquals(8,own.knownRemainingTile);
        assertEquals(0,prediction.snapshot(1).opponentObservations.get(0).knownRemainingTile);
        assertTrue(prediction.snapshot(0).privateInfo.stream().anyMatch(t->t.contains(own.knownRemainingTile+" 확인")));
    }
    @Test void modifyingASnapshotCannotChangeTheStoredEvidence(){
        GameEngine e=round(null,0);Snapshot s=e.snapshot(0);
        s.opponentObservations.get(0).possibleOpponentTiles.clear();
        s.opponentObservations.get(0).knownRemainingTile=9;s.opponentObservations.clear();
        assertEquals(List.of(4,6,8),e.snapshot(0).opponentObservations.get(0).possibleOpponentTiles);
        assertEquals(0,e.snapshot(0).opponentObservations.get(0).knownRemainingTile);
    }
    @Test void observationsUseTheSameParityAndReverseRulesAsTheVisibleResult(){
        for(Card rivalCard:new Card[]{Card.REVERSE,Card.DISGUISE}){
            GameEngine e=new GameEngine(new Player("A","male_punk"),new Player("B","male_veteran"),new Random(1),true);
            e.apply(0,Command.loadout(List.of(Card.SCOUT,Card.PREDICT)));
            e.apply(1,Command.loadout(List.of(Card.REVERSE,Card.DISGUISE)));
            e.apply(0,Command.number(Action.TILE,2));e.apply(1,Command.number(Action.TILE,8));
            e.apply(0,Command.special(null,0));e.apply(1,Command.special(rivalCard,0));
            assertEquals(rivalCard==Card.DISGUISE?List.of(3,4,5,6,7,8,9):List.of(4,6,8),e.snapshot(0).opponentObservations.get(0).possibleOpponentTiles);
        }
    }
}
