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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.NetworkPeering;
import org.duniter.core.client.model.bma.NetworkWs2pHeads;
import org.duniter.core.client.model.bma.Ws2pHead;
import org.duniter.core.client.model.bma.converter.*;
import org.duniter.core.util.converter.Converter;
import org.duniter.core.util.jackson.JsonDeserializerConverterAdapter;
import org.duniter.core.util.jackson.JsonSerializerConverterAdapter;
import org.duniter.core.util.jackson.ToStringJsonSerializer;

import javax.annotation.Nullable;

/**
 * Created by blavenie on 07/12/16.
 */
public abstract class JacksonUtils extends SimpleModule {



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
        SimpleModule module = new SimpleModule()

        // Blockchain
        .addDeserializer(BlockchainBlock.Identity.class, new JsonDeserializerConverterAdapter<>(StringToIdentityConverter.class))
        .addDeserializer(BlockchainBlock.Joiner.class, new JsonDeserializerConverterAdapter<>(StringToJoinerConverter.class))
        .addDeserializer(BlockchainBlock.Revoked.class, new JsonDeserializerConverterAdapter<>(StringToRevokedConverter.class))
        .addDeserializer(BlockchainBlock.Certification.class, new JsonDeserializerConverterAdapter<>(StringToCertificationConverter.class))

        // Network
        .addDeserializer(NetworkPeering.Endpoint.class, new JsonDeserializerConverterAdapter<>(StringToEndpointConverter.class, false))
        .addDeserializer(Ws2pHead.class, new JsonDeserializerConverterAdapter<>(StringToWs2pHeadConverter.class, false))
        .addSerializer(NetworkPeering.Endpoint.class, new ToStringJsonSerializer<>(false))
        .addSerializer(Ws2pHead.class, new ToStringJsonSerializer(false));

        objectMapper.registerModule(module);

        // Adding features
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //objectMapper.getFactory().configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);

        return objectMapper;
    }

}
