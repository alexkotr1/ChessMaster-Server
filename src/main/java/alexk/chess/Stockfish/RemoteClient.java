package alexk.chess.Stockfish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.websocket.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ClientEndpoint
public class RemoteClient extends Client {
    private static Session session;
    private static ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LogManager.getLogger(RemoteClient.class);
    private CompletableFuture<JsonNode> futureResponse = new CompletableFuture<>();

    @Override
    public void startEngine() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(this, URI.create("wss://chess-api.com/v1"));
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Override
    public void sendCommand(String command) {

    }

    @Override
    public String getOutput() {
        return "";
    }

    @Override
    public String getBestMove(String fen, long whiteTime, long blackTime) {
        try {
            if (!session.isOpen()) {
                startEngine();
            }
            ObjectNode req = mapper.createObjectNode();
            req.put("fen", fen);
            req.put("depth", 18);
            req.put("maxThinkingTime", 100);

            futureResponse = new CompletableFuture<>();
            session.getBasicRemote().sendText(mapper.writeValueAsString(req));

            JsonNode res = futureResponse.get();
            return res.get("move").asText();

        } catch (IOException e) {
            logger.error("Error sending WebSocket message", e);
        } catch (Exception e) {
            logger.error("Unexpected error", e);
        }
        return null;
    }

    @OnMessage
    public void onMessage(String message) {
        logger.info("Received message from server: {}", message);
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(message);
            String type = node.get("type").asText();
            if (type.equals("bestmove")) futureResponse.complete(node);
        }
         catch (Exception e) {
            logger.error("Unexpected error", e);
        }
    }

    @Override
    public void stopEngine() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    @Override
    public float getEvalScore(String fen, int waitTime) {
        return 0;
    }
}
