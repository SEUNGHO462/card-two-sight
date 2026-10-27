package kr.ac.c2s.server;
import org.junit.jupiter.api.Test;
import kr.ac.c2s.common.Protocol;
import kr.ac.c2s.common.Protocol.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class ServerIntegrationTest {
    static final class Client implements AutoCloseable {
        final Socket socket;final Reader in;final Writer out;
        Client(int port)throws Exception{socket=new Socket("127.0.0.1",port);socket.setSoTimeout(6000);in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));out=new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8);}
        void send(Command c)throws IOException{out.write(Protocol.JSON.toJson(c)+"\n");out.flush();}
        Event await(Predicate<Event> test)throws IOException{for(int i=0;i<100;i++){String line=Protocol.readLine(in);if(line==null)throw new EOFException();Event e=Protocol.JSON.fromJson(line,Event.class);if(test.test(e))return e;}throw new AssertionError("No matching event");}
        Snapshot phase(Phase p)throws IOException{return await(e->"STATE".equals(e.type)&&e.state.phase==p).state;}
        @Override public void close()throws IOException{socket.close();}
    }
    @Test void realSocketsCompleteMatchAndRematch()throws Exception{
        try(GameServer server=new GameServer(0)){server.start();try(Client a=new Client(server.port());Client b=new Client(server.port());Client intruder=new Client(server.port())){
            a.send(Command.enter(Action.CREATE,"승호","male_handsome","dragon"));assertEquals(0,a.phase(Phase.LOBBY).you);
            b.send(Command.enter(Action.JOIN,"지민","female_leather","DRAGON"));assertEquals(1,b.phase(Phase.LOBBY).you);
            intruder.send(Command.enter(Action.JOIN,"세번째","male_punk","DRAGON"));assertEquals("ERROR",intruder.await(e->"ERROR".equals(e.type)).type);
            a.send(Command.of(Action.READY));b.send(Command.of(Action.READY));
            a.await(e->e.state!=null&&e.state.players[1]!=null&&e.state.players[0].ready&&e.state.players[1].ready);
            a.send(Command.of(Action.START));a.phase(Phase.LOADOUT_SELECT);b.phase(Phase.LOADOUT_SELECT);
            a.send(Command.loadout(java.util.List.of(Card.REVERSE,Card.SCOUT)));
            Snapshot privateChoice=a.await(e->e.state!=null&&e.state.me().loadoutReady).state;
            assertEquals(java.util.List.of(Card.REVERSE,Card.SCOUT),privateChoice.cards);
            Snapshot other=b.await(e->e.state!=null&&e.state.opponent().loadoutReady).state;
            assertTrue(other.cards.isEmpty());assertEquals(2,other.opponent().specialCount);
            b.send(Command.loadout(java.util.List.of(Card.DISGUISE,Card.PREDICT)));
            a.phase(Phase.TILE_SELECT);b.phase(Phase.TILE_SELECT);
            Snapshot finalA=null,finalB=null;
            for(int n=1;n<=9;n++){
                a.send(Command.number(Action.TILE,n));b.send(Command.number(Action.TILE,n));a.phase(Phase.SPECIAL_SELECT);b.phase(Phase.SPECIAL_SELECT);
                a.send(Command.special(null,0));b.send(Command.special(null,0));a.phase(Phase.ROUND_RESULT);b.phase(Phase.ROUND_RESULT);
                a.send(Command.of(Action.NEXT));b.send(Command.of(Action.NEXT));a.phase(Phase.ROULETTE_SELECT);b.phase(Phase.ROULETTE_SELECT);
                a.send(Command.number(Action.CHAMBER,0));b.send(Command.number(Action.CHAMBER,1));a.phase(Phase.ROULETTE_RESULT);b.phase(Phase.ROULETTE_RESULT);
                a.send(Command.of(Action.NEXT));b.send(Command.of(Action.NEXT));
                Snapshot afterA=a.await(e->e.state!=null&&(e.state.phase==Phase.TILE_SELECT||e.state.phase==Phase.FINAL)).state;
                Snapshot afterB=b.await(e->e.state!=null&&(e.state.phase==Phase.TILE_SELECT||e.state.phase==Phase.FINAL)).state;
                if(afterA.phase==Phase.FINAL){finalA=afterA;finalB=afterB;break;}
            }
            Snapshot sa=finalA,sb=finalB;assertNotNull(sa);assertNotNull(sb);assertEquals(sa.matchId,sb.matchId);assertEquals(sa.round,sa.draws);assertTrue(sa.round<=9);if(sa.reason.equals("9라운드 종료"))assertEquals(-1,sa.winner);
            a.send(Command.of(Action.REMATCH));b.send(Command.of(Action.REMATCH));Snapshot restarted=a.phase(Phase.LOADOUT_SELECT);b.phase(Phase.LOADOUT_SELECT);assertTrue(restarted.cards.isEmpty());assertNotEquals(sa.matchId,restarted.matchId);assertEquals(9,restarted.hand.size());
            b.close();Snapshot forfeited=a.phase(Phase.FINAL);assertEquals(0,forfeited.winner);assertEquals("상대 연결 종료",forfeited.reason);
        }}
    }
    @Test void wrongRoomPasswordAndInvalidMessagesAreRejected()throws Exception{
        try(GameServer server=new GameServer(0)){server.start();try(Client c=new Client(server.port())){
            c.send(Command.enter(Action.JOIN,"테스트","male_punk","MISSING"));assertEquals("ERROR",c.await(e->"ERROR".equals(e.type)).type);
            c.send(Command.of(Action.START));assertEquals("ERROR",c.await(e->"ERROR".equals(e.type)).type);
        }}
    }
}
