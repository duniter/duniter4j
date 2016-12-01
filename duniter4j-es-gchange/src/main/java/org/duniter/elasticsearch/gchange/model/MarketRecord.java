package org.duniter.elasticsearch.gchange.model;

import org.duniter.core.client.model.elasticsearch.Record;

/**
 * Created by blavenie on 01/12/16.
 */
public class MarketRecord extends Record{

    public static final String PROPERTY_TITLE="title";
    public static final String PROPERTY_DESCRIPTION="description";

    private String title;
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
