package kr.ac.c2s.server;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import static kr.ac.c2s.common.Protocol.*;

/** Server-owned JDBC history. No client credentials or hidden cards are stored. */
public final class JdbcMatchStore {
    private final String url,user,password;
    public JdbcMatchStore(String url,String user,String password){this.url=url;this.user=user;this.password=password;}
    public static JdbcMatchStore configured() throws IOException {
        Path path=Path.of(System.getProperty("c2s.db.config",".runtime/db.properties"));
        Properties p=new Properties();
        if(Files.isRegularFile(path))try(Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)){p.load(reader);}
        String url=System.getenv().getOrDefault("C2S_DB_URL",p.getProperty("url",""));
        if(url.isBlank())return null;
        return new JdbcMatchStore(url,System.getenv().getOrDefault("C2S_DB_USER",p.getProperty("user","")),System.getenv().getOrDefault("C2S_DB_PASSWORD",p.getProperty("password","")));
    }
    private Connection open() throws SQLException {
        Properties p=new Properties();p.setProperty("user",user);p.setProperty("password",password);
        p.setProperty("forceConnectionTimeZoneToSession","true");
        p.setProperty("connectTimeout","2000");p.setProperty("socketTimeout","3000");
        return DriverManager.getConnection(url,p);
    }
    public void initialize() throws SQLException {
        try(Connection c=open();Statement s=c.createStatement()){
            s.executeUpdate("CREATE TABLE IF NOT EXISTS match_results (match_id VARCHAR(36) PRIMARY KEY, player_a VARCHAR(48) NOT NULL, player_b VARCHAR(48) NOT NULL, wins_a INT NOT NULL, wins_b INT NOT NULL, life_a INT NOT NULL, life_b INT NOT NULL, winner INT NOT NULL, rounds INT NOT NULL, reason VARCHAR(100) NOT NULL, match_mode VARCHAR(10) NOT NULL DEFAULT 'ONLINE', ai_difficulty VARCHAR(10) NULL, finished_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)) CHARACTER SET utf8mb4");
        }
    }
    public void save(Snapshot s) throws SQLException {save(s,"ONLINE",null);}
    public void save(Snapshot s,String mode,String difficulty) throws SQLException {
        if(!Set.of("ONLINE","AI").contains(mode))throw new IllegalArgumentException("Unknown match mode");
        if(mode.equals("AI")&&!Set.of("EASY","NORMAL","HARD").contains(difficulty==null?"":difficulty))throw new IllegalArgumentException("Unknown AI difficulty");
        if(mode.equals("ONLINE"))difficulty=null;
        if(s.phase!=Phase.FINAL||s.matchId.isBlank())throw new IllegalArgumentException("Only completed matches can be stored");
        String sql="INSERT INTO match_results (match_id,player_a,player_b,wins_a,wins_b,life_a,life_b,winner,rounds,reason,match_mode,ai_difficulty) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try(Connection c=open();PreparedStatement q=c.prepareStatement(sql)){
            q.setString(1,s.matchId);q.setString(2,s.players[0].name);q.setString(3,s.players[1].name);
            q.setInt(4,s.players[0].wins);q.setInt(5,s.players[1].wins);q.setInt(6,s.players[0].life);q.setInt(7,s.players[1].life);
            q.setInt(8,s.winner);q.setInt(9,s.round);q.setString(10,s.reason);q.setString(11,mode);q.setString(12,difficulty);q.executeUpdate();
        }catch(SQLIntegrityConstraintViolationException duplicate){if(duplicate.getErrorCode()!=1062)throw duplicate;}
    }
    public List<MatchRecord> recent(int limit) throws SQLException {
        List<MatchRecord> rows=new ArrayList<>();
        try(Connection c=open();PreparedStatement q=c.prepareStatement("SELECT * FROM match_results ORDER BY finished_at DESC,match_id DESC LIMIT ?")){
            q.setInt(1,Math.max(1,Math.min(100,limit)));
            try(ResultSet rs=q.executeQuery()){while(rs.next()){
                MatchRecord row=new MatchRecord();row.matchId=rs.getString("match_id");row.playerA=rs.getString("player_a");row.playerB=rs.getString("player_b");
                row.winsA=rs.getInt("wins_a");row.winsB=rs.getInt("wins_b");row.winner=rs.getInt("winner");row.reason=rs.getString("reason");
                row.matchMode=rs.getString("match_mode");row.aiDifficulty=rs.getString("ai_difficulty");
                row.finishedAt=rs.getTimestamp("finished_at").toInstant().toString();rows.add(row);
            }}
        }return rows;
    }
}
