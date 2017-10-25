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

        JsonAttributeParser<String> isAttribute = new JsonAttributeParser<>(PROPERTY_ID, String.class);
        String idValue = isAttribute.getValue(jsonString);
        Assert.assertEquals("joe", idValue);

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
