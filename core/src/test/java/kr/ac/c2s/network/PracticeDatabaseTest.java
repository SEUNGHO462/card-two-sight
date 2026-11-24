package kr.ac.c2s.network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static kr.ac.c2s.common.Protocol.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;
import kr.ac.c2s.server.JdbcMatchStore;
class PracticeDatabaseTest {
 @Test @Timeout(90) void completedAiGameIsStoredAndQueriedWithModeAndDifficulty()throws Exception{
  assumeTrue("1".equals(System.getenv("C2S_DB_TEST")));
  Properties p=new Properties();try(var reader=java.nio.file.Files.newBufferedReader(java.nio.file.Path.of("../.runtime/db.properties"))){p.load(reader);}
  String url=p.getProperty("url").replace("/card_two_sight?","/card_two_sight_test?");
  JdbcMatchStore store=new JdbcMatchStore(url,p.getProperty("user"),p.getProperty("password"));
  BlockingQueue<Event> events=new LinkedBlockingQueue<>();String matchId=null;
  try(LocalGameGateway gateway=new LocalGameGateway("JDBC연습테스트","male_punk","female_vest",PracticeDifficulty.EASY,Runnable::run,store)){
   gateway.setListener(events::add);gateway.send(Command.of(Action.CREATE));Set<String> sent=new HashSet<>();boolean finished=false;
   long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(75);
   while(System.nanoTime()<deadline){
    Event event=events.poll(2,TimeUnit.SECONDS);if(event==null)continue;assertNotEquals("ERROR",event.type,event.message);
    if("HISTORY".equals(event.type)){
     String id=matchId;MatchRecord row=event.records.stream().filter(x->id.equals(x.matchId)).findFirst().orElseThrow();
     assertEquals("AI",row.matchMode);assertEquals("EASY",row.aiDifficulty);assertEquals("JDBC연습테스트",row.playerA);
     if(finished){assertEquals(1,event.records.stream().filter(x->id.equals(x.matchId)).count());return;}
     finished=true;gateway.send(Command.of(Action.HISTORY));continue;
    }
    Snapshot s=event.state;if(s==null)continue;Command command=null;
    switch(s.phase){
     case LOBBY -> command=Command.of(s.me().ready?Action.START:Action.READY);
     case LOADOUT_SELECT -> {if(!s.me().loadoutReady)command=Command.loadout(List.of(Card.SCOUT,Card.PREDICT));}
     case BET_SELECT -> {if(!s.me().betReady)command=Command.number(Action.BET,s.me().chips==0?0:1);}
     case TILE_SELECT -> {if(!s.me().selected)command=Command.number(Action.TILE,s.hand.get(s.hand.size()-1));}
     case SPECIAL_SELECT -> {if(!s.me().specialReady)command=Command.special(null,0);}
     case ROUND_RESULT,ROULETTE_RESULT -> {if(!s.me().acknowledged)command=Command.of(Action.NEXT);}
     case AMMO_LOAD -> {if(s.pendingAmmo>0)for(int i=0;i<9;i++)if(!s.loadedChambers.contains(i)){command=Command.number(Action.LOAD_AMMO,i);break;}}
     case ROULETTE_SELECT -> {if(!s.me().chamberReady)command=Command.number(Action.CHAMBER,0);}
     case FINAL -> {matchId=s.matchId;command=Command.of(Action.HISTORY);}
    }
    if(command!=null&&sent.add(s.round+":"+s.phase+":"+command.action))gateway.send(command);
   }
   fail("AI game did not finish");
  }finally{
   if(matchId!=null)try(Connection c=DriverManager.getConnection(url,p.getProperty("user"),p.getProperty("password"));PreparedStatement q=c.prepareStatement("DELETE FROM match_results WHERE match_id=?")){q.setString(1,matchId);q.executeUpdate();}
  }
 }
}
