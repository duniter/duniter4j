package org.duniter.elasticsearch.service.changes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonSyntaxException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.elasticsearch.exception.InvalidFormatException;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.joda.time.DateTime;

import java.io.IOException;

/**
 * Created by blavenie on 30/11/16.
 */
public class ChangeUtils {

    public static ChangeEvent fromJson(ObjectMapper objectMapper, String json) {
        try {
            JsonNode actualObj = objectMapper.readTree(json);
            String index = actualObj.get("_index").asText();
            String type = actualObj.get("_type").asText();
            String id = actualObj.get("_id").asText();
            DateTime timestamp = new DateTime(actualObj.get("_timestamp").asLong());
            ChangeEvent.Operation operation = ChangeEvent.Operation.valueOf(actualObj.get("_operation").asText());
            long version = actualObj.get("_version").asLong();

            JsonNode sourceNode = actualObj.get("_source");
            BytesReference source = null;
            if (sourceNode != null) {
                // TODO : fill bytes reference from source
                //source = sourceNode.
            }

            ChangeEvent event = new ChangeEvent(index, type, id, timestamp, operation, version, source);
            return event;
        } catch (IOException | JsonSyntaxException e) {
            throw new InvalidFormatException("Invalid record JSON: " + e.getMessage(), e);
        }
    }

    public static String toJson(ChangeEvent change) {
        try {
            XContentBuilder builder = new XContentBuilder(JsonXContent.jsonXContent, new BytesStreamOutput());
            builder.startObject()
                    .field("_index", change.getIndex())
                    .field("_type", change.getType())
                    .field("_id", change.getId())
                    .field("_timestamp", change.getTimestamp())
                    .field("_version", change.getVersion())
                    .field("_operation", change.getOperation().toString());
            if (change.getSource() != null) {
                builder.rawField("_source", change.getSource());
            }
            builder.endObject();

            return builder.string();
        } catch (IOException e) {
            throw new TechnicalException("Error while generating JSON from change event", e);
        }
    }
}
