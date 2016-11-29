package org.duniter.elasticsearch.service.event;

import org.nuiton.i18n.I18n;

import java.util.Locale;

/**
 * Created by blavenie on 29/11/16.
 */
public class Event {

    private EventType type;

    private String code;

    private long time;

    private String message;


    private String[] params;

    public Event(EventType type, String code) {
        this(type, code, null);
    }

    public Event(EventType type, String code, String[] params) {
        this.type = type;
        this.code = code;
        this.params = params;
        // default
        this.message = I18n.t("duniter4j.event." + code, params);
        this.time = Math.round(1d * System.currentTimeMillis() / 1000);
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public String getLocalizedMessage(Locale locale) {
        return I18n.l(locale, "duniter4j.event." + code, params);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    public long getTime() {
        return time;
    }

    public enum EventType {
        INFO,
        WARN,
        ERROR
    }
}
