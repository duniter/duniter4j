package org.duniter.core.client.model.bma.jackson;

/*
 * #%L
 * Duniter4j :: Core Client API
 * %%
 * Copyright (C) 2014 - 2017 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.util.json.JsonArrayParser;
import org.duniter.core.util.json.JsonAttributeParser;

import java.util.List;

/**
 * Created by blavenie on 07/12/16.
 */
public abstract class JacksonUtils extends SimpleModule {

    public static final String REGEX_ATTRIBUTE_REPLACE = "[,]?(?:\"%s\"|%s)[\\s\\n\\r]*:[\\s\\n\\r]*(?:\"[^\"]+\"|null)";


    private static final ThreadLocal<ObjectMapper> mapper = new ThreadLocal<ObjectMapper>() {
        @Override
        protected ObjectMapper initialValue() {
            return newObjectMapper();
        }
    };

    public static ObjectMapper getThreadObjectMapper() {
        return mapper.get();
    }

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

        // Adding features
        //objectMapper.getFactory().configure(JsonGenerator.Feature., true);

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
        String regex = String.format(REGEX_ATTRIBUTE_REPLACE, attributeName, attributeName);
        return jsonString.replaceAll(regex, "");
    }

}
