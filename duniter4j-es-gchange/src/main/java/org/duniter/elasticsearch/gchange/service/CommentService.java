package org.duniter.elasticsearch.gchange.service;

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


import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.MapUtils;
import org.duniter.core.client.model.elasticsearch.RecordComment;
import org.duniter.core.client.service.bma.WotRemoteService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.elasticsearch.exception.DocumentNotFoundException;
import org.duniter.elasticsearch.gchange.PluginSettings;
import org.duniter.elasticsearch.gchange.model.MarketRecord;
import org.duniter.elasticsearch.gchange.model.event.GchangeEventCodes;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.user.model.UserEvent;
import org.duniter.elasticsearch.user.service.UserEventService;
import org.duniter.elasticsearch.user.service.UserService;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.nuiton.i18n.I18n;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Benoit on 30/03/2015.
 */
public class CommentService extends AbstractService {

    private UserEventService userEventService;
    private UserService userService;

    @Inject
    public CommentService(Client client,
                          PluginSettings pluginSettings,
                          CryptoService cryptoService,
                          UserService userService,
                          UserEventService userEventService) {
        super("gchange.comment", client, pluginSettings, cryptoService);
        this.userEventService = userEventService;
        this.userService = userService;
    }


    public String indexCommentFromJson(final String index, final String recordType, final String type, final String json) {
        JsonNode commentObj = readAndVerifyIssuerSignature(json);
        String issuer = getMandatoryField(commentObj, RecordComment.PROPERTY_ISSUER).asText();

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Indexing a %s from issuer [%s]", type, issuer.substring(0, 8)));
        }
        String commentId = indexDocumentFromJson(index, type, json);

        // Notify record issuer
        notifyRecordIssuerForComment(index, recordType, commentObj, true, commentId);

        return commentId;
    }

    public void updateCommentFromJson(final String index, final String recordType, final String type, final String json, final String id) {
        JsonNode commentObj = readAndVerifyIssuerSignature(json);

        if (logger.isDebugEnabled()) {
            String issuer = getMandatoryField(commentObj, RecordComment.PROPERTY_ISSUER).asText();
            logger.debug(String.format("Indexing a %s from issuer [%s]", type, issuer.substring(0, 8)));
        }

        updateDocumentFromJson(index, type, id, json);

        // Notify record issuer
        notifyRecordIssuerForComment(index, recordType, commentObj, false, id);

    }

    public XContentBuilder createRecordCommentType(String index, String type) {
        String stringAnalyzer = pluginSettings.getDefaultStringAnalyzer();

        try {
            XContentBuilder mapping = XContentFactory.jsonBuilder().startObject().startObject(type)
                    .startObject("properties")

                    // issuer
                    .startObject("issuer")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // time
                    .startObject("time")
                    .field("type", "integer")
                    .endObject()

                    // message
                    .startObject("message")
                    .field("type", "string")
                    .field("analyzer", stringAnalyzer)
                    .endObject()

                    // record
                    .startObject("record")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    // reply to
                    .startObject("reply_to")
                    .field("type", "string")
                    .field("index", "not_analyzed")
                    .endObject()

                    .endObject()
                    .endObject().endObject();

            return mapping;
        }
        catch(IOException ioe) {
            throw new TechnicalException(String.format("Error while getting mapping for index [%s/%s]: %s", index, type, ioe.getMessage()), ioe);
        }
    }


    /* -- Internal methods -- */

    /**
     * Notify user when new comment
     */
    private void notifyRecordIssuerForComment(final String index, final String recordType, JsonNode actualObj, boolean isNewComment, String commentId) {
        String issuer = getMandatoryField(actualObj, RecordComment.PROPERTY_ISSUER).asText();

        // Notify issuer of record (is not same as comment writer)
        String recordId = getMandatoryField(actualObj, RecordComment.PROPERTY_RECORD).asText();
        Map<String, Object> recordFields = getFieldsById(index, recordType, recordId,
                MarketRecord.PROPERTY_TITLE, MarketRecord.PROPERTY_ISSUER);
        if (MapUtils.isEmpty(recordFields)) { // record not found
            throw new DocumentNotFoundException(I18n.t("duniter.market.error.comment.recordNotFound", recordId));
        }
        String recordIssuer = recordFields.get(MarketRecord.PROPERTY_ISSUER).toString();

        // Get user title
        String issuerTitle = userService.getProfileTitle(issuer);

        String recordTitle = recordFields.get(MarketRecord.PROPERTY_TITLE).toString();
        if (!issuer.equals(recordIssuer)) {
            userEventService.notifyUser(
                    UserEvent.newBuilder(UserEvent.EventType.INFO, GchangeEventCodes.NEW_COMMENT.name())
                    .setMessage(
                            isNewComment ? I18n.n("duniter.market.event.newComment") : I18n.n("duniter.market.event.updateComment"),
                            issuerTitle != null ? issuerTitle : issuer.substring(0, 8),
                            recordTitle)
                    .setRecipient(recordIssuer)
                    .setReference(index, recordType, recordId)
                    .setReferenceAnchor(commentId)
                    .build());
        }
    }

}
