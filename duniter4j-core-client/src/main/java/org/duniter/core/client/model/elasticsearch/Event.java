package org.duniter.core.client.model.elasticsearch;

import org.nuiton.i18n.I18n;

import java.util.Locale;

/**
 * Created by blavenie on 29/11/16.
 */
public class Event extends Record {

    public static final String PROPERTY_TYPE="type";
    public static final String PROPERTY_CODE="code";
    public static final String PROPERTY_MESSAGE="message";
    public static final String PROPERTY_PARAMS="params";

    private String type;

    private String code;

    private String message;

    private String[] params;

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public void setMessage(String message) {
        this.message = message;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }
}
