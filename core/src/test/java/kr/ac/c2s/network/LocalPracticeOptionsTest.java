package kr.ac.c2s.network;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import kr.ac.c2s.common.Protocol.*;
class LocalPracticeOptionsTest {
    @Test void changingOpponentResetsReadinessAndPersistsIntoMatch() throws Exception {
        var events=new LinkedBlockingQueue<Event>();
        try(var gateway=new LocalGameGateway("테스트","male_handsome",Runnable::run)){
            gateway.setListener(events::add);gateway.send(Command.of(Action.CREATE));await(events);
            gateway.send(Command.of(Action.READY));assertTrue(await(events).me().ready);
            gateway.configure(PracticeDifficulty.HARD,"female_silver");
            Snapshot s=await(events);assertFalse(s.me().ready);assertEquals("female_silver",s.opponent().characterId);assertTrue(s.message.contains("HARD"));
            gateway.send(Command.of(Action.READY));await(events);gateway.send(Command.of(Action.START));
            s=await(events);assertEquals(Phase.LOADOUT_SELECT,s.phase);assertEquals("female_silver",s.opponent().characterId);
        }
    }
    private Snapshot await(BlockingQueue<Event> events)throws Exception{Event e=events.poll(3,TimeUnit.SECONDS);assertNotNull(e);assertEquals("STATE",e.type);return e.state;}
}
