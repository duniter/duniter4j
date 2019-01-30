package org.duniter.client.actions;

/*
 * #%L
 * Duniter4j :: Client
 * %%
 * Copyright (C) 2014 - 2017 EIS
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

import com.beust.jcommander.*;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.collect.ImmutableList;
import org.duniter.client.actions.params.AuthParameters;
import org.duniter.client.actions.params.PeerParameters;
import org.duniter.client.actions.utils.Formatters;
import org.duniter.client.actions.validators.PubkeyValidator;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.config.ConfigurationOption;
import org.duniter.core.client.model.bma.EndpointApi;
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.TransactionRemoteService;
import org.duniter.core.client.service.local.NetworkService;
import org.duniter.core.client.service.local.PeerService;
import org.duniter.core.exception.BusinessException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.crypto.KeyPair;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by blavenie on 22/03/17.
 */
@Parameters(resourceBundle = "i18n.duniter4j-client", commandDescription = "Send a transaction", commandDescriptionKey = "duniter4j.client.transaction.action")
public class TransactionAction extends AbstractAction  {

    private static Logger log = LoggerFactory.getLogger(TransactionAction.class);

    @ParametersDelegate
    public AuthParameters authParameters = new AuthParameters();

    @ParametersDelegate
    public PeerParameters peerParameters = new PeerParameters();

    @Parameter(names = "--amount", description = "Amount", validateWith = PositiveInteger.class)
    public Integer amount;

    @Parameter(names = "--output", description = "Output pubkey", validateWith = PubkeyValidator.class)
    public String output;

    @Parameter(names = "--comment", description = "TX Comment")
    public String comment;

    @Parameter(names = "--broadcast", description = "Broadcast document sent to all nodes")
    public boolean broadcast = false;

    private int mainConsensusPeerCount = 0;
    private int forkConsensusPeerCount = 0;

    public TransactionAction() {
        super();
    }

    @Override
    public void run() {
        try {

            // Make sure auth parameters are filled
            authParameters.parse();
            peerParameters.parse();

            // Reducing node timeout when broadcast
            if (peerParameters.timeout != null) {
                Configuration.instance().getApplicationConfig().setOption(ConfigurationOption.NETWORK_TIMEOUT.getKey(), peerParameters.timeout.toString());
            }

            Peer peer = peerParameters.getPeer();

            Currency currency = ServiceLocator.instance().getBlockchainRemoteService().getCurrencyFromPeer(peer);
            ServiceLocator.instance().getCurrencyService().save(currency);
            peer.setCurrency(currency.getId());
            ServiceLocator.instance().getPeerService().save(peer);


            // Compute keypair and wallet
            KeyPair keypair;
            if (authParameters.authScrypt) {
                CryptoService cryptoService = ServiceLocator.instance().getCryptoService();
                keypair = cryptoService.getKeyPairFromSeed(
                        cryptoService.getSeed(
                        new String(authParameters.salt),
                        new String(authParameters.password),
                                authParameters.scryptParams.get(0), // N
                                authParameters.scryptParams.get(1), // p
                                authParameters.scryptParams.get(2) // r
                        ));
            }
            else {
                fail(I18n.t("duniter4j.client.transaction.error.unknownAuth"));
                return;
            }

            Wallet wallet = new Wallet(
                    currency.getId(),
                    null,
                    keypair.getPubKey(),
                    keypair.getSecKey());
            wallet.setCurrencyId(currency.getId());

            // Parse TX parameters
            parseTransactionParameters();

            // Send TX document to ONE peer
            if (!broadcast) {
                sendToPeer(peer, wallet);
            }

            // Sent TX using broadcast
            else {
                sendBroadcast(peer, currency, wallet, peerParameters.useSsl);
            }
        }
        catch(BusinessException | TechnicalException e) {
            fail(e);
        }
    }

    protected void parseTransactionParameters() {

        // Get output
        while (output == null) {
            JCommander.getConsole().print(I18n.t("duniter4j.client.transaction.params.output.ask") + " ");
            output = new String(JCommander.getConsole().readPassword(false));
            try {
                new PubkeyValidator().validate("output", output);
            } catch (ParameterException e) {
                JCommander.getConsole().println(e.getMessage());
                output = null;
            }
        }

        // Get Amount
        while (amount == null) {
            JCommander.getConsole().print(I18n.t("duniter4j.client.transaction.params.amount.ask") + " ");
            String amountStr = new String(JCommander.getConsole().readPassword(false));
            try {
                new PositiveInteger().validate("amount", amountStr);
                amount = Integer.parseInt(amountStr);
            } catch (ParameterException e) {
                JCommander.getConsole().println(e.getMessage());
            }
        }
    }

    protected void sendToPeer(Peer peer, Wallet wallet) {
        TransactionRemoteService txService = ServiceLocator.instance().getTransactionRemoteService();

        logTxSummary(wallet);

        txService.transfer(peer, wallet, output, amount, comment);
        JCommander.getConsole().println(I18n.t("duniter4j.client.transaction.sent"));
    }

    protected void sendBroadcast(Peer mainPeer, Currency currency, Wallet wallet, boolean useSsl) {

        TransactionRemoteService txService = ServiceLocator.instance().getTransactionRemoteService();
        PeerService peerService = ServiceLocator.instance().getPeerService();

        log.info(I18n.t("duniter4j.client.transaction.loadingMemberPeers"));

        // Filter to [member + UP] peers
        NetworkService.Filter peersFilter = new NetworkService.Filter();
        peersFilter.filterType = NetworkService.FilterType.MEMBER;
        peersFilter.filterStatus = Peer.PeerStatus.UP;
        if (useSsl) {
            peersFilter.filterEndpoints = ImmutableList.of(EndpointApi.BMAS.name());
        }
        else {
            peersFilter.filterEndpoints = ImmutableList.of(EndpointApi.BASIC_MERKLED_API.name());
        }
        // Sort by [lesser difficulty first]
        NetworkService.Sort sortLesserDifficulty = new NetworkService.Sort();
        sortLesserDifficulty.sortType = NetworkService.SortType.HARDSHIP;
        sortLesserDifficulty.sortAsc = true;

        // Get the peers list
        List<Peer> peers = ServiceLocator.instance().getNetworkService().getPeers(mainPeer, peersFilter, sortLesserDifficulty);

        if (CollectionUtils.isEmpty(peers)) {
            log.warn(I18n.t("duniter4j.client.transaction.error.broadcast.noMemberPeer"));
            sendToPeer(mainPeer, wallet);
            return;
        }

        log.info(I18n.t("duniter4j.client.transaction.broadcast.memberPeerCount", peers.size()));

        logTxSummary(wallet);

        peers.stream().forEach(peer -> {
            peer.setCurrency(currency.getId());
            peerService.save(peer);

            log.debug(String.format("Send TX to [%s]...", peer));
            try {
                txService.transfer(peer, wallet, output, amount, comment);

                log.debug(String.format("Successfully sent to [%s]", peer));

                if (peer.getStats() != null) {
                    if (peer.getStats().isMainConsensus()) {
                        mainConsensusPeerCount++;
                    } else if (peer.getStats().isForkConsensus()) {
                        forkConsensusPeerCount++;
                    }
                }

            }
            catch (Exception e) {
                log.debug(String.format("Could not send transaction to [%s]: %s", peer, e.getMessage()));
            }
        });

        if (mainConsensusPeerCount > 0) {
            JCommander.getConsole().println(I18n.t("duniter4j.client.transaction.broadcast.success", mainConsensusPeerCount));
        }
        else if (forkConsensusPeerCount > 0){
            fail(I18n.t("duniter4j.client.transaction.broadcast.successOnForkOnly", forkConsensusPeerCount));
        }
        else {
            fail(I18n.t("duniter4j.client.transaction.broadcast.failed"));
        }

    }

    protected void logTxSummary(Wallet wallet) {
        // Log TX summary
        JCommander.getConsole().println(I18n.t("duniter4j.client.transaction.broadcast.summary",
                Formatters.formatPubkey(wallet.getPubKeyHash()),
                Formatters.formatPubkey(output),
                amount,
                Formatters.currencySymbol(wallet.getCurrency())));
    }

}
