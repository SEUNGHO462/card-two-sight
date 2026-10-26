package kr.ac.c2s.server;
import java.util.concurrent.CountDownLatch;
import kr.ac.c2s.common.Protocol;
public final class ServerLauncher {
    public static void main(String[] args) throws Exception {
        int port=args.length>0?Integer.parseInt(args[0]):Protocol.DEFAULT_PORT;
        GameServer server=new GameServer(port); server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.out.println("CARD TWO SIGHT 서버 실행 · 포트 "+server.port()+" · 종료 Ctrl+C");
        new CountDownLatch(1).await();
    }
}
