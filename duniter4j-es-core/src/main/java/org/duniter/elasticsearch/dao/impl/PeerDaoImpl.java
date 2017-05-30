package org.duniter.elasticsearch.dao.impl;

/*
 * #%L
 * UCoin Java :: Core Client API
 * %%
 * Copyright (C) 2014 - 2016 EIS
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
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.elasticsearch.dao.AbstractDao;
import org.duniter.elasticsearch.dao.PeerDao;
import org.duniter.elasticsearch.dao.TypeDao;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by blavenie on 29/12/15.
 */
public class PeerDaoImpl extends AbstractDao implements PeerDao {

    public PeerDaoImpl(){
        super("duniter.dao.peer");
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Peer create(Peer peer) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getId()));
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getCurrency()));
        //Preconditions.checkNotNull(peer.getHash());
        Preconditions.checkNotNull(peer.getHost());
        Preconditions.checkNotNull(peer.getApi());

        // Serialize into JSON
        // WARN: must use GSON, to have same JSON result (e.g identities and joiners field must be converted into String)
        try {
            String json = objectMapper.writeValueAsString(peer);

            // Preparing indexBlocksFromNode
            IndexRequestBuilder indexRequest = client.prepareIndex(peer.getCurrency(), TYPE)
                    .setId(peer.getId())
                    .setSource(json);

            // Execute indexBlocksFromNode
            indexRequest
                    .setRefresh(true)
                    .execute();
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(e);
        }
        return peer;
    }

    @Override
    public Peer update(Peer peer) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getId()));
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getCurrency()));
        //Preconditions.checkNotNull(peer.getHash());
        Preconditions.checkNotNull(peer.getHost());
        Preconditions.checkNotNull(peer.getApi());

        // Serialize into JSON
        try {
            String json = objectMapper.writeValueAsString(peer);

            // Preparing indexBlocksFromNode
            UpdateRequestBuilder updateRequest = client.prepareUpdate(peer.getCurrency(), TYPE, peer.getId())
                    .setDoc(json);

            // Execute indexBlocksFromNode
            updateRequest
                    .setRefresh(true)
                    .execute();
        }
        catch(JsonProcessingException e) {
            throw new TechnicalException(e);
        }
        return peer;
    }

    @Override
    public Peer getById(String id) {
        throw new TechnicalException("not implemented");
    }

    @Override
    public void remove(Peer peer) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getId()));
        Preconditions.checkArgument(StringUtils.isNotBlank(peer.getCurrency()));

        // Delete the document
        client.prepareDelete(peer.getCurrency(), TYPE, peer.getId()).execute().actionGet();
    }

    @Override
    public List<Peer> getPeersByCurrencyId(String currencyId) {
        throw new TechnicalException("no implemented: loading all peers may be unsafe for memory...");
    }

    @Override
    public boolean isExists(String currencyId, String peerId) {
        return client.isDocumentExists(currencyId, TYPE, peerId);
    }

    @Override
    public XContentBuilder createTypeMapping() {
        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder()
                    .startObject()
                    .startObject(TYPE)
                    .startObject("properties")

                    // currency
                    .startObject("currency")
                    .field("type", "string")
                    .endObject()

                    // pubkey
                    .startObject("pubkey")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // api
                    .startObject("api")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // uid
                    .startObject("uid")
                    .field("type", "string")
                    .endObject()

                    // dns
                    .startObject("dns")
                    .field("type", "string")
                    .endObject()

                    // ipv4
                    .startObject("ipv4")
                    .field("type", "string")
                    .endObject()

                    // ipv6
                    .startObject("ipv6")
                    .field("type", "string")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException("Error while getting mapping for peer index: " + ioe.getMessage(), ioe);
        }
    }
}
