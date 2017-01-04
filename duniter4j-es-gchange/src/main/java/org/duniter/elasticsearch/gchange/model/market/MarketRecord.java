package org.duniter.elasticsearch.gchange.model.market;

import org.duniter.core.client.model.elasticsearch.Record;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by blavenie on 01/12/16.
 */
public class MarketRecord extends Record{

    public static final String PROPERTY_TITLE="title";
    public static final String PROPERTY_DESCRIPTION="description";
    public static final String PROPERTY_PRICE="price";
    public static final String PROPERTY_UNIT="unit";
    public static final String PROPERTY_CURRENCY="currency";
    public static final String PROPERTY_THUMBNAIL="thumbnail";

    private String title;
    private String description;
    private Map<String, String> thumbnail = new HashMap<>();
    private Double price;
    private String unit;
    private String currency;

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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Map<String, String> getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Map<String, String> thumbnail) {
        this.thumbnail = thumbnail;
    }
}
