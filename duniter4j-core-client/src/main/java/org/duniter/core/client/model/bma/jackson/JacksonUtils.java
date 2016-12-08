package org.duniter.core.client.model.bma.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.NetworkPeering;

/**
 * Created by blavenie on 07/12/16.
 */
public abstract class JacksonUtils extends SimpleModule {

    public static ObjectMapper newObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Configure deserializer
        SimpleModule module = new SimpleModule();
        module.addDeserializer(BlockchainBlock.Identity.class, new IdentityDeserializer());
        module.addDeserializer(BlockchainBlock.Joiner.class, new JoinerDeserializer());
        module.addDeserializer(BlockchainBlock.Revoked.class, new RevokedDeserializer());
        module.addDeserializer(NetworkPeering.Endpoint.class, new EndpointDeserializer());

        objectMapper.registerModule(module);

        return objectMapper;
    }
}
