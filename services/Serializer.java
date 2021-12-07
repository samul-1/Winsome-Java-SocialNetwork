package services;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

public class Serializer<T> {
    private final ObjectMapper mapper = new ObjectMapper();

    public String serialize(T obj) {
        this.mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public T parse(String source, Class<T> cls) throws JsonMappingException, JsonProcessingException {
        return mapper.readValue(source, this.mapper.getTypeFactory().constructType(cls));
    }

    public T parse(File source, Class<T> cls) throws IOException {
        return mapper.readValue(source, this.mapper.getTypeFactory().constructType(cls));
    }

}
