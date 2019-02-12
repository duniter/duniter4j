package org.duniter.core.util.json;

/*
 * #%L
 * UCoin Java :: Core Client API
 * %%
 * Copyright (C) 2014 - 2016 EIS
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

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by blavenie on 05/01/16.
 */
public class JsonAttributeParserTest {

    private static final String PROPERTY_ID = "id";
    private static final String PROPERTY_TS = "ts";
    private static final String PROPERTY_B = "b";

    private  static final String TS_VALUE = "2014-12-02T13:58:23.801+0100";
    private static final String OBJ_JSON = ("{'id':'joe','ts':'" + TS_VALUE + "','foo':{'bar':{'v1':50019820,'v2':0,     'v3':0.001, 'v4':-100, 'v5':0.000001, 'v6':0.0, 'b':true}}}")
            .replace("'", "\"");

    @Test
    public void getValueAsString() {
        String jsonString = String.format("%s", OBJ_JSON);

        JsonAttributeParser<String> idAttribute = new JsonAttributeParser<>(PROPERTY_ID, String.class);
        String idValue = idAttribute.getValue(jsonString);
        Assert.assertEquals("joe", idValue);

    }

    @Test
    public void getValueAsLong() {

        String jsonString = "{\n" +
                "    \"version\": 10,\n" +
                "    \"nonce\": 10400000001312,\n" +
                "    \"number\": 755,\n" +
                "    \"powMin\": 81,\n" +
                "    \"time\": 1489204846,\n" +
                "    \"medianTime\": 1489201914,\n" +
                "    \"membersCount\": 59,\n" +
                "    \"monetaryMass\": 177000,\n" +
                "    \"unitbase\": 0,\n" +
                "    \"issuersCount\": 19,\n" +
                "    \"issuersFrame\": 96,\n" +
                "    \"issuersFrameVar\": 0,\n" +
                "    \"currency\": \"g1\",\n" +
                "    \"issuer\": \"Com8rJukCozHZyFao6AheSsfDQdPApxQRnz7QYFf64mm\",\n" +
                "    \"signature\": \"HSA1F2JEf9vsORi5ABUrEgRKfLHFM76Yks9ibH1dgcQ/UBlo1iP9bYUT+lUQDgG5+EDUwuGQq5v+cJEVGciZDw==\",\n" +
                "    \"hash\": \"000004C0781DA1F71571A83D23D8B38D67E2F73529D833917A274C372D2968C2\",\n" +
                "    \"parameters\": \"\",\n" +
                "    \"previousHash\": \"00000937703D4C34B234B18B7C5E7E67462BC8EBC0144AA6DE42FAD169C8EF05\",\n" +
                "    \"previousIssuer\": \"4fHMTFBMo5sTQEc5p1CNWz28S4mnnqdUBmECq1zt4n2m\",\n" +
                "    \"inner_hash\": \"7210E5D8B36BB2064D1DCFBE08C9FC8D0D107B87ED4D0AE8B6BF54C615366060\",\n" +
                "    \"dividend\": null,\n" +
                "    \"identities\": [],\n" +
                "    \"joiners\": [],\n" +
                "    \"actives\": [],\n" +
                "    \"leavers\": [],\n" +
                "    \"revoked\": [],\n" +
                "    \"excluded\": [],\n" +
                "    \"certifications\": [],\n" +
                "    \"transactions\": [],\n" +
                "    \"raw\": \"Version: 10\\nType: Block\\nCurrency: g1\\nNumber: 755\\nPoWMin: 81\\nTime: 1489204846\\nMedianTime: 1489201914\\nUnitBase: 0\\nIssuer: Com8rJukCozHZyFao6AheSsfDQdPApxQRnz7QYFf64mm\\nIssuersFrame: 96\\nIssuersFrameVar: 0\\nDifferentIssuersCount: 19\\nPreviousHash: 00000937703D4C34B234B18B7C5E7E67462BC8EBC0144AA6DE42FAD169C8EF05\\nPreviousIssuer: 4fHMTFBMo5sTQEc5p1CNWz28S4mnnqdUBmECq1zt4n2m\\nMembersCount: 59\\nIdentities:\\nJoiners:\\nActives:\\nLeavers:\\nRevoked:\\nExcluded:\\nCertifications:\\nTransactions:\\nInnerHash: 7210E5D8B36BB2064D1DCFBE08C9FC8D0D107B87ED4D0AE8B6BF54C615366060\\nNonce: 10400000001312\\n\"\n" +
                "  }";

        JsonAttributeParser<Long> idAttribute = new JsonAttributeParser<>("number", Long.class);
        Long number = idAttribute.getValue(jsonString);
        Assert.assertNotNull(number);
        Assert.assertEquals(755L, number.longValue());
    }

    @Test
    public void removeStringAttributeFromJson() {
        String jsonString = String.format("%s", OBJ_JSON);
        String expectedJson = jsonString.replace("\"id\":\"joe\",", "");
        expectedJson = expectedJson.replace(", \"b\":true", "");

        // Remove 'id'
        JsonAttributeParser<String> idAttribute = new JsonAttributeParser<>(PROPERTY_ID, String.class);
        String newJson = idAttribute.removeFromJson(jsonString);

        // Remove 'b'
        JsonAttributeParser<Boolean> bAttribute = new JsonAttributeParser<>(PROPERTY_B, Boolean.class);
        newJson = bAttribute.removeFromJson(newJson);

        Assert.assertEquals(expectedJson, newJson);

        // Remove 'ts'
        JsonAttributeParser<String> tsAttribute = new JsonAttributeParser<>(PROPERTY_TS, String.class);
        newJson = tsAttribute.removeFromJson(newJson);

        expectedJson = expectedJson.replace("\"ts\":\""+TS_VALUE+"\",", "");
        Assert.assertEquals(expectedJson, newJson);
    }
}
