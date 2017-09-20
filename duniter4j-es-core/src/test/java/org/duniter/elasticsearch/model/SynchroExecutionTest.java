package org.duniter.elasticsearch.model;

import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


public class SynchroExecutionTest {

    @Test
    public void deserialize() {
        String json = "{\n" +
                "        \"issuer\" : null,\n" +
                "        \"hash\" : null,\n" +
                "        \"signature\" : null,\n" +
                "        \"time\" : 1505836503,\n" +
                "        \"currency\" : \"g1\",\n" +
                "        \"peer\" : \"CA99448CDD90AB3772474A4CBCCC5A392F4E9AD3F9FA1C4018C6FB432BC04BA8\",\n" +
                "        \"result\" : {\n" +
                "          \"inserts\" : 2,\n" +
                "          \"updates\" : 0,\n" +
                "          \"invalidSignatures\" : 0,\n" +
                "          \"deletes\" : 0\n" +
                "        }\n" +
                "      }";

        try {
            SynchroExecution obj = JacksonUtils.getThreadObjectMapper().readValue(json, SynchroExecution.class);
            Assert.assertNotNull(obj);
        }
        catch(IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
