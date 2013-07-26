package org.deephacks.confit.internal.jaxrs;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.deephacks.confit.test.DateTime;
import org.deephacks.confit.test.DurationTime;

import java.io.IOException;

public class Serializers {

    public static class DateTimeDeserializer extends JsonDeserializer<DateTime> {
        @Override
        public DateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            return new DateTime(node.get("dateTimeString").getTextValue());
        }
    }

    public static class DurationTimeDeserializer extends JsonDeserializer<DurationTime> {
        @Override
        public DurationTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            int hours = node.get("hours").getIntValue();
            int minutes = node.get("minutes").getIntValue();
            int seconds = node.get("seconds").getIntValue();
            boolean negative = node.get("negative").getBooleanValue();
            return new DurationTime(negative, hours, minutes, seconds);
        }
    }

}
