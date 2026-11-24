package kr.ac.c2s.server;
import static kr.ac.c2s.common.Protocol.*;
/** Terminal access to the same JDBC history used by the game server. */
public final class DatabaseRecords {
    public static void main(String[] args)throws Exception{
        JdbcMatchStore db=JdbcMatchStore.configured();if(db==null)throw new IllegalStateException("DB connection is not configured");
        db.initialize();System.out.println("MySQL JDBC connection OK");
        if(args.length>0&&args[0].equals("--check"))return;
        var rows=db.recent(20);if(rows.isEmpty())System.out.println("No saved matches yet.");
        for(MatchRecord r:rows)System.out.println(r.finishedAt+" | "+r.matchMode+(r.aiDifficulty==null?"":"/"+r.aiDifficulty)+" | "+r.playerA+" "+r.winsA+":"+r.winsB+" "+r.playerB+" | "+(r.winner<0?"무승부":r.winner==0?r.playerA:r.playerB)+" | "+r.reason);
    }
}
