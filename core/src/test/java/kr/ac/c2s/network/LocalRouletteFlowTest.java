package kr.ac.c2s.network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import kr.ac.c2s.common.Protocol.*;
class LocalRouletteFlowTest {
    @Test @Timeout(15) void realGatewayDeliversPurchasedHintAndDirectShotThenRetainsReceipt()throws Exception{
        var events=new LinkedBlockingQueue<Event>();
        try(var gateway=new LocalGameGateway("테스트","male_handsome","female_silver",PracticeDifficulty.EASY,Runnable::run)){
            gateway.setListener(events::add);gateway.send(Command.of(Action.CREATE));state(events,s->s.phase==Phase.LOBBY);
            gateway.send(Command.of(Action.READY));state(events,s->s.me().ready);
            gateway.send(Command.of(Action.START));state(events,s->s.phase==Phase.LOADOUT_SELECT);
            gateway.send(Command.loadout(List.of(Card.SCOUT,Card.PREDICT)));state(events,s->s.phase==Phase.TILE_SELECT);
            gateway.send(Command.number(Action.TILE,8));state(events,s->s.phase==Phase.SPECIAL_SELECT);
            gateway.send(Command.special(null,0));state(events,s->s.phase==Phase.ROUND_RESULT);
            gateway.send(Command.of(Action.NEXT));state(events,s->s.phase==Phase.ROULETTE_SELECT);
            gateway.send(Command.hint("INSPECT",0));Snapshot receipt=state(events,s->s.hintHistory.size()==1);
            assertEquals(4,receipt.me().chips);assertTrue(receipt.knownChambers.containsKey(0));assertTrue(receipt.hintHistory.get(0).contains("R1"));
            gateway.send(Command.number(Action.CHAMBER,0));state(events,s->s.phase==Phase.ROULETTE_RESULT);
            gateway.send(Command.of(Action.NEXT));Snapshot next=state(events,s->s.phase==Phase.TILE_SELECT&&s.round==2);
            assertEquals(receipt.hintHistory,next.hintHistory);assertEquals(4,next.me().chips);assertTrue(next.knownChambers.isEmpty());
        }
    }
    private Snapshot state(BlockingQueue<Event> events,Predicate<Snapshot> condition)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
        while(System.nanoTime()<deadline){Event e=events.poll(1,TimeUnit.SECONDS);if(e==null)continue;assertNotEquals("ERROR",e.type,e.message);if(e.state!=null&&condition.test(e.state))return e.state;}
        throw new AssertionError("Expected practice state not received");
    }
}
