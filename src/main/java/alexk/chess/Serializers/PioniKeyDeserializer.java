package alexk.chess.Serializers;

import alexk.chess.Pionia.*;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

public class PioniKeyDeserializer extends KeyDeserializer {
    @Override
    public Object deserializeKey(String key, com.fasterxml.jackson.databind.DeserializationContext ctxt) {
        String[] parts = key.split("/-/");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid Pioni key format: " + key);
        }

        String type = parts[0];
        Boolean isWhite = Boolean.parseBoolean(parts[1]);
        char x = parts[2].charAt(0);
        int y = Integer.parseInt(parts[3]);
        String id = parts[4];
        Boolean captured = Boolean.parseBoolean(parts[5]);

        return switch (type) {
            case "Stratiotis" -> new Stratiotis(isWhite, null, x, y, id, captured);
            case "Pyrgos" -> new Pyrgos(isWhite, null, x, y, id, captured);
            case "Alogo" -> new Alogo(isWhite, null, x, y, id, captured);
            case "Stratigos" -> new Stratigos(isWhite, null, x, y, id, captured);
            case "Vasilissa" -> new Vasilissa(isWhite, null, x, y, id, captured);
            case "Vasilias" -> new Vasilias(isWhite, null, x, y, id, captured);
            default -> throw new IllegalArgumentException("Unknown Pioni type: " + type);
        };
    }
}
