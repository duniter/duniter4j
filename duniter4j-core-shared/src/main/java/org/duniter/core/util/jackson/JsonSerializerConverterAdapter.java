package org.duniter.core.util.jackson;

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
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.extern.slf4j.Slf4j;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.converter.Converter;
import org.duniter.core.util.json.JsonSyntaxException;

import java.io.IOException;

/**
 * Convert an object into a string
 */
@Slf4j
public class JsonSerializerConverterAdapter<T> extends JsonSerializer<T> {

    private final Converter<T, String> converter;
    private final boolean failIfInvalid;

    public JsonSerializerConverterAdapter(Class<? extends Converter<T, String>> converterClass) {
        this(converterClass, true);
    }

    public JsonSerializerConverterAdapter(
            Class<? extends Converter<T, String>> converterClass,
            boolean failIfInvalid) {
        try {
            converter = converterClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TechnicalException(e);
        }
        this.failIfInvalid = failIfInvalid;
    }

    public JsonSerializerConverterAdapter(
            Converter<T, String> converter) {
        this(converter, true);
    }

    public JsonSerializerConverterAdapter(
            Converter<T, String> converter,
            boolean failIfInvalid) {
        this.converter = converter;
        this.failIfInvalid = failIfInvalid;
    }

    @Override
    public void serialize(T head, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) {

        try {
            jsonGenerator.writeString(converter.convert(head));
        }
        // Unable to serialize
        catch(IOException e) {
            // Fail
            if (failIfInvalid) {
                throw new JsonSyntaxException(e);
            }
            // Or continue
            if (log.isDebugEnabled()) {
                log.warn(e.getMessage(), e); // link the exception
            }
            else {
                log.warn(e.getMessage());
            }
        }
    }
}