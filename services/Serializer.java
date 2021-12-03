package services;

import com.fasterxml.jackson.databind.ObjectMapper;

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
        }
        return null;

    }

    public T parse(String source, Class<T> cls) {
        try {
            return mapper.readValue(source, this.mapper.getTypeFactory().constructType(cls));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
