package org.duniter.elasticsearch.dao.handler;

import java.util.regex.Pattern;

public class AddSequenceAttributeHandler implements StringReaderHandler {
        private int order;
        private final String attributeName;
        private final Pattern filterPattern;
        public AddSequenceAttributeHandler(String attributeName, String filterRegex, int startValue) {
            this.order = startValue;
            this.attributeName = attributeName;
            this.filterPattern = Pattern.compile(filterRegex);
        }

        @Override
        public String onReadLine(String line) {
            // add 'order' field into
            if (filterPattern.matcher(line).matches()) {
                return String.format("%s, \"%s\": %d}",
                        line.substring(0, line.length()-1),
                        attributeName,
                        order++);
            }
            return line;
        }
    }