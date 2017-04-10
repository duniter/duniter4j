package org.duniter.elasticsearch.subscription.util.stringtemplate;

import org.duniter.core.client.model.ModelUtils;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.StringUtils;
import org.nuiton.i18n.I18n;
import org.stringtemplate.v4.AttributeRenderer;

import java.util.Locale;

/**
 * Add format capabilities: i18n, pubkey
 * Created by blavenie on 10/04/17.
 */
public class StringRenderer extends org.stringtemplate.v4.StringRenderer{

    @Override
    public String toString(Object o, String formatString, Locale locale) {
        return formatString == null ? (String)o :
                (formatString.equals("pubkey") ? ModelUtils.minifyPubkey((String)o) :
                        (formatString.startsWith("i18n") ? toI18nString(o, formatString, locale) :
                            super.toString(o, formatString, locale)));
    }

    protected String toI18nString(Object key, String formatString, Locale locale) {
        String[] params = formatString.startsWith("i18n:") ? formatString.substring(5).split(",") : null;
        if (CollectionUtils.isNotEmpty(params)) {
            return I18n.l(locale, key.toString(), params);
        }
        return I18n.l(locale, key.toString());
    }
}
