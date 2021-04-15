package org.duniter.core.client.service.bma;

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

import org.duniter.core.beans.Service;
import org.duniter.core.client.model.bma.BlockchainDifficulties;
import org.duniter.core.client.model.local.Identity;
import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.client.model.bma.BlockchainMemberships;
import org.duniter.core.client.model.bma.BlockchainParameters;
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.exception.BlockNotFoundException;
import org.duniter.core.client.service.exception.PubkeyAlreadyUsedException;
import org.duniter.core.client.service.exception.UidAlreadyUsedException;
import org.duniter.core.client.service.exception.UidMatchAnotherPubkeyException;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;

import java.util.Map;

public interface BlockchainRemoteService extends Service {

    /**
     * get the blockchain parameters (currency parameters)
     *
     * @param currency
     * @param useCache
     * @return
     */
    BlockchainParameters getParameters(String currency, boolean useCache);

    BlockchainParameters getParameters(Peer peer, boolean useCache);

    /**
     * get the blockchain parameters (currency parameters)
     *
     * @param currency
     * @return
     */
    BlockchainParameters getParameters(String currency);

    /**
     * get the blockchain parameters (currency parameters)
     *
     * @param peer the peer to use for request
     * @return
     */
    BlockchainParameters getParameters(Peer peer);

    /**
     * Retrieve a block, by id (from 0 to current)
     *
     * @param currency
     * @param number
     * @return
     */
    BlockchainBlock getBlock(String currency, long number) throws BlockNotFoundException;

    /**
     * Retrieve the dividend of a block, by id (from 0 to current).
     * Usefull method to avoid to deserialize allOfToList the block
     *
     * @param currency
     * @param number
     * @return
     */
    Long getBlockDividend(String currency, long number) throws BlockNotFoundException;

    Long getBlockDividend(Peer peer, long number) throws BlockNotFoundException;

    /**
     * Retrieve a block, by id (from 0 to current)
     *
     * @param peer   the peer to use for request
     * @param number the block number
     * @return
     */
    BlockchainBlock getBlock(Peer peer, long number) throws BlockNotFoundException;

    /**
     * Get block with TX
     *
     * @param peer   the peer to use for request
     * @return
     */
    long[] getBlocksWithTx(Peer peer);

    /**
     * Retrieve a block, by id (from 0 to current) as JSON string
     *
     * @param peer   the peer to use for request
     * @param number the block number
     * @return
     */
    String getBlockAsJson(Peer peer, long number) throws BlockNotFoundException;

    /**
     * Retrieve a block, by id (from 0 to current) as JSON string
     *
     * @param peer   the peer to use for request
     * @param number the block number
     * @return
     */
    String[] getBlocksAsJson(Peer peer, int count, int from);

    /**
     * Retrieve the current block (with short cache)
     *
     * @return
     */
    BlockchainBlock getCurrentBlock(Peer peer, boolean useCache);
    BlockchainBlock getCurrentBlock(String currency, boolean useCache);

    /**
     * Retrieve the current block
     *
     * @return
     */
    BlockchainBlock getCurrentBlock(Peer peer);
    BlockchainBlock getCurrentBlock(String currency);

    /**
     * Retrieve the currency data, from peer
     *
     * @param peer
     * @return
     */
    Currency getCurrencyFromPeer(Peer peer);

    /**
     * Retrieve personal difficulties (level, uid)
     * @return
     */
    BlockchainDifficulties getDifficulties(Peer peer);
    BlockchainDifficulties getDifficulties(String currency);

    /**
     * Retrieve the last emitted UD (or ud0 if not UD emitted yet)
     *
     * @return
     */
    long getLastDividend(Peer peer);
    long getLastDividend(String currency);

    /**
     * Check is a identity is not already used by a existing member
     *
     * @param peer
     * @param identity
     * @throws UidAlreadyUsedException    if UID already used by another member
     * @throws PubkeyAlreadyUsedException if pubkey already used by another member
     */
    void checkNotMemberIdentity(Peer peer, Identity identity) throws UidAlreadyUsedException, PubkeyAlreadyUsedException;

    /**
     * Check is a wallet is a member, and load its attribute isMember and certTimestamp
     *
     * @param peer
     * @param wallet
     * @throws UidMatchAnotherPubkeyException is uid already used by another pubkey
     */
    void loadAndCheckMembership(Peer peer, Wallet wallet) throws UidMatchAnotherPubkeyException;

    /**
     * Load identity attribute isMember and timestamp
     *
     * @param identity
     */
    void loadMembership(String currency, Identity identity, boolean checkLookupForNonMember);

    BlockchainMemberships getMembershipByUid(String currency, String uid);

    BlockchainMemberships getMembershipByPublicKey(String currency, String pubkey);

    /**
     * Request to integrate the wot
     */
    void requestMembership(Wallet wallet);

    void requestMembership(Peer peer, String currency, byte[] pubKey, byte[] secKey, String uid, String membershipBlockUid, String selfBlockUid);

    BlockchainMemberships getMembershipByPubkeyOrUid(String currency, String uidOrPubkey);

    BlockchainMemberships getMembershipByPubkeyOrUid(Peer peer, String uidOrPubkey);

    String getMembership(Wallet wallet,
                                BlockchainBlock block,
                                boolean sideIn);

    /**
     * Get UD, by block number
     *
     * @param currency
     * @param startOffset
     * @return
     */
    Map<Integer, Long> getUDs(Peer peer, long startOffset);
    Map<Integer, Long> getUDs(String currency, long startOffset);

    /**
     * Listening new block event
     * @param listener
     * @param autoReconnect
     * @return
     */
    WebsocketClientEndpoint addBlockListener(Peer peer, WebsocketClientEndpoint.MessageListener listener, boolean autoReconnect);
    WebsocketClientEndpoint addBlockListener(String currency, WebsocketClientEndpoint.MessageListener listener, boolean autoReconnect);



}