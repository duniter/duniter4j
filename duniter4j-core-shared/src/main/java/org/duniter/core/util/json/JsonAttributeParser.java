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

import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.Preconditions;

import javax.print.DocFlavor;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonAttributeParser<T extends Object> {

    public enum Type {
        INTEGER,
        LONG,
        DOUBLE,
        BIGDECIMAL,
        BOOLEAN,
        STRING
    }

    public static final String REGEX_ATTRIBUTE_STRING_VALUE = "\\\"%s\\\"\\s*:\\s*(?:\"([^\"]+)\\\"|null)";
    public static final String REGEX_ATTRIBUTE_NUMERIC_VALUE = "\\\"%s\\\"\\s*:\\s*(?:([\\d]+(?:[.][\\d]+)?)|null)";
    public static final String REGEX_ATTRIBUTE_BOOLEAN_VALUE = "\\\"%s\\\"\\s*:\\s*(?:(true|false)|null)";

    public static JsonAttributeParser<String> newStringParser(final String attributeName){
        return new JsonAttributeParser<>(attributeName, String.class);
    }

    private Type type;
    private Pattern pattern;
    private DecimalFormat decimalFormat;
    private String attributeName;

    public JsonAttributeParser(String attributeName, Class<? extends T> clazz) {
        Preconditions.checkNotNull(attributeName);

        this.attributeName = attributeName;

        // String
        if (String.class.isAssignableFrom(clazz)) {
            type = Type.STRING;
            this.pattern = Pattern.compile(String.format(REGEX_ATTRIBUTE_STRING_VALUE, attributeName));
        }
        // Integer
        else if (Integer.class.isAssignableFrom(clazz)) {
            type = Type.INTEGER;
            this.pattern = Pattern.compile(String.format(REGEX_ATTRIBUTE_NUMERIC_VALUE, attributeName));
            this.decimalFormat = new DecimalFormat();
            this.decimalFormat.setParseIntegerOnly(true);
        }
        // Long
        else if (Long.class.isAssignableFrom(clazz)) {
            type = Type.LONG;
            this.pattern = Pattern.compile(String.format(REGEX_ATTRIBUTE_NUMERIC_VALUE, attributeName));
            this.decimalFormat = new DecimalFormat();
        }
        // Double
        else if (Double.class.isAssignableFrom(clazz)) {
            type = Type.DOUBLE;
            this.pattern = Pattern.compile(String.format(REGEX_ATTRIBUTE_NUMERIC_VALUE, attributeName));
            this.decimalFormat = new DecimalFormat();
            this.decimalFormat.getDecimalFormatSymbols().setDecimalSeparator('.');
        }
        // BigDecimal
        else if (BigDecimal.class.isAssignableFrom(clazz)) {
            type = Type.BIGDECIMAL;
            this.pattern = Pattern.compile(String.format(REGEX_ATTRIBUTE_NUMERIC_VALUE, attributeName));
            this.decimalFormat = new DecimalFormat();
            this.decimalFormat.setParseBigDecimal(true); // allow big decimal
            this.decimalFormat.getDecimalFormatSymbols().setDecimalSeparator('.');
        }
        // Boolean
        else if (Boolean.class.isAssignableFrom(clazz)) {
            type = Type.BOOLEAN;
            this.pattern = Pattern.compile(String.format(REGEX_ATTRIBUTE_BOOLEAN_VALUE, attributeName));
        }
        else {
            throw new IllegalArgumentException("Invalid attribute class " + clazz.getCanonicalName());
        }
    }

    public T getValue(String jsonString) {
        Preconditions.checkNotNull(jsonString);

        Matcher matcher = pattern.matcher(jsonString);

        if (!matcher.find()) {
            return null;
        }

        return parseValue(matcher.group(1));
    }

    public List<T> getValues(String jsonString) {
        Preconditions.checkArgument(type == Type.STRING);

        Matcher matcher = pattern.matcher(jsonString);
        List<T> result = new ArrayList<T>();
        while (matcher.find()) {
            String strValue = matcher.group(1);
            result.add(parseValue(strValue));
        }

        return result;
    }

    public String removeFromJson(final String jsonString) {
        Matcher matcher = pattern.matcher(jsonString);
        if (!matcher.find()) {
            return jsonString;
        }

        int start = matcher.start();
        int end = matcher.end();

        char before = jsonString.charAt(start-1);
        boolean hasCommaBefore = before == ',';
        while (before == ',' || before == ' ' || before == '\t' || before == '\n') {
            before = jsonString.charAt(--start-1);
            hasCommaBefore = hasCommaBefore || (before == ',');
        }
        char after = jsonString.charAt(end);
        while ((!hasCommaBefore && after == ',') || after == ' ' || after == '\t' || after == '\n') {
            after = jsonString.charAt(++end);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(jsonString.substring(0, start));
        sb.append(jsonString.substring(end));
        return sb.toString();
    }

    /* -- private methods -- */

    private T parseValue(String attributeValue) {

        switch(type) {
            case STRING:
                return (T)attributeValue;
            case INTEGER:
                try {
                    Number result = decimalFormat.parse(attributeValue);
                    return (T)new Integer(result.intValue());
                } catch (ParseException e) {
                    throw new TechnicalException(String.format("Error while parsing json numeric value, for attribute [%s]: %s", attributeName,e.getMessage()), e);
                }
            case LONG:
                try {
                    Number result = decimalFormat.parse(attributeValue);
                    return (T)new Long(result.longValue());
                } catch (ParseException e) {
                    throw new TechnicalException(String.format("Error while parsing json numeric value, for attribute [%s]: %s", attributeName,e.getMessage()), e);
                }
            case DOUBLE:
                try {
                    Number result = decimalFormat.parse(attributeValue);
                    return (T)new Double(result.doubleValue());
                } catch (ParseException e) {
                    throw new TechnicalException(String.format("Error while parsing json numeric value, for attribute [%s]: %s", attributeName,e.getMessage()), e);
                }
            case BIGDECIMAL:
                try {
                    Number result = decimalFormat.parse(attributeValue);
                    return (T)result;
                } catch (ParseException e) {
                    throw new TechnicalException(String.format("Error while parsing json numeric value, for attribute [%s]: %s", attributeName,e.getMessage()), e);
                }
            case BOOLEAN:
                return (T)new Boolean(attributeValue);
        }

        return null;
    }

}