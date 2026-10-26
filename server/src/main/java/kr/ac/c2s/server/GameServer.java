package kr.ac.c2s.server;

import kr.ac.c2s.common.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static kr.ac.c2s.common.Protocol.*;

/** 두 명의 방을 관리하는 TCP 서버. GUI가 없어 서버 PC에서 따로 실행할 수 있다. */
public final class GameServer implements AutoCloseable {
    private final ServerSocket listener;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Set<Session> clients = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean running = new AtomicBoolean(true);
    public GameServer(int port) throws IOException { listener = new ServerSocket(); listener.setReuseAddress(true); listener.bind(new InetSocketAddress(port)); }
    public int port() { return listener.getLocalPort(); }
    public void start() { daemon("c2s-accept", this::accept); }
    public static Thread daemon(String name, Runnable action) { Thread t=new Thread(action,name); t.setDaemon(true); t.start(); return t; }
    private void accept() {
        while(running.get()) try {
            Socket socket=listener.accept(); socket.setTcpNoDelay(true);
            if(clients.size()>=32) { socket.close(); continue; }
            Session s=new Session(socket); clients.add(s); s.start();
        } catch(IOException e) { if(running.get()) System.err.println("접속 처리 실패: "+e.getMessage()); }
    }
    private final class Session implements AutoCloseable {
        final Socket socket;
        final BlockingQueue<Event> outbound = new ArrayBlockingQueue<>(128);
        final AtomicBoolean closed = new AtomicBoolean();
        Room room; int index=-1; Player identity;
        Thread writer;
        Session(Socket socket) { this.socket=socket; }
        void start() {
            writer=daemon("c2s-writer",()->{
                try(Writer out=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                    while(!closed.get()) { Event e=outbound.take(); out.write(JSON.toJson(e)); out.write('\n'); out.flush(); }
                } catch(Exception ignored) { close(); }
            });
            daemon("c2s-reader",()->{
                try(Reader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8))) {
                    String line;
                    while(!closed.get() && (line=Protocol.readLine(in))!=null) {
                        try {
                            Command c=JSON.fromJson(line,Command.class);
                            if(c==null || c.action==null) throw new IllegalArgumentException("잘못된 명령입니다.");
                            if(c.action==Action.LEAVE) { close(); break; }
                            if(c.action==Action.CREATE || c.action==Action.JOIN) enter(this,c);
                            else { if(room==null) throw new IllegalArgumentException("먼저 방에 입장하세요."); room.apply(this,c); }
                        } catch(RuntimeException e) { send(Event.error(e instanceof IllegalArgumentException?e.getMessage():"메시지 형식이 올바르지 않습니다.")); }
                    }
                } catch(IOException ignored) {} finally { close(); }
            });
        }
        void send(Event e) { if(!closed.get() && !outbound.offer(e)) close(); }
        @Override public void close() {
            if(!closed.compareAndSet(false,true)) return;
            clients.remove(this); try { socket.close(); } catch(IOException ignored) {}
            if(writer!=null && Thread.currentThread()!=writer) writer.interrupt();
            Room previous=room; room=null; if(previous!=null) previous.depart(this);
        }
    }
    private synchronized void enter(Session s, Command c) {
        if(s.room!=null) throw new IllegalArgumentException("이미 방에 입장해 있습니다.");
        String password=cleanPassword(c.password);
        s.identity=new Player(cleanName(c.name),cleanCharacter(c.characterId));
        if(c.action==Action.CREATE) {
            if(rooms.size()>=16) throw new IllegalArgumentException("열린 방이 너무 많습니다.");
            if(rooms.containsKey(password)) throw new IllegalArgumentException("이미 사용 중인 방 비밀번호입니다.");
            Room r=new Room(password); rooms.put(password,r); r.join(s);
        } else {
            Room r=rooms.get(password);
            if(r==null) throw new IllegalArgumentException("해당 비밀번호의 방이 없습니다.");
            r.join(s);
        }
    }
    private final class Room {
        final String password;
        final Session[] seats=new Session[2];
        final boolean[] ready=new boolean[2], rematch=new boolean[2];
        GameEngine engine;
        Room(String password) { this.password=password; }
        synchronized void join(Session s) {
            if(engine!=null) throw new IllegalArgumentException("이미 진행 중인 방입니다.");
            int slot=seats[0]==null?0:seats[1]==null?1:-1;
            if(slot<0) throw new IllegalArgumentException("방이 가득 찼습니다.");
            seats[slot]=s; s.room=this; s.index=slot; broadcast();
        }
        synchronized void apply(Session s, Command c) {
            int p=s.index;
            if(seats[p]!=s) throw new IllegalArgumentException("입장 정보가 만료되었습니다.");
            if(c.action==Action.REMATCH) {
                if(engine==null || engine.phase()!=Phase.FINAL || seats[0]==null || seats[1]==null)
                    throw new IllegalArgumentException("두 플레이어가 연결된 최종 결과 화면에서 다시 할 수 있습니다.");
                rematch[p]=true;
                if(rematch[0]&&rematch[1]) begin();
            } else if(engine==null) {
                if(c.action==Action.READY) ready[p]=!ready[p];
                else if(c.action==Action.START) {
                    if(p!=0 || seats[1]==null || !ready[0] || !ready[1]) throw new IllegalArgumentException("방장이 두 사람의 준비 완료 후 시작할 수 있습니다.");
                    begin();
                } else throw new IllegalArgumentException("대기방 명령이 아닙니다.");
            } else engine.apply(p,c);
            broadcast();
        }
        void begin() { engine=new GameEngine(seats[0].identity,seats[1].identity,new SecureRandom()); Arrays.fill(rematch,false); }
        synchronized void broadcast() {
            for(int viewer=0;viewer<2;viewer++) if(seats[viewer]!=null) {
                Snapshot state;
                if(engine==null) {
                    state=new Snapshot(); state.you=viewer;
                    for(int p=0;p<2;p++) if(seats[p]!=null) { state.players[p]=seats[p].identity.copy(); state.players[p].ready=ready[p]; }
                } else {
                    state=engine.snapshot(viewer);
                    for(int p=0;p<2;p++) state.players[p].rematch=rematch[p];
                }
                state.password=password; seats[viewer].send(Event.state(state));
            }
        }
        synchronized void depart(Session s) {
            if(s.index<0 || seats[s.index]!=s) return;
            int p=s.index; seats[p]=null; ready[p]=false;
            if(engine!=null) { engine.forfeit(p); broadcast(); }
            else if(p==0) {
                if(seats[1]!=null) { seats[1].send(Event.closed("방장이 방을 나갔습니다.")); seats[1].room=null; seats[1]=null; }
            } else broadcast();
            if(p==0 || (seats[0]==null && seats[1]==null)) rooms.remove(password,this);
        }
    }
    @Override public void close() {
        if(!running.compareAndSet(true,false)) return;
        try { listener.close(); } catch(IOException ignored) {}
        for(Session s:List.copyOf(clients)) s.close(); rooms.clear();
    }
}
