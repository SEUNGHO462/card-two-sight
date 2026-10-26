package kr.ac.c2s.game;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;
import kr.ac.c2s.common.Protocol.*;

/** 닉네임·설정·최종 승패 기록을 한글 JSON 키로 저장한다. 방 비밀번호는 저장하지 않는다. */
public final class RecordStore {
    private final Path file;
    public String nickname="플레이어", character="male_handsome";
    public float volume=.45f; public boolean sound=true;
    public int wins,losses,draws;
    private final Set<String> recorded=new LinkedHashSet<>();
    public String lastError="";
    public RecordStore() {
        Path dir=Path.of(System.getProperty("c2s.dataDir",Path.of(System.getProperty("user.home"),".card-two-sight").toString()));file=dir.resolve("기록.json");
        if(!Files.isRegularFile(file))return;
        try {
            JsonObject o=JsonParser.parseString(Files.readString(file,StandardCharsets.UTF_8)).getAsJsonObject();
            nickname=text(o,"닉네임",nickname);character=text(o,"캐릭터",character);
            wins=number(o,"승리",0);losses=number(o,"패배",0);draws=number(o,"무승부",0);
            volume=o.has("음량")?Math.max(0,Math.min(1,o.get("음량").getAsFloat())):volume;
            sound=!o.has("효과음")||o.get("효과음").getAsBoolean();
            if(o.has("기록한경기"))for(JsonElement e:o.getAsJsonArray("기록한경기"))recorded.add(e.getAsString());
        }catch(Exception e){lastError="기록 파일을 읽지 못했습니다. 새 기록으로 시작합니다.";}
    }
    private static String text(JsonObject o,String k,String fallback){return o.has(k)?o.get(k).getAsString():fallback;}
    private static int number(JsonObject o,String k,int fallback){return o.has(k)?Math.max(0,o.get(k).getAsInt()):fallback;}
    public void record(Snapshot s) {
        if(s.phase!=Phase.FINAL||s.matchId.isBlank()||!recorded.add(s.matchId))return;
        if(s.winner<0)draws++;else if(s.winner==s.you)wins++;else losses++;
        save();
    }
    public void save() {
        JsonObject o=new JsonObject();o.addProperty("닉네임",nickname);o.addProperty("캐릭터",character);
        o.addProperty("승리",wins);o.addProperty("패배",losses);o.addProperty("무승부",draws);
        o.addProperty("음량",volume);o.addProperty("효과음",sound);
        JsonArray ids=new JsonArray();recorded.forEach(ids::add);o.add("기록한경기",ids);
        try {
            Files.createDirectories(file.getParent());Path tmp=file.resolveSibling("기록.tmp");
            Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(o),StandardCharsets.UTF_8);
            try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
            lastError="";
        }catch(IOException e){lastError="기록을 저장하지 못했습니다. 저장 폴더 권한을 확인하세요.";}
    }
}
