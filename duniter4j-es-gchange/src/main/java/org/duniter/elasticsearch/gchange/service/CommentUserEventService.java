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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.MapUtils;
import org.duniter.core.client.model.ModelUtils;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.elasticsearch.RecordComment;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.gchange.model.market.MarketRecord;
import org.duniter.elasticsearch.gchange.model.event.GchangeEventCodes;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.BlockchainService;
import org.duniter.elasticsearch.service.changes.ChangeEvent;
import org.duniter.elasticsearch.service.changes.ChangeService;
import org.duniter.elasticsearch.service.changes.ChangeSource;
import org.duniter.elasticsearch.user.model.UserEvent;
import org.duniter.elasticsearch.user.model.UserEventCodes;
import org.duniter.elasticsearch.user.service.UserEventService;
import org.duniter.elasticsearch.user.service.UserService;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.nuiton.i18n.I18n;

import java.io.IOException;
import java.util.*;

/**
 * Created by Benoit on 30/03/2015.
 */
public class CommentUserEventService extends AbstractService implements ChangeService.ChangeListener {

    static {
        I18n.n("duniter.market.error.comment.recordNotFound");
        I18n.n("duniter.market.event.newComment");
        I18n.n("duniter.market.event.updateComment");
        I18n.n("duniter.market.event.newReplyComment");
        I18n.n("duniter.market.event.updateReplyComment");

        I18n.n("duniter.registry.error.comment.recordNotFound");
        I18n.n("duniter.registry.event.newComment");
        I18n.n("duniter.registry.event.updateComment");
        I18n.n("duniter.registry.event.newReplyComment");
        I18n.n("duniter.registry.event.updateReplyComment");
    }

    public final UserService userService;

    public final UserEventService userEventService;

    public final ObjectMapper objectMapper;

    public final List<ChangeSource> changeListenSources;

    public final boolean enable;

    public final String recordType;

    @Inject
    public CommentUserEventService(Client client, PluginSettings settings, CryptoService cryptoService,
                                   BlockchainService blockchainService,
                                   UserService userService,
                                   UserEventService userEventService) {
        super("duniter.user.event.comment", client, settings, cryptoService);
        this.userService = userService;
        this.userEventService = userEventService;
        this.objectMapper = JacksonUtils.newObjectMapper();
        this.changeListenSources = ImmutableList.of(
                new ChangeSource(MarketService.INDEX, MarketService.RECORD_COMMENT_TYPE),
                new ChangeSource(RegistryService.INDEX, RegistryService.RECORD_COMMENT_TYPE));
        ChangeService.registerListener(this);

        this.enable = pluginSettings.enableBlockchainSync();

        this.recordType = MarketService.RECORD_TYPE; // same as RegistryService.RECORD_TYPE

        if (this.enable) {
            blockchainService.registerConnectionListener(createConnectionListeners());
        }
    }

    @Override
    public String getId() {
        return "duniter.user.event.comment";
    }

    @Override
    public void onChange(ChangeEvent change) {


        try {
            switch (change.getOperation()) {
                case CREATE:
                    if (change.getSource() != null) {
                        RecordComment comment = objectMapper.readValue(change.getSource().streamInput(), RecordComment.class);
                        processCreateComment(change.getIndex(), change.getType(), change.getId(), comment);
                    }
                    break;
                case INDEX:
                    if (change.getSource() != null) {
                        RecordComment comment = objectMapper.readValue(change.getSource().streamInput(), RecordComment.class);
                        processUpdateComment(change.getIndex(), change.getType(), change.getId(), comment);
                    }
                    break;

                // on DELETE : remove user event on block (using link
                case DELETE:
                    processCommentDelete(change);

                    break;
            }

        }
        catch(IOException e) {
            throw new TechnicalException(String.format("Unable to parse received comment %s", change.getId()), e);
        }
    }

    @Override
    public Collection<ChangeSource> getChangeSources() {
        return changeListenSources;
    }

    /* -- internal method -- */

    /**
     * Create a listener that notify admin when the Duniter node connection is lost or retrieve
     */
    private WebsocketClientEndpoint.ConnectionListener createConnectionListeners() {
        return new WebsocketClientEndpoint.ConnectionListener() {
            private boolean errorNotified = false;

            @Override
            public void onSuccess() {
                // Send notify on reconnection
                if (errorNotified) {
                    errorNotified = false;
                    userEventService.notifyAdmin(UserEvent.newBuilder(UserEvent.EventType.INFO, UserEventCodes.NODE_BMA_UP.name())
                            .setMessage(I18n.n("duniter.event.NODE_BMA_UP"),
                                    pluginSettings.getNodeBmaHost(),
                                    String.valueOf(pluginSettings.getNodeBmaPort()),
                                    pluginSettings.getClusterName())
                            .build());
                }
            }

            @Override
            public void onError(Exception e, long lastTimeUp) {
                if (errorNotified) return; // already notify

                // Wait 1 min, then notify admin (once)
                long now = System.currentTimeMillis() / 1000;
                boolean wait = now - lastTimeUp < 60;
                if (!wait) {
                    errorNotified = true;
                    userEventService.notifyAdmin(UserEvent.newBuilder(UserEvent.EventType.ERROR, UserEventCodes.NODE_BMA_DOWN.name())
                            .setMessage(I18n.n("duniter.event.NODE_BMA_DOWN"),
                                    pluginSettings.getNodeBmaHost(),
                                    String.valueOf(pluginSettings.getNodeBmaPort()),
                                    pluginSettings.getClusterName(),
                                    String.valueOf(lastTimeUp))
                            .build());
                }
            }
        };
    }

    /**
     * Send notification from a new comment
     *
     * @param index
     * @param type
     * @param commentId
     * @param comment
     */
    private void processCreateComment(String index, String type, String commentId, RecordComment comment) {

        processUpdateOrCreateComment(index, type, commentId, comment,
                GchangeEventCodes.NEW_COMMENT,  String.format("duniter.%s.event.newComment", index.toLowerCase()),
                GchangeEventCodes.NEW_REPLY_COMMENT,  String.format("duniter.%s.event.newReplyComment", index.toLowerCase()));
    }

    /**
     * Same as processCreateComment(), but with other code and message.
     *
     * @param index
     * @param type
     * @param commentId
     * @param comment
     */
    private void processUpdateComment(String index, String type, String commentId, RecordComment comment) {

        processUpdateOrCreateComment(index, type, commentId, comment,
                GchangeEventCodes.UPDATE_COMMENT,  String.format("duniter.%s.event.updateComment", index.toLowerCase()),
                GchangeEventCodes.UPDATE_REPLY_COMMENT,  String.format("duniter.%s.event.updateReplyComment", index.toLowerCase()));
    }


    /**
     * Same as processCreateComment(), but with other code and message.
     *
     * @param index
     * @param type
     * @param commentId
     * @param comment
     */
    private void processUpdateOrCreateComment(String index, String type, String commentId, RecordComment comment,
                                              GchangeEventCodes eventCodeForRecordIssuer, String messageKeyForRecordIssuer,
                                              GchangeEventCodes eventCodeForParentCommentIssuer, String messageKeyForParentCommentIssuer) {
        // Get record issuer
        String recordId = comment.getRecord();
        Map<String, Object> record = getFieldsById(index, this.recordType, recordId,
                MarketRecord.PROPERTY_TITLE, MarketRecord.PROPERTY_ISSUER);

        // Record not found : nothing to emit
        if (MapUtils.isEmpty(record)) {
            logger.warn(I18n.t(String.format("duniter.%s.error.comment.recordNotFound", index.toLowerCase()), recordId));
            return;
        }

        // Fetch record info
        String recordIssuer = record.get(MarketRecord.PROPERTY_ISSUER).toString();
        String recordTitle = record.get(MarketRecord.PROPERTY_TITLE).toString();

        // Get comment issuer title
        String issuer = comment.getIssuer();
        String issuerTitle = userService.getProfileTitle(issuer);

        // Notify issuer of record (is not same as comment writer)
        if (!issuer.equals(recordIssuer)) {
            userEventService.notifyUser(
                    UserEvent.newBuilder(UserEvent.EventType.INFO, eventCodeForRecordIssuer.name())
                            .setMessage(
                                    messageKeyForRecordIssuer,
                                    issuer,
                                    issuerTitle != null ? issuerTitle : ModelUtils.minifyPubkey(issuer),
                                    recordTitle
                            )
                            .setRecipient(recordIssuer)
                            .setReference(index, recordType, recordId)
                            .setReferenceAnchor(commentId)
                            .setTime(comment.getTime())
                            .build());
        }

        // Notify comment is a reply to another comment
        if (StringUtils.isNotBlank(comment.getReplyTo())) {

            String parentCommentIssuer = getTypedFieldById(index, type, comment.getReplyTo(), RecordComment.PROPERTY_ISSUER);

            if (StringUtils.isNotBlank(parentCommentIssuer) &&
                    !issuer.equals(parentCommentIssuer) &&
                    !recordIssuer.equals(parentCommentIssuer)) {

                userEventService.notifyUser(
                        UserEvent.newBuilder(UserEvent.EventType.INFO, eventCodeForParentCommentIssuer.name())
                                .setMessage(
                                        messageKeyForParentCommentIssuer,
                                        issuer,
                                        issuerTitle != null ? issuerTitle : ModelUtils.minifyPubkey(issuer),
                                        recordTitle
                                )
                                .setRecipient(parentCommentIssuer)
                                .setReference(index, recordType, recordId)
                                .setReferenceAnchor(commentId)
                                /*.setTime(comment.getTime()) - DO NOT set time, has the comment time is NOT the update time*/
                                .build());
            }
        }

    }

    private void processCommentDelete(ChangeEvent change) {
        if (change.getId() == null) return;

        // Delete events that reference this block
        userEventService.deleteEventsByReference(new UserEvent.Reference(change.getIndex(), change.getType(), change.getId()));
    }


}
