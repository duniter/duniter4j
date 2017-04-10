package org.duniter.elasticsearch.subscription.util.stringtemplate;

import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.StringUtils;
import org.nuiton.i18n.I18n;
import org.stringtemplate.v4.AttributeRenderer;

import java.util.Locale;

/**
 * Created by blavenie on 10/04/17.
 */
public class I18nRenderer implements AttributeRenderer{

    @Override
    public String toString(Object key, String formatString, Locale locale) {
        if (formatString == null || !formatString.startsWith("i18n")) return key.toString();
        String[] params = formatString.startsWith("i18n:") ? formatString.substring(5).split(",") : null;
        if (CollectionUtils.isNotEmpty(params)) {
            return I18n.l(locale, key.toString(), params);
        }
        return I18n.l(locale, key.toString());
    }
}
