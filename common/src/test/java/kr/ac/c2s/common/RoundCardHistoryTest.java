package kr.ac.c2s.common;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static kr.ac.c2s.common.Protocol.*;

class RoundCardHistoryTest {
    private GameEngine game(Card opponent){
        GameEngine e=new GameEngine(new Player("나","male_punk"),new Player("상대","female_vest"),new Random(7),true);
        e.apply(0,Command.loadout(List.of(Card.SCOUT,Card.REVERSE)));
        Card reserve=opponent==Card.DISGUISE?Card.REVERSE:Card.DISGUISE;
        e.apply(1,Command.loadout(List.of(opponent,reserve)));
        e.apply(0,Command.number(Action.TILE,3));e.apply(1,Command.number(Action.TILE,8));
        return e;
    }
    @Test void revealsEachUsedCardOnlyAfterBothPlayersConfirmAndFromEachView(){
        for(Card card:Card.values()){
            GameEngine e=game(card);
            e.apply(1,Command.special(card,3));
            assertEquals(Phase.SPECIAL_SELECT,e.phase());
            for(int viewer=0;viewer<2;viewer++){
                assertTrue(e.snapshot(viewer).history.isEmpty());
                assertTrue(e.snapshot(viewer).privateInfo.isEmpty());
            }
            e.apply(0,Command.special(Card.SCOUT,0));
            assertEquals(Phase.ROUND_RESULT,e.phase());
            Snapshot first=e.snapshot(0),second=e.snapshot(1);
            String parity=card==Card.DISGUISE?"홀수":"짝수";
            assertEquals("R1 · 나 3 · 상대 "+parity+" · "+(card==Card.REVERSE?"승리":"패배")
                +"\n나 특수: 정찰\n상대 특수: "+card.label,first.history.get(0));
            assertTrue(second.history.get(0).startsWith("R1 · 나 8 · 상대 홀수 · "));
            assertTrue(second.history.get(0).endsWith("\n나 특수: "+card.label+"\n상대 특수: 정찰"));
            assertTrue(first.privateInfo.contains("상대 사용 특수카드: "+card.label));
            assertFalse(second.privateInfo.stream().anyMatch(s->s.contains("5~9")));
        }
    }
    @Test void recordsSkippedCardsExplicitly(){
        GameEngine e=game(Card.REVERSE);
        e.apply(0,Command.special(null,0));e.apply(1,Command.special(null,0));
        for(int viewer=0;viewer<2;viewer++){
            assertTrue(e.snapshot(viewer).history.get(0).endsWith("\n나 특수: 미사용\n상대 특수: 미사용"));
            assertTrue(e.snapshot(viewer).privateInfo.isEmpty());
        }
    }
}
