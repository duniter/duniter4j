package org.duniter.elasticsearch.subscription.util.stringtemplate;

import org.stringtemplate.v4.AttributeRenderer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateRenderer implements AttributeRenderer {

    public DateRenderer() {
    }

    public String toString(Object o, String formatString, Locale locale) {
        if(formatString == null) {
            formatString = "short";
        }

        Date d;
        if(o instanceof Calendar) {
            d = ((Calendar)o).getTime();
        } else {
            d = (Date)o;
        }

        Integer styleI = (Integer)org.stringtemplate.v4.DateRenderer.formatToInt.get(formatString);
        Object f;
        if(styleI == null) {
            f = new SimpleDateFormat(formatString, locale);
        } else {
            int style = styleI.intValue();
            if(formatString.startsWith("date:")) {
                f = DateFormat.getDateInstance(style, locale);
            } else if(formatString.startsWith("time:")) {
                f = DateFormat.getTimeInstance(style, locale);
            } else {
                f = DateFormat.getDateTimeInstance(style, style, locale);
            }
        }

        return ((DateFormat)f).format(d);
    }
}