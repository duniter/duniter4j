package org.duniter.elasticsearch.gchange.model.registry;

import org.duniter.core.client.model.elasticsearch.Record;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by blavenie on 01/12/16.
 */
public class RegistryRecord extends Record{

    public static final String PROPERTY_TITLE="title";
    public static final String PROPERTY_DESCRIPTION="description";
    public static final String PROPERTY_THUMBNAIL="thumbnail";

    private String title;
    private String description;
    private Map<String, String> thumbnail = new HashMap<>();

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

    public Map<String, String> getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Map<String, String> thumbnail) {
        this.thumbnail = thumbnail;
    }

}
