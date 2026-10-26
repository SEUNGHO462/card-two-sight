package kr.ac.c2s.game;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import kr.ac.c2s.common.Protocol.*;
import static org.junit.jupiter.api.Assertions.*;

class RecordStoreTest {
    @TempDir Path directory;
    @Test void koreanRecordsPersistAndDuplicateFinalDoesNotCountTwice() throws Exception {
        String previous=System.getProperty("c2s.dataDir");
        System.setProperty("c2s.dataDir",directory.toString());
        try {
            RecordStore store=new RecordStore();store.nickname="승호";store.character="male_handsome";
            Snapshot s=new Snapshot();s.phase=Phase.FINAL;s.matchId="one-match";s.you=0;s.winner=0;
            store.record(s);store.record(s);
            assertEquals(1,store.wins);assertTrue(store.lastError.isEmpty());
            String json=Files.readString(directory.resolve("기록.json"));assertTrue(json.contains("닉네임"));assertTrue(json.contains("승리"));
            RecordStore restored=new RecordStore();assertEquals("승호",restored.nickname);assertEquals(1,restored.wins);
            restored.record(s);assertEquals(1,restored.wins);
            s.matchId="draw-match";s.winner=-1;restored.record(s);assertEquals(1,new RecordStore().draws);
        } finally {if(previous==null)System.clearProperty("c2s.dataDir");else System.setProperty("c2s.dataDir",previous);}
    }
}
