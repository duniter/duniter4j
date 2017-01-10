package org.duniter.core.client.model.bma.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.client.model.bma.gson.JsonArrayParser;
import org.duniter.core.client.model.bma.gson.JsonAttributeParser;

import java.util.List;

/**
 * Created by blavenie on 07/12/16.
 */
public abstract class JacksonUtils extends SimpleModule {

    public static final String REGEX_ATTRIBUTE_REPLACE = "[,]?[\"\\s\\n\\r]*%s[\"]?[\\s\\n\\r]*:[\\s\\n\\r]*\"[^\"]+\"";


    public static ObjectMapper newObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Configure deserializer
        SimpleModule module = new SimpleModule();

        // Blockchain
        module.addDeserializer(BlockchainBlock.Identity.class, new IdentityDeserializer());
        module.addDeserializer(BlockchainBlock.Joiner.class, new JoinerDeserializer());
        module.addDeserializer(BlockchainBlock.Revoked.class, new RevokedDeserializer());
        module.addDeserializer(BlockchainBlock.Certification.class, new CertificationDeserializer());

        // Network
        module.addDeserializer(NetworkPeering.Endpoint.class, new EndpointDeserializer());

        objectMapper.registerModule(module);

        return objectMapper;
    }

    public static List<String> getValuesFromJSONAsString(String jsonString, String attributeName) {
        return new JsonAttributeParser(attributeName).getValues(jsonString);
    }

    public static String getValueFromJSONAsString(String jsonString, String attributeName) {
        return new JsonAttributeParser(attributeName).getValueAsString(jsonString);
    }

    public static Number getValueFromJSONAsNumber(String jsonString, String attributeName) {
        return new JsonAttributeParser(attributeName).getValueAsNumber(jsonString);
    }

    public static int getValueFromJSONAsInt(String jsonString, String attributeName) {
        return new JsonAttributeParser(attributeName).getValueAsInt(jsonString);
    }

    public static List<String> getArrayValuesFromJSONAsInt(String jsonString) {
        return new JsonArrayParser().getValuesAsList(jsonString);
    }

    public static String removeAttribute(String jsonString, String attributeName) {
        return jsonString.replaceAll(String.format(REGEX_ATTRIBUTE_REPLACE, attributeName), "");
    }

}
