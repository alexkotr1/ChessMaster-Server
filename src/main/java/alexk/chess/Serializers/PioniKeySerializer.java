package alexk.chess.Serializers;

import alexk.chess.Pionia.Pioni;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class PioniKeySerializer extends JsonSerializer<Pioni> {
    @Override
    public void serialize(Pioni value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String delimiter = "/-/";
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder
                .append(value.getType()).append(delimiter)
                .append(value.getIsWhite()).append(delimiter)
                .append(value.getXPos()).append(delimiter)
                .append(value.getYPos()).append(delimiter)
                .append(value.getID()).append(delimiter)
                .append(value.getCaptured()).append(delimiter);
        gen.writeFieldName(keyBuilder.toString());
    }
}
