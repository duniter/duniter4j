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
import org.duniter.core.client.model.bma.WotCertification;
import org.duniter.core.client.model.bma.WotLookup;
import org.duniter.core.client.model.local.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface WotRemoteService extends Service {

    List<Identity> findIdentities(Set<String> currenciesIds, String uidOrPubKey);

    WotLookup.Uid find(Peer peer, String uidOrPubKey);
    WotLookup.Uid find(String currencyId, String uidOrPubKey);

    void getRequirements(String currencyId, String pubKey);

    WotLookup.Uid findByUid(Peer peer, String uid);
    WotLookup.Uid findByUid(String currencyId, String uid);

    WotLookup.Uid findByUidAndPublicKey(Peer peer, String uid, String pubKey);
    WotLookup.Uid findByUidAndPublicKey(String currencyId, String uid, String pubKey);

    Identity getIdentity(Peer peer, String uid, String pubKey);
    Identity getIdentity(String currencyId, String uid, String pubKey);

    Identity getIdentity(Peer peer, String pubKey);
    Identity getIdentity(String currencyId, String pubKey);

    Collection<Certification> getCertifications(Peer peer, String uid, String pubkey, boolean isMember);
    Collection<Certification> getCertifications(String currencyId, String uid, String pubkey, boolean isMember);

    WotCertification getCertifiedBy(Peer peer, String uid);
    WotCertification getCertifiedBy(String currencyId, String uid);

    long countValidCertifiers(Peer peer, String pubkey);
    long countValidCertifiers(String currencyId, String pubkey);

    WotCertification getCertifiersOf(Peer peer, String uid);
    WotCertification getCertifiersOf(String currencyId, String uid);

    String getSignedIdentity(String currencyId, byte[] pubKey, byte[] secKey, String uid, String blockUid);

    Map<String, String> getMembersUids(Peer peer);
    List<Member> getMembers(Peer peer);
    List<Member> getMembers(String currencyId);

    void sendIdentity(Peer peer, String currency, byte[] pubKey, byte[] secKey, String uid, String blockUid);
    void sendIdentity(String currencyId, byte[] pubKey, byte[] secKey, String uid, String blockUid);

    String sendCertification(Wallet wallet, Identity identity);

    String sendCertification(Peer peer,
                             byte[] pubKey, byte[] secKey,
                             String userUid, String userPubKeyHash,
                             String userTimestamp, String userSignature);

    String sendCertification(String currencyId,
                             byte[] pubKey, byte[] secKey,
                             String userUid, String userPubKeyHash,
                             String userTimestamp, String userSignature);

}
