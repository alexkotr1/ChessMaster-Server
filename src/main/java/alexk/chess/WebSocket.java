package alexk.chess;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.server.Server;
import java.util.*;

@ServerEndpoint("/")
public class WebSocket {
    private static final Logger logger = LogManager.getLogger(WebSocket.class);
    @OnOpen
    public void onOpen(Session session) { logger.info("Client connected: {}", session.getId()); }

    @OnMessage
    public void onMessage(String message, Session session ) {
        logger.info("Received message in Websocket: {}", message);
        Game game = Game.findGameBySession(session);
        Message m = Message.parse(message);
        if (m == null) return;
        if (Message.pending.containsKey(m.getMessageID())) {
            logger.info("Replying to message ID: {}", m.getMessageID());
            Message reply = Message.pending.get(m.getMessageID());
            reply.triggerReplyCallback(m);
            Message.pending.remove(reply.getMessageID());
            return;
        }
        if (game != null && m.getCode() != RequestCodes.JOIN_GAME && m.getCode() != RequestCodes.HOST_GAME && m.getCode() != RequestCodes.START_AI_GAME) {
            game.onMessageReceived(m,session);
            return;
        }
        switch (m.getCode()){
            case RequestCodes.JOIN_GAME -> {
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
            }
            case RequestCodes.HOST_GAME ->{
                try {
                    int time = Message.mapper.readValue(m.getData(),int.class);
                    game = new Game(Game.generateCode(),session,time,false);
                    Message res = new Message();
                    res.setCode(RequestCodes.HOST_GAME_RESULT);
                    res.setData(game.getCode());
                    res.send(session,m);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            case RequestCodes.START_AI_GAME -> {
                try {
                    int time = Message.mapper.readValue(m.getData(),int.class);
                    game = new Game(Game.generateCode(),session,time,true);
                    game.start();
                    Message res = new Message();
                    res.send(session,m);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("Client disconnected: {}", session.getId());
        Game g = Game.findGameBySession(session);
        if (g != null) g.onCloseReceived(session);
    }

    public static void main(String[] args) {
        Set<Class<?>> endpointClasses = Collections.singleton(WebSocket.class);
        Map<String, Object> properties = new HashMap<>();
        String host = "localhost";
        int port = 8080;
        Server server = new Server(host, port, "/", properties, endpointClasses);
        try {
            server.start();
            logger.info("WebSocket server started on ws://{}:{}/", host, port);
            logger.info("Press Enter to stop the server...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
            System.out.println("WebSocket server stopped.");
        }
    }

}
