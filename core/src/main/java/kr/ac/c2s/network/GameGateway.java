package kr.ac.c2s.network;
import kr.ac.c2s.common.Protocol.*;
import java.util.function.Consumer;
public interface GameGateway extends AutoCloseable {
    void send(Command command);
    void setListener(Consumer<Event> listener);
    @Override void close();
}
