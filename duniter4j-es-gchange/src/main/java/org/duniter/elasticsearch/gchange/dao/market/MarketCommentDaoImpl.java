package org.duniter.elasticsearch.gchange.dao.market;

import org.duniter.core.exception.TechnicalException;
import org.duniter.elasticsearch.gchange.PluginSettings;
import org.duniter.elasticsearch.gchange.dao.AbstractCommentDaoImpl;
import org.duniter.elasticsearch.gchange.dao.AbstractRecordDaoImpl;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

/**
 * Created by blavenie on 03/04/17.
 */
public class MarketCommentDaoImpl extends AbstractCommentDaoImpl implements MarketCommentDao {

    @Inject
    public MarketCommentDaoImpl(PluginSettings pluginSettings) {
        super(MarketIndexDao.INDEX, pluginSettings);
    }
}
