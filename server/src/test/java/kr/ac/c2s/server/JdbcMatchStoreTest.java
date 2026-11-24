package kr.ac.c2s.server;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static kr.ac.c2s.common.Protocol.*;
import java.sql.*;
class JdbcMatchStoreTest {
 @Test void mysqlPersistsAndReloadsActualSocketMatchWithoutDuplicateRows()throws Exception{
  assumeTrue("1".equals(System.getenv("C2S_DB_TEST")),"Enable explicit local MySQL integration test");
  java.util.Properties config=new java.util.Properties();try(var reader=java.nio.file.Files.newBufferedReader(java.nio.file.Path.of("../.runtime/db.properties"))){config.load(reader);}
  String url=config.getProperty("url").replace("/card_two_sight?","/card_two_sight_test?");
  JdbcMatchStore store=new JdbcMatchStore(url,config.getProperty("user"),config.getProperty("password"));store.initialize();
  String matchId=null;
  try(GameServer server=new GameServer(0,store)){
   server.start();
   try(var a=new ServerIntegrationTest.Client(server.port());var b=new ServerIntegrationTest.Client(server.port())){
    a.send(Command.enter(Action.CREATE,"JDBC테스트A","male_punk","JDBCTEST"));a.phase(Phase.LOBBY);
    b.send(Command.enter(Action.JOIN,"JDBC테스트B","female_vest","JDBCTEST"));b.phase(Phase.LOBBY);
    a.send(Command.of(Action.READY));b.send(Command.of(Action.READY));a.await(e->e.state!=null&&e.state.players[1]!=null&&e.state.players[0].ready&&e.state.players[1].ready);
    a.send(Command.of(Action.START));a.phase(Phase.LOADOUT_SELECT);b.phase(Phase.LOADOUT_SELECT);
    a.send(Command.loadout(java.util.List.of(Card.SCOUT,Card.PREDICT)));b.send(Command.loadout(java.util.List.of(Card.REVERSE,Card.DISGUISE)));a.phase(Phase.BET_SELECT);b.phase(Phase.BET_SELECT);
    a.send(Command.number(Action.BET,3));b.send(Command.number(Action.BET,2));a.phase(Phase.TILE_SELECT);b.phase(Phase.TILE_SELECT);
    a.send(Command.number(Action.TILE,8));b.send(Command.number(Action.TILE,2));a.phase(Phase.SPECIAL_SELECT);b.phase(Phase.SPECIAL_SELECT);
    a.send(Command.special(null,0));b.send(Command.special(null,0));a.phase(Phase.ROUND_RESULT);b.phase(Phase.ROUND_RESULT);
    b.close();Snapshot result=a.phase(Phase.FINAL);matchId=result.matchId;
    a.send(Command.of(Action.HISTORY));Event history=a.await(e->"HISTORY".equals(e.type));
    String id=matchId;MatchRecord row=history.records.stream().filter(x->id.equals(x.matchId)).findFirst().orElseThrow();
    assertEquals("ONLINE",row.matchMode);assertNull(row.aiDifficulty);assertEquals("JDBC테스트A",row.playerA);assertEquals(1,row.winsA);assertEquals(0,row.winner);assertEquals("상대 연결 종료",row.reason);
    store.save(result);assertEquals(1,store.recent(100).stream().filter(x->id.equals(x.matchId)).count());
    JdbcMatchStore reopened=new JdbcMatchStore(url,config.getProperty("user"),config.getProperty("password"));assertTrue(reopened.recent(100).stream().anyMatch(x->id.equals(x.matchId)));
   }
  }finally{
   if(matchId!=null)try(Connection c=DriverManager.getConnection(url,config.getProperty("user"),config.getProperty("password"));PreparedStatement q=c.prepareStatement("DELETE FROM match_results WHERE match_id=?")){q.setString(1,matchId);q.executeUpdate();}
  }
 }
}
