package org.duniter.elasticsearch.gchange.dao.market;

import org.duniter.elasticsearch.dao.IndexDao;

/**
 * Created by blavenie on 03/04/17.
 */
public interface MarketIndexDao extends IndexDao<MarketIndexDao> {
    String INDEX = "market";
    String CATEGORY_TYPE = "category";
}
