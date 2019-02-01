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

import com.google.common.collect.Maps;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.*;
import org.duniter.core.client.model.local.Identity;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.exception.*;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.cache.Cache;
import org.duniter.core.util.cache.SimpleCache;
import org.duniter.core.util.crypto.CryptoUtils;
import org.duniter.core.util.json.JsonArrayParser;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockchainRemoteServiceImpl extends BaseRemoteServiceImpl implements BlockchainRemoteService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainRemoteServiceImpl.class);

    private static final String JSON_DIVIDEND_ATTR = "\"dividend\":";

    public static final String URL_BASE = "/blockchain";

    public static final String URL_PARAMETERS = URL_BASE + "/parameters";

    public static final String URL_BLOCK = URL_BASE + "/block/%s";


    public static final String URL_BLOCKS_FROM = URL_BASE + "/blocks/%s/%s";

    public static final String URL_BLOCK_CURRENT = URL_BASE + "/current";

    public static final String URL_BLOCK_WITH_TX = URL_BASE + "/with/tx";

    public static final String URL_BLOCK_WITH_UD = URL_BASE + "/with/ud";

    public static final String URL_MEMBERSHIP = URL_BASE + "/membership";

    public static final String URL_MEMBERSHIP_SEARCH = URL_BASE + "/memberships/%s";

    public static final String URL_DIFFICULTIES = URL_BASE + "/difficulties";

    public static final String URL_WS_BLOCK = "/ws/block";

    private Configuration config;

    // Cache need for wallet refresh : iteration on wallet should not
    // execute a download of the current block
    private Cache<String, BlockchainBlock> mCurrentBlockCache;

    // Cache on blockchain parameters
    private Cache<String, BlockchainParameters> mParametersCache;

    private Map<URI, WebsocketClientEndpoint> wsEndPoints = new HashMap<>();

    public BlockchainRemoteServiceImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        config = Configuration.instance();

        // Initialize caches
        initCaches();
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (wsEndPoints.size() != 0) {
            for (WebsocketClientEndpoint clientEndPoint: wsEndPoints.values()) {
                clientEndPoint.close();
            }
            wsEndPoints.clear();
        }
    }

    @Override
    public BlockchainParameters getParameters(Peer peer, boolean useCache) {
        if (!useCache || peer.getCurrency() == null) {
            return getParameters(peer);
        } else {
            BlockchainParameters result = mParametersCache.getIfPresent(peer.getCurrency());
            if (result == null) {
                result = getParameters(peer);
                if (result != null) {
                    mParametersCache.put(peer.getCurrency(), result);
                }
            }
            return result;
        }
    }

    @Override
    public BlockchainParameters getParameters(String currencyId, boolean useCache) {
        return getParameters(peerService.getActivePeerByCurrencyId(currencyId), useCache);
    }

    @Override
    public BlockchainParameters getParameters(Peer peer) {
        return httpService.executeRequest(peer, URL_PARAMETERS, BlockchainParameters.class);
    }

    @Override
    public BlockchainParameters getParameters(String currencyId) {
        return getParameters(peerService.getActivePeerByCurrencyId(currencyId));
    }

    @Override
    public BlockchainBlock getBlock(Peer peer, long number) throws BlockNotFoundException {
        try {
            return httpService.executeRequest(peer, String.format(URL_BLOCK, number), BlockchainBlock.class);
        }
        catch(HttpNotFoundException e) {
            throw new BlockNotFoundException(String.format("Block #%s not found on peer [%s]", number, peer));
        }
    }

    @Override
    public BlockchainBlock getBlock(String currencyId, long number) throws BlockNotFoundException  {
       return getBlock(peerService.getActivePeerByCurrencyId(currencyId), number);
    }

    @Override
    public Long getBlockDividend(Peer peer, long number) throws BlockNotFoundException {
        String path = String.format(URL_BLOCK, number);
        try {
            String json = httpService.executeRequest(peer, path, String.class);
            return getDividendFromBlockJson(json);
        }
        catch(HttpNotFoundException e) {
            throw new BlockNotFoundException(String.format("Block #%s not found", number));
        }
    }

    @Override
    public Long getBlockDividend(String currencyId, long number) throws BlockNotFoundException {
        return getBlockDividend(peerService.getActivePeerByCurrencyId(currencyId), number);
    }



    @Override
    public long[] getBlocksWithTx(Peer peer) {
        try {
            Blocks blocks = httpService.executeRequest(peer, URL_BLOCK_WITH_TX, Blocks.class);
            return (blocks == null || blocks.getResult() == null) ? new long[0] : blocks.getResult().getBlocks();
        }
        catch(HttpNotFoundException e) {
            throw new TechnicalException(String.format("Error while getting blocks with TX on peer [%s]", peer));
        }
    }


    @Override
    public String getBlockAsJson(Peer peer, long number) {
        // get blockchain parameter
        String path = String.format(URL_BLOCK, number);
        try {
            return httpService.executeRequest(peer, path, String.class);
        }
        catch(HttpNotFoundException e) {
            throw new BlockNotFoundException(String.format("Block #%s not found on peer [%s]", number, peer));
        }
    }

    @Override
    public String[] getBlocksAsJson(Peer peer, int count, int from) {
        // get blockchain parameter
        String path = String.format(URL_BLOCKS_FROM, count, from);
        String jsonBlocksStr = httpService.executeRequest(peer, path, String.class);

        // Parse only array content, but deserialize array item
        JsonArrayParser parser = new JsonArrayParser();
        return parser.getValuesAsArray(jsonBlocksStr);
    }

    /**
     * Retrieve the current block (with short cache)
     *
     * @return
     */
    @Override
    public BlockchainBlock getCurrentBlock(Peer peer, boolean useCache) {
        if (!useCache || peer.getCurrency() == null) {
            return getCurrentBlock(peer);
        } else {
            BlockchainBlock result = mCurrentBlockCache.getIfPresent(peer.getCurrency());
            if (result == null) {
                result = getCurrentBlock(peer);
                if (result != null) {
                    mCurrentBlockCache.put(peer.getCurrency(), result);
                }
            }
            return result;
        }
    }

    public BlockchainBlock getCurrentBlock(String currencyId, boolean useCache) {
        return getCurrentBlock(peerService.getActivePeerByCurrencyId(currencyId), useCache);
    }

    @Override
    public BlockchainBlock getCurrentBlock(String currencyId) {
        return getCurrentBlock(peerService.getActivePeerByCurrencyId(currencyId));
    }

    @Override
    public BlockchainBlock getCurrentBlock(Peer peer) {
        // get blockchain parameter
        BlockchainBlock result = httpService.executeRequest(peer, URL_BLOCK_CURRENT, BlockchainBlock.class);
        return result;
    }

    @Override
    public org.duniter.core.client.model.local.Currency getCurrencyFromPeer(Peer peer) {
        BlockchainParameters parameter = getParameters(peer);
        BlockchainBlock firstBlock = getBlock(peer, 0);
        BlockchainBlock lastBlock = getCurrentBlock(peer);

        org.duniter.core.client.model.local.Currency result = new org.duniter.core.client.model.local.Currency();
        result.setId(parameter.getCurrency());
        result.setFirstBlockSignature(firstBlock.getSignature());
        result.setMembersCount(lastBlock.getMembersCount());
        result.setLastUD(parameter.getUd0());

        return result;
    }

    @Override
    public long getLastUD(Peer peer) {
        // get block number with UD
        String blocksWithUdResponse = httpService.executeRequest(peer, URL_BLOCK_WITH_UD, String.class);

        int[] blocksWithUD = getBlockNumbersFromJson(blocksWithUdResponse);

        // If no result (this could happen when no UD has been send
        if (blocksWithUD != null && blocksWithUD.length > 0) {

            int index = blocksWithUD.length - 1;
            while (index >= 0) {

                try {
                    // Get the UD from the last block with UD
                    String path = String.format(URL_BLOCK, blocksWithUD[index]);
                    String json = httpService.executeRequest(peer, path, String.class);
                    Long lastUD = getDividendFromBlockJson(json);

                    // Check not null (should never append)
                    if (lastUD == null) {
                        throw new TechnicalException("Unable to get last UD from server");
                    }
                    return lastUD.longValue();
                } catch (HttpNotFoundException e) {
                    index--; // Can occur something (observed in Duniter 0.50.0)
                }
            }
        }

        // get the first UD from currency parameter
        BlockchainParameters parameter = getParameters(peer);
        return parameter.getUd0();
    }

    @Override
    public long getLastUD(String currencyId) {
        return getLastUD(peerService.getActivePeerByCurrencyId(currencyId));
    }

    /**
     * Check is a identity is not already used by a existing member
     *
     * @param peer
     * @param identity
     * @throws UidAlreadyUsedException    if UID already used by another member
     * @throws PubkeyAlreadyUsedException if pubkey already used by another member
     */
    public void checkNotMemberIdentity(Peer peer, Identity identity) throws UidAlreadyUsedException, PubkeyAlreadyUsedException {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(identity);
        Preconditions.checkArgument(StringUtils.isNotBlank(identity.getUid()));
        Preconditions.checkArgument(StringUtils.isNotBlank(identity.getPubkey()));

        // Read membership data from the UID
        BlockchainMemberships result = getMembershipByPubkeyOrUid(peer, identity.getUid());

        // uid already used by another member
        if (result != null) {
            throw new UidAlreadyUsedException(String.format("User identifier '%s' is already used by another member", identity.getUid()));
        }

        result = getMembershipByPubkeyOrUid(peer, identity.getPubkey());

        // pubkey already used by another member
        if (result != null) {
            throw new PubkeyAlreadyUsedException(String.format("Pubkey key '%s' is already used by another member", identity.getPubkey()));
        }
    }

    /**
     * Check is a wallet is a member, and load its attribute isMember and certTimestamp
     *
     * @param wallet
     * @throws UidMatchAnotherPubkeyException is uid already used by another pubkey
     */
    public void loadAndCheckMembership(Peer peer, Wallet wallet) throws UidMatchAnotherPubkeyException {
        Preconditions.checkNotNull(wallet);

        // Load membership data
        loadMembership(null, peer, wallet.getIdentity(), true);

        // Something wrong on pubkey : uid already used by another pubkey !
        if (wallet.getIdentity().getIsMember() == null) {
            throw new UidMatchAnotherPubkeyException(wallet.getPubKeyHash());
        }
    }

    /**
     * Load identity attribute isMember and timestamp
     *
     * @param identity
     */
    public void loadMembership(String currencyId, Identity identity, boolean checkLookupForNonMember) {
        loadMembership(currencyId, null, identity, checkLookupForNonMember);
    }


    public BlockchainMemberships getMembershipByUid(String currencyId, String uid) {
        Preconditions.checkArgument(StringUtils.isNotBlank(uid));

        BlockchainMemberships result = getMembershipByPubkeyOrUid(currencyId, uid);
        if (result == null || !uid.equals(result.getUid())) {
            return null;
        }
        return result;
    }

    public BlockchainMemberships getMembershipByPublicKey(String currencyId, String pubkey) {
        Preconditions.checkArgument(StringUtils.isNotBlank(pubkey));

        BlockchainMemberships result = getMembershipByPubkeyOrUid(currencyId, pubkey);
        if (result == null || !pubkey.equals(result.getPubkey())) {
            return null;
        }
        return result;
    }

    /**
     * Request to integrate the wot
     */
    public void requestMembership(Wallet wallet) {
        Preconditions.checkNotNull(wallet);
        Preconditions.checkNotNull(wallet.getCurrencyId());
        Preconditions.checkNotNull(wallet.getCertTimestamp());

        BlockchainBlock block = getCurrentBlock(wallet.getCurrencyId());

        // Compute membership document
        String membership = getMembership(wallet,
                block,
                true /*side in*/);

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Will send membership document: \n------\n%s------",
                    membership));
        }

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("membership", membership));

        HttpPost httpPost = new HttpPost(httpService.getPath(wallet.getCurrencyId(), URL_MEMBERSHIP));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));
        } catch (UnsupportedEncodingException e) {
            throw new TechnicalException(e);
        }

        String membershipResult = httpService.executeRequest(httpPost, String.class);
        if (log.isDebugEnabled()) {
            log.debug("received from /tx/process: " + membershipResult);
        }
    }


    public void requestMembership(Peer peer, String currency, byte[] pubKey, byte[] secKey, String uid, String membershipBlockUid, String selfBlockUid) {
        // http post /blockchain/membership
        HttpPost httpPost = new HttpPost(httpService.getPath(peer, URL_MEMBERSHIP));

        // compute the self-certification
        String membership = getSignedMembership(currency, pubKey, secKey, uid, membershipBlockUid, selfBlockUid, true/*side in*/);

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("membership", membership));

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(urlParameters));
        }
        catch(UnsupportedEncodingException e) {
            throw new TechnicalException(e);
        }

        // Execute the request
        httpService.executeRequest(httpPost, String.class);
    }

    public BlockchainMemberships getMembershipByPubkeyOrUid(Peer peer, String uidOrPubkey) {
        try {
            return httpService.executeRequest(peer, String.format(URL_MEMBERSHIP_SEARCH, uidOrPubkey), BlockchainMemberships.class);
        } catch (HttpBadRequestException e) {
            log.debug("No member matching this pubkey or uid: " + uidOrPubkey);
            return null;
        }
    }

    public BlockchainMemberships getMembershipByPubkeyOrUid(String currencyId, String uidOrPubkey) {
        return getMembershipByPubkeyOrUid(peerService.getActivePeerByCurrencyId(currencyId), uidOrPubkey);
    }

    public String getMembership(Wallet wallet,
                                BlockchainBlock block,
                                boolean sideIn
    ) {

        // Create the member ship document
        String membership = getUnsignedMembership( wallet.getCurrency(),
                wallet.getPubKeyHash(),
                wallet.getUid(),
                block.getNumber() + '-' + block.getHash(),
                wallet.getCertTimestamp(),
                sideIn
        );

        // Add signature
        CryptoService cryptoService = ServiceLocator.instance().getCryptoService();
        String signature = cryptoService.sign(membership, wallet.getSecKey());

        return new StringBuilder().append(membership).append(signature)
                .append('\n').toString();
    }

    /**
     * Get UD, by block number
     *
     * @param peer
     * @param startOffset
     * @return
     */
    public Map<Integer, Long> getUDs(Peer peer, long startOffset) {
        log.debug(String.format("Getting block's UD from block [%s]", startOffset));

        int[] blockNumbersWithUD = getBlocksWithUD(peer);

        Map<Integer, Long> result = Maps.newLinkedHashMap();

        boolean previousBlockInsert = false;
        if (blockNumbersWithUD != null && blockNumbersWithUD.length != 0) {
            Integer previousBlockNumberWithUd = null;
            for (Integer blockNumber : blockNumbersWithUD) {
                if (blockNumber >= startOffset) {
                    if(!previousBlockInsert){
                        Long previousUd = getParameters(peer, true/*with cache*/).getUd0();
                        Integer previousBlockNumber = 0;
                        if(previousBlockNumberWithUd!=null){
                            previousUd = getBlockDividend(peer, previousBlockNumberWithUd);
                            if (previousUd == null) {
                                throw new TechnicalException(
                                        String.format("Unable to get UD from server block [%s]",
                                                previousBlockNumberWithUd)
                                );
                            }
                            previousBlockNumber = previousBlockNumberWithUd;
                        }
                        result.put(previousBlockNumber, previousUd);
                        previousBlockInsert = true;
                    }
                    Long ud = getBlockDividend(peer, blockNumber);
                    // Check not null (should never append)
                    if (ud == null) {
                        throw new TechnicalException(String.format("Unable to get UD from server block [%s]", blockNumber));
                    }
                    result.put(blockNumber, ud);
                }else{
                    previousBlockNumberWithUd = blockNumber;
                }
            }
        }else{
            result.put(0, getParameters(peer, true/*with cache*/).getUd0());
        }

        return result;
    }

    /**
     * Get UD, by block number
     *
     * @param currencyId
     * @param startOffset
     * @return
     */
    public Map<Integer, Long> getUDs(String currencyId, long startOffset) {
        return getUDs(peerService.getActivePeerByCurrencyId(currencyId), startOffset);
    }

    @Override
    public WebsocketClientEndpoint addBlockListener(Peer peer, WebsocketClientEndpoint.MessageListener listener, boolean autoReconnect) {
        Preconditions.checkNotNull(peer);
        Preconditions.checkNotNull(listener);

        // Get (or create) the websocket endpoint
        WebsocketClientEndpoint wsClientEndPoint = getWebsocketClientEndpoint(peer, URL_WS_BLOCK, autoReconnect);

        // add listener
        wsClientEndPoint.registerListener(listener);

        return wsClientEndPoint;
    }

    @Override
    public WebsocketClientEndpoint addBlockListener(String currencyId, WebsocketClientEndpoint.MessageListener listener, boolean autoReconnect) {
        return addBlockListener(peerService.getActivePeerByCurrencyId(currencyId), listener, autoReconnect);
    }

    @Override
    public BlockchainDifficulties getDifficulties(Peer peer) {
        return httpService.executeRequest(peer, URL_DIFFICULTIES, BlockchainDifficulties.class, config.getNetworkLargerTimeout());
    }

    @Override
    public BlockchainDifficulties getDifficulties(String currencyId) {
        return getDifficulties(peerService.getActivePeerByCurrencyId(currencyId));
    }


    /* -- Internal methods -- */

    /**
     * Initialize caches
     */
    protected void initCaches() {
        int cacheTimeInMillis = config.getNetworkCacheTimeInMillis();

        mCurrentBlockCache = new SimpleCache<String, BlockchainBlock>(cacheTimeInMillis) {
            @Override
            public BlockchainBlock load(String currencyId) {
                return getCurrentBlock(currencyId);
            }
        };

        mParametersCache = new SimpleCache<String, BlockchainParameters>(/*eternal cache*/) {
            @Override
            public BlockchainParameters load(String currencyId) {
                return getParameters(currencyId);
            }
        };
    }


    protected void loadMembership(String currencyId, Peer peer, Identity identity, boolean checkLookupForNonMember) {
        Preconditions.checkNotNull(identity);
        Preconditions.checkArgument(StringUtils.isNotBlank(identity.getUid()));
        Preconditions.checkArgument(StringUtils.isNotBlank(identity.getPubkey()));
        Preconditions.checkArgument(peer != null || currencyId != null);

        // Read membership data from the UID
        BlockchainMemberships result = peer != null
                ? getMembershipByPubkeyOrUid(peer, identity.getUid())
                : getMembershipByPubkeyOrUid(currencyId, identity.getUid());

        // uid not used = not was member
        if (result == null) {
            identity.setMember(false);

            if (checkLookupForNonMember) {
                WotRemoteService wotService = ServiceLocator.instance().getWotRemoteService();
                Identity lookupIdentity = peer != null
                        ? wotService.getIdentity(peer, identity.getUid(), identity.getPubkey())
                        : wotService.getIdentity(currencyId, identity.getUid(), identity.getPubkey());

                // Self certification exists, update the cert timestamp
                if (lookupIdentity != null) {
                    identity.setTimestamp(lookupIdentity.getTimestamp());
                }

                // Self certification not exists: make sure the cert time is cleaning
                else {
                    identity.setTimestamp(null);
                }
            }
        }

        // UID and pubkey is a member: fine
        else if (identity.getPubkey().equals(result.getPubkey())) {
            identity.setMember(true);
            //FIXME identity.setTimestamp(result.getSigDate());
        }

        // Something wrong on pubkey : uid already used by anither pubkey !
        else {
            identity.setMember(null);
        }

    }

    private int[] getBlocksWithUD(Peer peer) {
        log.debug("Getting blocks with UD");

        String json = httpService.executeRequest(peer, URL_BLOCK_WITH_UD, String.class);

        int startIndex = json.indexOf("[");
        int endIndex = json.lastIndexOf(']');

        if (startIndex == -1 || endIndex == -1) {
            return null;
        }

        String blockNumbersStr = json.substring(startIndex + 1, endIndex).trim();

        if (StringUtils.isBlank(blockNumbersStr)) {
            return null;
        }


        String[] blockNumbers = blockNumbersStr.split(",");
        int[] result = new int[blockNumbers.length];
        try {
            int i=0;
            for (String blockNumber : blockNumbers) {
                result[i++] = Integer.parseInt(blockNumber.trim());
            }
        }
        catch(NumberFormatException e){
            if (log.isDebugEnabled()) {
                log.debug(String.format("Bad format of the response '%s'.", URL_BLOCK_WITH_UD));
            }
            throw new TechnicalException("Unable to read block with UD numbers: " + e.getMessage(), e);
        }

        return result;
    }

    private int[] getBlocksWithUD(String currencyId) {
        Peer peer = peerService.getActivePeerByCurrencyId(currencyId);
        return getBlocksWithUD(peer);
    }

    protected String getSignedMembership(String currency,
                                      byte[] pubKey,
                                      byte[] secKey,
                                      String userId,
                                      String membershipBlockUid,
                                      String selfBlockUid,
                                      boolean sideIn) {
        // Compute the pub key hash
        String pubKeyHash = CryptoUtils.encodeBase58(pubKey);

        // Create the member ship document
        String membership = getUnsignedMembership(currency,
                pubKeyHash,
                userId,
                membershipBlockUid,
                selfBlockUid,
                sideIn
        );

        // Add signature
        CryptoService cryptoService = ServiceLocator.instance().getCryptoService();
        String signature = cryptoService.sign(membership, secKey);

        return new StringBuilder().append(membership).append(signature)
                .append('\n').toString();
    }

    protected String getUnsignedMembership(String currency,
                                           String pubkey,
                                           String userId,
                                           String membershipBlockUid,
                                           String selfBlockUid,
                                           boolean sideIn
    ) {
        // see https://github.com/ucoin-io/ucoin/blob/master/doc/Protocol.md#membership
        return new StringBuilder()
                .append("Version: ").append(Protocol.VERSION)
                .append("\nType: ").append(Protocol.TYPE_MEMBERSHIP)
                .append("\nCurrency: ").append(currency)
                .append("\nIssuer: ").append(pubkey)
                .append("\nBlock: ").append(membershipBlockUid)
                .append("\nMembership: ").append(sideIn ? "IN" : "OUT")
                .append("\nUserID: ").append(userId)
                .append("\nCertTS: ").append(selfBlockUid)
                .append("\n")
                .toString();
    }

    private Integer getLastBlockNumberFromJson(final String json) {
        int[] numbers = getBlockNumbersFromJson(json);
        if (numbers == null || numbers.length == 0) {
            return null;
        }
        return numbers[numbers.length-1];
    }

    private int[] getBlockNumbersFromJson(final String json) {

        String arrayPrefix = "\"blocks\": [";
        int startIndex = json.indexOf(arrayPrefix);
        int endIndex = json.lastIndexOf(']');
        if (startIndex == -1 || endIndex == -1) {
            return null;
        }

        String jsonArrayContent = json.substring(startIndex+arrayPrefix.length(),endIndex).trim();
        if (jsonArrayContent.length() == 0) {
            return null;
        }

        String[] blockNumbers = jsonArrayContent.split(",");

        try {
            int[] result = new int[blockNumbers.length];
            int index = 0;
            for (String blockNumber: blockNumbers) {
                result[index++] = Integer.parseInt(blockNumber.trim());
            }
            return result;
        } catch(NumberFormatException e) {
            if (log.isDebugEnabled()) {
                log.debug("Could not parse JSON (block numbers)");
            }
            throw new TechnicalException("Could not parse server response");
        }
    }


    protected Long getDividendFromBlockJson(String blockJson) {

        int startIndex = blockJson.indexOf(JSON_DIVIDEND_ATTR);
        if (startIndex == -1) {
            return null;
        }
        startIndex += JSON_DIVIDEND_ATTR.length();
        int endIndex = blockJson.indexOf(',', startIndex);
        if (endIndex == -1) {
            return null;
        }

        String dividendStr = blockJson.substring(startIndex, endIndex).trim();
        if (dividendStr.length() == 0
                || "null".equals(dividendStr)) {
            return null;
        }

        return Long.parseLong(dividendStr);
    }
}
