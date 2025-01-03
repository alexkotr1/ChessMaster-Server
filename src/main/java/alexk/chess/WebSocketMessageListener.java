package alexk.chess;

import jakarta.websocket.Session;

public interface WebSocketMessageListener {
    void onMessageReceived(Message message, Session session);
    void onCloseReceived(Session session);

}
