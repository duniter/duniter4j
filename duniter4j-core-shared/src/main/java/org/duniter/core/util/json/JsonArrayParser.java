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

import org.duniter.core.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse JSON array content, without deserialize each item.
 *
 * Created by blavenie on 05/01/16.
 */
public class JsonArrayParser {

    enum ParserState {
        READING_OBJECT,
        READING_ARRAY
    }

    public String[] getValuesAsArray(String jsonArray) throws JsonSyntaxException {
        List<String> result = getValuesAsList(jsonArray);
        if (CollectionUtils.isEmpty(result)) {
            return null;
        }
        return result.toArray(new String[result.size()]);
    }

    public List<String> getValuesAsList(String jsonArray) throws JsonSyntaxException {
        ParserState state = ParserState.READING_ARRAY;
        List<String> result = new ArrayList<String>();
        StringBuilder currentObject = null;
        int i = 0;
        int parenthesisBalance = 0;
        for (char c : jsonArray.toCharArray()) {
            switch (c) {
                case '{': {
                    if (state == ParserState.READING_ARRAY) {
                        state = ParserState.READING_OBJECT;
                        currentObject = new StringBuilder();
                    }
                    parenthesisBalance++;
                    currentObject.append(c);
                    break;
                }
                case '}': {
                    if (state == ParserState.READING_ARRAY) {
                        throw new JsonSyntaxException("unexpected '}' at " + i);
                    } else {
                        currentObject.append(c);
                        parenthesisBalance--;
                        if (parenthesisBalance == 0) {
                            state = ParserState.READING_ARRAY;
                            result.add(currentObject.toString());
                        }
                    }
                    break;
                }
                default: {
                    if (state == ParserState.READING_OBJECT) {
                        currentObject.append(c);
                    }
                }
            }
            i++;
        }
        return result;
    }
}
