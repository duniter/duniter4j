package org.duniter.elasticsearch.dao.impl;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.fasterxml.jackson.core.JsonProcessingException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.dao.AbstractDao;
import org.duniter.elasticsearch.dao.BlockStatDao;
import org.duniter.elasticsearch.model.BlockchainBlockStat;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

/**
 * Created by Benoit on 30/03/2015.
 */
public class BlockStatDaoImpl extends AbstractDao implements BlockStatDao {

    public BlockStatDaoImpl(){
        super("duniter.dao.blockStat");
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public void create(BlockchainBlockStat block, boolean wait) {
        Preconditions.checkNotNull(block);
        Preconditions.checkArgument(StringUtils.isNotBlank(block.getCurrency()));
        Preconditions.checkNotNull(block.getHash());
        Preconditions.checkNotNull(block.getNumber());

        // Serialize into JSON
        // WARN: must use GSON, to have same JSON result (e.g identities and joiners field must be converted into String)
        try {
            String json = objectMapper.writeValueAsString(block);

            // Preparing
            IndexRequestBuilder request = client.prepareIndex(block.getCurrency(), TYPE)
                    .setId(block.getNumber().toString())
                    .setSource(json);

            // Execute
            client.safeExecuteRequest(request, wait);
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public void create(String currencyName, String id, byte[] json, boolean wait) {
        Preconditions.checkNotNull(currencyName);
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(json);
        Preconditions.checkArgument(json.length > 0);

        // Preparing indexBlocksFromNode
        IndexRequestBuilder request = client.prepareIndex(currencyName, TYPE)
                .setId(id)
                .setRefresh(true)
                .setSource(json);

        // Execute
        client.safeExecuteRequest(request, wait);
    }

    public boolean isExists(String currencyName, String id) {
        return client.isDocumentExists(currencyName, TYPE, id);
    }

    public void update(BlockchainBlockStat block, boolean wait) {
        Preconditions.checkNotNull(block);
        Preconditions.checkArgument(StringUtils.isNotBlank(block.getCurrency()));
        Preconditions.checkNotNull(block.getNumber());

        // Serialize into JSON
        // WARN: must use GSON, to have same JSON result (e.g identities and joiners field must be converted into String)
        try {
            String json = objectMapper.writeValueAsString(block);

            // Preparing
            UpdateRequestBuilder request = client.prepareUpdate(block.getCurrency(), TYPE, block.getNumber().toString())
                    .setRefresh(true)
                    .setDoc(json);

            // Execute
            client.safeExecuteRequest(request, wait);
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(e);
        }
    }

    /**
     *
     * @param currencyName
     * @param id the block id
     * @param json block as JSON
     */
    public void update(String currencyName, String id, byte[] json, boolean wait) {
        Preconditions.checkNotNull(currencyName);
        Preconditions.checkNotNull(json);
        Preconditions.checkArgument(json.length > 0);

        // Preparing index
        UpdateRequestBuilder request = client.prepareUpdate(currencyName, TYPE, id)
                .setRefresh(true)
                .setDoc(json);

        // Execute
        client.safeExecuteRequest(request, wait);
    }

    @Override
    public void delete(String currency, String id, boolean wait) {
        Preconditions.checkNotNull(currency);
        Preconditions.checkNotNull(id);

        // Preparing request
        DeleteRequestBuilder request = client.prepareDelete(currency, TYPE, id);

        // Execute
        client.safeExecuteRequest(request, wait);
    }

    @Override
    public XContentBuilder createTypeMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder()
                    .startObject()
                    .startObject(TYPE)
                    .startObject("properties")

                    // currency
                    .startObject(BlockchainBlockStat.PROPERTY_CURRENCY)
                    .field("type", "string")
                    .endObject()

                    // version
                    .startObject(BlockchainBlockStat.PROPERTY_VERSION)
                    .field("type", "integer")
                    .endObject()

                    // block number
                    .startObject(BlockchainBlockStat.PROPERTY_NUMBER)
                    .field("type", "integer")
                    .endObject()

                    // medianTime
                    .startObject(BlockchainBlockStat.PROPERTY_MEDIAN_TIME)
                    .field("type", "long")
                    .endObject()

                    // hash
                    .startObject(BlockchainBlockStat.PROPERTY_HASH)
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // membersCount
                    .startObject(BlockchainBlockStat.PROPERTY_MEMBERS_COUNT)
                    .field("type", "integer")
                    .endObject()

                    // unitbase
                    .startObject(BlockchainBlockStat.PROPERTY_UNITBASE)
                    .field("type", "integer")
                    .endObject()

                    // monetaryMass
                    .startObject(BlockchainBlockStat.PROPERTY_MONETARY_MASS)
                    .field("type", "long")
                    .endObject()

                    // dividend
                    .startObject(BlockchainBlockStat.PROPERTY_DIVIDEND)
                    .field("type", "integer")
                    .endObject()

                    // --- STATS properties ---

                    // txCount
                    .startObject(BlockchainBlockStat.PROPERTY_TX_COUNT)
                    .field("type", "integer")
                    .endObject()

                    // txAmount
                    .startObject(BlockchainBlockStat.PROPERTY_TX_AMOUNT)
                    .field("type", "long")
                    .endObject()

                    // txChangeAmount
                    .startObject(BlockchainBlockStat.PROPERTY_TX_CHANGE_COUNT)
                    .field("type", "integer")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException("Error while getting mapping for block stat index: " + ioe.getMessage(), ioe);
        }
    }

    public BlockchainBlockStat getById(String currencyName, String id) {
        return client.getSourceById(currencyName, TYPE, id, BlockchainBlockStat.class);
    }

    /* -- Internal methods -- */

}
