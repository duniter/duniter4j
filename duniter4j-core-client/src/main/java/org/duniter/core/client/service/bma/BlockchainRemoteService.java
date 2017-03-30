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
     * @param currencyId
     * @param useCache
     * @return
     */
    BlockchainParameters getParameters(long currencyId, boolean useCache);

    /**
     * get the blockchain parameters (currency parameters)
     *
     * @param currencyId
     * @return
     */
    BlockchainParameters getParameters(long currencyId);

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
     * @param currencyId
     * @param number
     * @return
     */
    BlockchainBlock getBlock(long currencyId, long number) throws BlockNotFoundException;

    /**
     * Retrieve the dividend of a block, by id (from 0 to current).
     * Usefull method to avoid to deserialize allOfToList the block
     *
     * @param currencyId
     * @param number
     * @return
     */
    Long getBlockDividend(long currencyId, long number) throws BlockNotFoundException;

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
    BlockchainBlock getCurrentBlock(long currencyId, boolean useCache);

    /**
     * Retrieve the current block
     *
     * @return
     */
    BlockchainBlock getCurrentBlock(long currencyId);

    /**
     * Retrieve the current block
     *
     * @param peer the peer to use for request
     * @return the last block
     */
    BlockchainBlock getCurrentBlock(Peer peer);

    /**
     * Retrieve the currency data, from peer
     *
     * @param peer
     * @return
     */
    Currency getCurrencyFromPeer(Peer peer);

    BlockchainParameters getBlockchainParametersFromPeer(Peer peer);

    /**
     * Retrieve the last emitted UD (or ud0 if not UD emitted yet)
     *
     * @param currencyId id of currency
     * @return
     */
    long getLastUD(long currencyId);

    /**
     * Retrieve the last emitted UD, from a peer (or ud0 if not UD emitted yet)
     *
     * @param currencyId id of currency
     * @return
     */
    long getLastUD(Peer peer);

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
    void loadMembership(long currencyId, Identity identity, boolean checkLookupForNonMember);


    BlockchainMemberships getMembershipByUid(long currencyId, String uid);

    BlockchainMemberships getMembershipByPublicKey(long currencyId, String pubkey);

    /**
     * Request to integrate the wot
     */
    void requestMembership(Wallet wallet);

    void requestMembership(Peer peer, String currency, byte[] pubKey, byte[] secKey, String uid, String membershipBlockUid, String selfBlockUid);

    BlockchainMemberships getMembershipByPubkeyOrUid(long currencyId, String uidOrPubkey);

    BlockchainMemberships getMembershipByPubkeyOrUid(Peer peer, String uidOrPubkey);

    String getMembership(Wallet wallet,
                                BlockchainBlock block,
                                boolean sideIn);

    /**
     * Get UD, by block number
     *
     * @param currencyId
     * @param startOffset
     * @return
     */
    Map<Integer, Long> getUDs(long currencyId, long startOffset);

    /**
     * Listening new block event
     * @param currencyId
     * @param listener
     * @param autoReconnect
     * @return
     */
    WebsocketClientEndpoint addBlockListener(long currencyId, WebsocketClientEndpoint.MessageListener listener, boolean autoReconnect);

    WebsocketClientEndpoint addBlockListener(Peer peer, WebsocketClientEndpoint.MessageListener listener, boolean autoReconnect);


}