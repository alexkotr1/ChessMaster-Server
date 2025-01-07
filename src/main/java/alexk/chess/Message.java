package alexk.chess;

import alexk.chess.Pionia.Pioni;
import alexk.chess.Serializers.PioniKeySerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.websocket.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private static final Logger logger = LogManager.getLogger(Message.class);
    public static ObjectMapper mapper;
    private Consumer<Message> replyCallback;
    public static final Map<String, Message> pending = new ConcurrentHashMap<>();
    static {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addKeySerializer(Pioni.class, new PioniKeySerializer());
        //module.addKeyDeserializer(Pioni.class, new PioniKeyDeserializer()); works without it?
        mapper.registerModule(module);
    }
    private RequestCodes code;
    private String data;
    private String messageID;
    private Pioni pioni;

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public RequestCodes getCode() {
        return code;
    }

    public void setCode(RequestCodes code) {
        this.code = code;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setData(Object o) {
        try {
            this.data = mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing data: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void setPioni(Pioni pioni) {
        this.pioni = pioni;
    }

    public Pioni getPioni() {
        if (pioni == null) {
            return null;
        }
        return pioni;
    }

    public static Message parse(String res) {
        try {
            return mapper.readValue(res, Message.class);
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
        return null;
    }

    public void send(Session session, Message replyTo) {
        try {
            if (replyTo != null) {
                this.messageID = replyTo.messageID;
            }
            if (getMessageID() == null) setMessageID(UUID.randomUUID().toString());
            pending.put(getMessageID(), this);
            String json = mapper.writeValueAsString(this);
            session.getBasicRemote().sendText(json);
            logger.info("Sent message with ID: {} Code: {} Data: {}", messageID, getCode(), data);
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
    public void onReply(Consumer<Message> callback) {
        if (this.replyCallback != null) {
            System.err.println("Callback already set");
            return;
        }
        this.replyCallback = callback;
    }

    public void triggerReplyCallback(Message response) {
        System.out.println("Replying to message ID: " + response.messageID);
        if (replyCallback != null) {
            System.out.println("replyCallback null");
            replyCallback.accept(response);
        }
    }
    @Override
    public String toString() {
        return "Message{" +
                "code=" + code +
                ", data='" + data + '\'' +
                ", messageID='" + messageID + '\'' +
                ", pioni='" + pioni + '\'' +
                '}';
    }
}
