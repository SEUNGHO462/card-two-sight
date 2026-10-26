package kr.ac.c2s.network;
import kr.ac.c2s.common.Protocol;
import kr.ac.c2s.common.Protocol.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** 네트워크 스레드에서 읽고, 화면 갱신은 전달받은 dispatch로만 실행한다. */
public final class SocketGameGateway implements GameGateway {
    private final String host; private final int port;
    private final Consumer<Runnable> dispatch;
    private volatile Consumer<Event> listener=e->{};
    private volatile Socket socket; private Writer writer; private volatile boolean closed;
    private final ExecutorService sender=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"c2s-client-send");t.setDaemon(true);return t;});
    public SocketGameGateway(String host,int port,Consumer<Runnable> dispatch) { this.host=host;this.port=port;this.dispatch=dispatch; }
    @Override public void setListener(Consumer<Event> listener) { this.listener=listener; }
    private void emit(Event event) { dispatch.accept(()->{ if(!closed) listener.accept(event); }); }
    @Override public void send(Command command) {
        if(closed) return;
        sender.execute(()->{
            try {
                if(socket==null) {
                    Socket created=new Socket(); socket=created; created.connect(new InetSocketAddress(host,port),4500);
                    created.setTcpNoDelay(true); writer=new BufferedWriter(new OutputStreamWriter(created.getOutputStream(),StandardCharsets.UTF_8));
                    Thread reader=new Thread(this::receive,"c2s-client-read"); reader.setDaemon(true); reader.start();
                }
                writer.write(Protocol.JSON.toJson(command));writer.write('\n');writer.flush();
            } catch(IOException e) { emit(Event.closed("서버에 연결할 수 없습니다. 주소·포트와 서버 실행 상태를 확인하세요.")); releaseSocket(); }
        });
    }
    private void receive() {
        try(Reader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8))) {
            String line; while(!closed && (line=Protocol.readLine(in))!=null) emit(Protocol.JSON.fromJson(line,Event.class));
            if(!closed) emit(Event.closed("서버 연결이 종료되었습니다."));
        } catch(Exception e) { if(!closed) emit(Event.closed("서버 연결이 끊겼습니다.")); }
    }
    private void releaseSocket() { try { if(socket!=null) socket.close(); } catch(IOException ignored) {} }
    @Override public void close() { closed=true;releaseSocket();sender.shutdownNow(); }
}
