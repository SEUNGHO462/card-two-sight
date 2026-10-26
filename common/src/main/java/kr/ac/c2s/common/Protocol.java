package kr.ac.c2s.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.util.*;

/** 화면·서버 담당자가 함께 사용하는 통신 계약. 비공개 패는 Snapshot.hand에만 존재한다. */
public final class Protocol {
    private Protocol() {}
    public static final Gson JSON = new GsonBuilder().disableHtmlEscaping().create();
    public static final int DEFAULT_PORT = 54555;
    public static final String[] CHARACTER_IDS = {"female_leather", "female_vest", "male_veteran", "female_silver", "male_punk", "male_handsome"};
    public static final String[] CHARACTER_NAMES = {"레나", "서린", "태식", "마담 류", "준", "도윤"};
    public enum Phase { LOBBY, TILE_SELECT, SPECIAL_SELECT, ROUND_RESULT, ROULETTE_SELECT, ROULETTE_RESULT, FINAL }
    public enum Card {
        REVERSE("역전"), SCOUT("정찰"), DISGUISE("위장"), PREDICT("예측");
        public final String label;
        Card(String label) { this.label = label; }
    }
    public enum Action { CREATE, JOIN, READY, START, TILE, SPECIAL, NEXT, HINT, CHAMBER, REMATCH, LEAVE }
    public static final class Command {
        public Action action;
        public String name, characterId, password, hint;
        public Card card;
        public int number;
        public static Command of(Action a) { Command c = new Command(); c.action = a; return c; }
        public static Command number(Action a, int n) { Command c = of(a); c.number = n; return c; }
        public static Command enter(Action a, String name, String id, String password) {
            Command c = of(a); c.name = name; c.characterId = id; c.password = password; return c;
        }
        public static Command special(Card card, int predicted) { Command c = number(Action.SPECIAL, predicted); c.card = card; return c; }
        public static Command hint(String hint, int chamber) { Command c = number(Action.HINT, chamber); c.hint = hint; return c; }
    }
    public static final class Player {
        public String name, characterId;
        public int life = 3, wins, remaining = 9, specialCount = 2, chips = 12, danger;
        public boolean ready, selected, specialReady, acknowledged, chamberReady, rematch;
        public Player() {}
        public Player(String name, String characterId) { this.name = name; this.characterId = characterId; }
        public Player copy() { return JSON.fromJson(JSON.toJson(this), Player.class); }
    }
    public static final class Snapshot {
        public String matchId = "", password = "", message = "", reason = "";
        public Phase phase = Phase.LOBBY;
        public int dangerDelta;
        public int you, round = 1, draws, winner = -2, ownTile, roundWinner = -2, ownChamber = -1;
        public String opponentParity = "?";
        public Player[] players = new Player[2];
        public List<Integer> hand = new ArrayList<>();
        public List<Card> cards = new ArrayList<>();
        public List<String> history = new ArrayList<>(), privateInfo = new ArrayList<>(), boughtHints = new ArrayList<>();
        public Map<Integer, Boolean> knownChambers = new HashMap<>();
        public boolean[] hit = new boolean[2];
        public Player me() { return players[you]; }
        public Player opponent() { return players[1 - you]; }
    }
    public static final class Event {
        public String type, message;
        public Snapshot state;
        public static Event state(Snapshot s) { Event e = new Event(); e.type = "STATE"; e.state = s; return e; }
        public static Event error(String text) { Event e = new Event(); e.type = "ERROR"; e.message = text; return e; }
        public static Event closed(String text) { Event e = error(text); e.type = "CLOSED"; return e; }
    }
    public static String cleanName(String s) {
        if (s == null) throw new IllegalArgumentException("닉네임을 입력하세요.");
        s = s.strip();
        if (s.length() < 1 || s.length() > 12 || s.chars().anyMatch(c -> Character.isISOControl(c) || c == '[' || c == ']'))
            throw new IllegalArgumentException("닉네임은 1~12자로 입력하세요.");
        return s;
    }
    public static String cleanPassword(String s) {
        if (s == null) throw new IllegalArgumentException("방 비밀번호를 입력하세요.");
        s = s.strip().toUpperCase(Locale.ROOT);
        if (!s.matches("[A-Z0-9가-힣]{3,16}")) throw new IllegalArgumentException("방 비밀번호는 한글·영문·숫자 3~16자입니다.");
        return s;
    }
    public static String cleanCharacter(String id) {
        if (!Arrays.asList(CHARACTER_IDS).contains(id)) throw new IllegalArgumentException("캐릭터를 선택하세요.");
        return id;
    }
    /** 소켓 입력이 무한히 커지는 것을 막는다. 한 메시지는 UTF-8 한 줄 JSON이다. */
    public static String readLine(Reader reader) throws IOException {
        StringBuilder b = new StringBuilder(); int c;
        while ((c = reader.read()) != -1) {
            if (c == '\n') return b.toString();
            if (c != '\r') b.append((char)c);
            if (b.length() > 16384) throw new IOException("메시지가 너무 큽니다.");
        }
        return b.length() == 0 ? null : b.toString();
    }
}
