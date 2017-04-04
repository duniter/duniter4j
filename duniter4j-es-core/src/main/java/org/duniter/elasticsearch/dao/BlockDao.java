package org.duniter.elasticsearch.dao;

import org.duniter.core.beans.Bean;
import org.duniter.core.client.model.bma.BlockchainBlock;

import java.util.List;

/**
 * Created by blavenie on 03/04/17.
 */
public interface BlockDao extends Bean, TypeDao<BlockDao> {

    void create(BlockchainBlock block, boolean wait);

    /**
     *
     * @param currencyName
     * @param number the block number
     * @param json block as JSON
     */
    void create(String currencyName, String id, byte[] json, boolean wait);

    boolean isExists(String currencyName, String id);

    void update(BlockchainBlock block, boolean wait);

    /**
     *
     * @param currencyName
     * @param number the block number, or -1 for current
     * @param json block as JSON
     */
    void update(String currencyName, String id, byte[] json, boolean wait);

    List<BlockchainBlock> findBlocksByHash(String currencyName, String query);

    int getMaxBlockNumber(String currencyName);

    BlockchainBlock getBlockById(String currencyName, String id);

    void deleteRange(final String currencyName, final int fromNumber, final int toNumber);
}
