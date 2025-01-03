package alexk.chess;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.glassfish.tyrus.server.Server;

import java.io.IOException;
import java.util.*;

@ServerEndpoint("/chat")
public class WebSocket {
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Client connected: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session ) {
        Game game = Game.findGameBySession(session);
        Message m = Message.parse(message);
        if (m == null) return;
        if (Message.pending.containsKey(m.getMessageID())) {
            Message reply = Message.pending.get(m.getMessageID());
            reply.triggerReplyCallback(reply);
            Message.pending.remove(reply.getMessageID());
        } else Message.pending.put(m.getMessageID(), m);
        if (game != null) game.onMessageReceived(m,session);
        if (game == null && m.getCode() == RequestCodes.JOIN_GAME){
            Game res = Game.join(session, m.getData());
            Message resMessage = new Message();
            resMessage.setCode(res != null ? RequestCodes.JOIN_GAME_SUCCESS : RequestCodes.JOIN_GAME_FAILURE);
            resMessage.setData(res != null ? res.getChessEngine().getMinutesAllowed() : 99);
            resMessage.send(session, m);
            if (res == null) return;
            Message notifyPlayer = new Message();
            notifyPlayer.setCode(RequestCodes.SECOND_PLAYER_JOINED);
            notifyPlayer.send(res.getWhite(),m);
            res.start();
        } else if (game == null && m.getCode() == RequestCodes.HOST_GAME){
            try {
                int time = Message.mapper.readValue(m.getData(),int.class);
                game = new Game(generateCode(),session,time);
                Message res = new Message();
                res.setCode(RequestCodes.HOST_GAME_RESULT);
                res.setData(game.getCode());
                res.send(session,m);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Client disconnected: " + session.getId());
    }

    public static void main(String[] args) {
        Set<Class<?>> endpointClasses = Collections.singleton(WebSocket.class);
        Map<String, Object> properties = new HashMap<>();
        Server server = new Server("localhost", 8025, "/", properties, endpointClasses);

        try {
            server.start();
            System.out.println("WebSocket server started on ws://localhost:8025/chat");
            System.out.println("Press Enter to stop the server...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
            System.out.println("WebSocket server stopped.");
        }
    }
    public static String generateCode() {
        StringBuilder word = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            char randomChar = (char) ('A' + random.nextInt(26));
            word.append(randomChar);
        }
        return word.toString();
    }
}
