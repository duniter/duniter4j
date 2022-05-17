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
import org.duniter.core.client.model.bma.TxHistory;
import org.duniter.core.client.model.bma.TxSource;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.exception.InsufficientCreditException;

import java.util.Map;


public interface TransactionRemoteService extends Service {

    /**
     * Transfer TX to the given peer, or if null to currency's default peer
     * @param peer
     * @param wallet
     * @param destPubKey
     * @param amount
     * @param comment
     * @return
     * @throws InsufficientCreditException
     */
    String transfer(Peer peer, Wallet wallet, String destPubKey, long amount,
                    String comment) throws InsufficientCreditException;

    /**
     * Transfer TX to the given peer, or if null to currency's default peer
     * @param peer
     * @param wallet
     * @param mapPubkeyAmount
     * @param comment
     * @return
     * @throws InsufficientCreditException
     */
    String transfer(Peer peer, Wallet wallet, Map<String,Long> mapPubkeyAmount,
                    String comment) throws InsufficientCreditException;

    /**
     * Same, using the default currency's peer
     * @param wallet
     * @param destPubKey
     * @param amount
     * @param comment
     * @return
     * @throws InsufficientCreditException
     */
	String transfer(Wallet wallet, String destPubKey, long amount,
                    String comment) throws InsufficientCreditException;

    TxSource getSources(Peer peer, String pubKey);
	TxSource getSources(String currencyId, String pubKey);

    long getCreditOrZero(Peer peer, String pubKey);
    long getCreditOrZero(String currencyId, String pubKey);

    Long getCredit(Peer peer, String pubKey);
    Long getCredit(String currencyId, String pubKey);

    long computeCredit(TxSource.Source[] sources);

    TxHistory getTxHistory(Peer peer, String pubKey, long fromBlockNumber, long toBlockNumber);
    TxHistory getTxHistory(String currencyId, String pubKey, long fromBlockNumber, long toBlockNumber);
}
