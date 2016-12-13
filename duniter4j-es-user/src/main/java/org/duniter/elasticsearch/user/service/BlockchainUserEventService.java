package org.duniter.elasticsearch.user.service;

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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.MapUtils;
import org.duniter.core.client.model.ModelUtils;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;
import org.duniter.elasticsearch.PluginSettings;
import org.duniter.elasticsearch.service.AbstractService;
import org.duniter.elasticsearch.service.BlockchainService;
import org.duniter.elasticsearch.service.changes.ChangeEvent;
import org.duniter.elasticsearch.service.changes.ChangeService;
import org.duniter.elasticsearch.service.changes.ChangeSource;
import org.duniter.elasticsearch.user.model.UserEvent;
import org.duniter.elasticsearch.user.model.UserEventCodes;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.nuiton.i18n.I18n;

import java.io.IOException;
import java.util.*;

/**
 * Created by Benoit on 30/03/2015.
 */
public class BlockchainUserEventService extends AbstractService implements ChangeService.ChangeListener {

    public static final String DEFAULT_PUBKEYS_SEPARATOR = ", ";

    public final UserService userService;

    public final UserEventService userEventService;

    public final ObjectMapper objectMapper;

    public final List<ChangeSource> changeListenSources;

    public final boolean enable;

    @Inject
    public BlockchainUserEventService(Client client, PluginSettings settings, CryptoService cryptoService,
                                      BlockchainService blockchainService,
                                      UserService userService,
                                      UserEventService userEventService) {
        super("duniter.user.event.blockchain", client, settings, cryptoService);
        this.userService = userService;
        this.userEventService = userEventService;
        this.objectMapper = JacksonUtils.newObjectMapper();
        this.changeListenSources = ImmutableList.of(new ChangeSource("*", BlockchainService.BLOCK_TYPE));
        ChangeService.registerListener(this);

        this.enable = pluginSettings.enableBlockchainSync();

        if (this.enable) {
            blockchainService.registerConnectionListener(createConnectionListeners());
        }
    }

    @Override
    public String getId() {
        return "duniter.user.event.blockchain";
    }

    @Override
    public void onChange(ChangeEvent change) {


        try {


            switch (change.getOperation()) {
                case INDEX:
                    if (change.getSource() != null) {
                        BlockchainBlock block = objectMapper.readValue(change.getSource().streamInput(), BlockchainBlock.class);
                        processBlockIndex(block);
                    }
                    break;

                // on DELETE : remove user event on block (using link
                case DELETE:
                    processBlockDelete(change);

                    break;
            }

        }
        catch(IOException e) {
            throw new TechnicalException(String.format("Unable to parse received block %s", change.getId()), e);
        }

        //logger.info("receiveing block change: " + change.toJson());
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

    private void processBlockIndex(BlockchainBlock block) {
        // Joiners
        if (CollectionUtils.isNotEmpty(block.getJoiners())) {
            for (BlockchainBlock.Joiner joiner: block.getJoiners()) {
                notifyUserEvent(block, joiner.getPublicKey(), UserEventCodes.MEMBER_JOIN, I18n.n("duniter.user.event.ms.join"), block.getCurrency());
            }
        }

        // Leavers
        if (CollectionUtils.isNotEmpty(block.getLeavers())) {
            for (BlockchainBlock.Joiner leaver: block.getJoiners()) {
                notifyUserEvent(block, leaver.getPublicKey(), UserEventCodes.MEMBER_LEAVE, I18n.n("duniter.user.event.ms.leave"), block.getCurrency());
            }
        }

        // Actives
        if (CollectionUtils.isNotEmpty(block.getActives())) {
            for (BlockchainBlock.Joiner active: block.getActives()) {
                notifyUserEvent(block, active.getPublicKey(), UserEventCodes.MEMBER_ACTIVE, I18n.n("duniter.user.event.ms.active"), block.getCurrency());
            }
        }

        // Tx
        if (CollectionUtils.isNotEmpty(block.getTransactions())) {
            for (BlockchainBlock.Transaction tx: block.getTransactions()) {
                processTx(block, tx);
            }
        }
    }

    private void processTx(BlockchainBlock block, BlockchainBlock.Transaction tx) {
        Set<String> senders = ImmutableSet.copyOf(tx.getIssuers());

        // Received
        String sendersString = joinPubkeys(senders, true);
        Set<String> receivers = new HashSet<>();
        for (String output : tx.getOutputs()) {
            String[] parts = output.split(":");
            if (parts.length >= 3 && parts[2].startsWith("SIG(")) {
                String receiver = parts[2].substring(4, parts[2].length() - 1);
                if (!senders.contains(receiver) && !receivers.contains(receiver)) {
                    notifyUserEvent(block, receiver, UserEventCodes.TX_RECEIVED, I18n.n("duniter.user.event.tx.received"), sendersString);
                    receivers.add(receiver);
                }
            }
        }

        // Sent
        if (CollectionUtils.isNotEmpty(receivers)) {
            String receiverStr = joinPubkeys(receivers, true);
            for (String sender : senders) {
                notifyUserEvent(block, sender, UserEventCodes.TX_SENT, I18n.n("duniter.user.event.tx.sent"), receiverStr);
            }
        }

        // TODO : indexer la TX dans un index/type spécifique ?
    }

    private void notifyUserEvent(BlockchainBlock block, String pubkey, UserEventCodes code, String message, String... params) {
        UserEvent event = UserEvent.newBuilder(UserEvent.EventType.INFO, code.name())
                .setRecipient(pubkey)
                .setMessage(message, params)
                .setTime(block.getMedianTime())
                .setReference(block.getCurrency(), BlockchainService.BLOCK_TYPE, String.valueOf(block.getNumber()))
                .setReferenceHash(block.getHash())
                .build();

        userEventService.notifyUser(event);
    }

    private void processBlockDelete(ChangeEvent change) {
        if (change.getId() == null) return;

        // Delete events that reference this block
        userEventService.deleteEventsByReference(new UserEvent.Reference(change.getIndex(), change.getType(), change.getId()));
    }

    private String joinPubkeys(Set<String> pubkeys, boolean minify) {
        Preconditions.checkNotNull(pubkeys);
        Preconditions.checkArgument(pubkeys.size()>0);
        if (pubkeys.size() == 1) {
            String pubkey = pubkeys.iterator().next();
            String title = userService.getProfileTitle(pubkey);
            return title != null ? title :
                    (minify ? ModelUtils.minifyPubkey(pubkey) : pubkey);
        }

        Map<String, String> profileTitles = userService.getProfileTitles(pubkeys);
        StringBuilder sb = new StringBuilder();
        pubkeys.stream().forEach((pubkey)-> {
            String title = profileTitles != null ? profileTitles.get(pubkey) : null;
            sb.append(DEFAULT_PUBKEYS_SEPARATOR);
            sb.append(title != null ? title :
                    (minify ? ModelUtils.minifyPubkey(pubkey) : pubkey));
        });

        return sb.substring(DEFAULT_PUBKEYS_SEPARATOR.length());
    }
}
