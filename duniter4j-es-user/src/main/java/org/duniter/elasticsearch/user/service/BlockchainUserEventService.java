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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Benoit on 30/03/2015.
 */
public class BlockchainUserEventService extends AbstractService implements ChangeService.ChangeListener {

    public final UserEventService userEventService;

    public final ObjectMapper objectMapper;

    public final List<ChangeSource> changeListenSources;

    public final Joiner simpleJoiner = Joiner.on(',');

    @Inject
    public BlockchainUserEventService(Client client, PluginSettings settings, CryptoService cryptoService,
                                      UserEventService userEventService) {
        super("duniter.user.event.blockchain", client, settings, cryptoService);
        this.userEventService = userEventService;
        this.objectMapper = JacksonUtils.newObjectMapper();
        this.changeListenSources = ImmutableList.of(new ChangeSource("*", BlockchainService.BLOCK_TYPE));
        ChangeService.registerListener(this);
    }

    @Override
    public String getId() {
        return "duniter.user.event.blockchain";
    }

    @Override
    public void onChange(ChangeEvent change) {
        if (change.getSource() == null) return;

        try {
            BlockchainBlock block = objectMapper.readValue(change.getSource().streamInput(), BlockchainBlock.class);

            switch (change.getOperation()) {
                case INDEX:
                    processBlockIndex(block);
                    break;

                // on DELETE : remove user event on block (using link
                case DELETE:
                    processBlockDelete(block);
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
        // TODO get profile name
        String sendersStr = simpleJoiner.join(senders);
        Set<String> receivers = new HashSet<>();
        for (String output : tx.getOutputs()) {
            String[] parts = output.split(":");
            if (parts.length >= 3 && parts[2].startsWith("SIG(")) {
                String receiver = parts[2].substring(4, parts[2].length() - 1);
                if (!senders.contains(receiver) && !receivers.contains(receiver)) {
                    notifyUserEvent(block, receiver, UserEventCodes.TX_RECEIVED, I18n.n("duniter.user.event.tx.received"), sendersStr);
                    receivers.add(receiver);
                }
            }
        }


        // Sent
        // TODO get profile name
        String receiverStr = simpleJoiner.join(receivers);
        for (String sender:senders) {
            notifyUserEvent(block, sender, UserEventCodes.TX_SENT, I18n.n("duniter.user.event.tx.sent"), receiverStr);
        }

        // TODO : index this TX in a special index ?
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

    private void processBlockDelete(BlockchainBlock block) {


        //userEventService.deleteUserEventByReference()
    }
}
