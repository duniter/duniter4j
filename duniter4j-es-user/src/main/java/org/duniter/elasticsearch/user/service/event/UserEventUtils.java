package org.duniter.elasticsearch.user.service.event;

import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.StringUtils;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by blavenie on 02/12/16.
 */
public abstract class UserEventUtils {

    public static String toJson(String issuer, String recipient, Locale locale, UserEvent event, String signature) {
        try {
            XContentBuilder eventObject = XContentFactory.jsonBuilder().startObject()
                    .field("type", event.getType().name())
                    .field("issuer", issuer) // TODO isuer = node pubkey
                    .field("recipient", recipient)
                    .field("time", event.getTime())
                    .field("code", event.getCode())
                    .field("message", event.getLocalizedMessage(locale));
            if (CollectionUtils.isNotEmpty(event.getParams())) {
                eventObject.array("params", event.getParams());
            }

            // Link
            UserEventLink link = event.getLink();
            if (link != null) {
                eventObject.startObject("link")
                        .field("index", link.getIndex())
                        .field("type", link.getType());
                if (StringUtils.isNotBlank(link.getId())) {
                    eventObject.field("id", link.getId());
                }
                eventObject.endObject();
            }

            if (StringUtils.isNotBlank(signature)) {
                eventObject.field("signature", signature);
            }
            eventObject.endObject();
            return eventObject.string();
        }
        catch(IOException e) {
            throw new TechnicalException(e);
        }

    }

    public static String toJson(Locale locale, UserEvent event) {
        try {
            XContentBuilder eventObject = XContentFactory.jsonBuilder().startObject()
                    .field("type", event.getType().name())
                    .field("time", event.getTime())
                    .field("code", event.getCode())
                    .field("message", event.getLocalizedMessage(locale));
            if (CollectionUtils.isNotEmpty(event.getParams())) {
                eventObject.array("params", event.getParams());
            }

            // Link
            UserEventLink link = event.getLink();
            if (link != null) {
                eventObject.startObject("link")
                        .field("index", link.getIndex())
                        .field("type", link.getType());
                if (StringUtils.isNotBlank(link.getId())) {
                    eventObject.field("id", link.getId());
                }
                eventObject.endObject();
            }
            eventObject.endObject();
            return eventObject.string();
        }
        catch(IOException e) {
            throw new TechnicalException(e);
        }

    }
}
