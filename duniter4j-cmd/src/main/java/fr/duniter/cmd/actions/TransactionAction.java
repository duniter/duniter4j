package fr.duniter.cmd.actions;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.collect.ImmutableList;
import fr.duniter.cmd.actions.params.AuthParameters;
import fr.duniter.cmd.actions.params.PeerParameters;
import fr.duniter.cmd.actions.utils.Formatters;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by blavenie on 22/03/17.
 */
public class TransactionAction implements Runnable {

    private static Logger log = LoggerFactory.getLogger(TransactionAction.class);

    @ParametersDelegate
    public AuthParameters authParameters = new AuthParameters();

    @ParametersDelegate
    public PeerParameters peerParameters = new PeerParameters();

    @Parameter(names = "--amount", description = "Amount", required = true, validateWith = PositiveInteger.class)
    public int amount;

    @Parameter(names = "--output", description = "Output pubkey", required = true)
    public String output;

    @Parameter(names = "--comment", description = "TX Comment")
    public String comment;

    private int mainConsensusPeerCount = 0;
    private int forkConsensusPeerCount = 0;

    @Override
    public void run() {
        try {

            // Make sure auth parameters are filled
            authParameters.parse();
            peerParameters.parse();

            // Reducing node timeout when broadcast


            Peer peer = peerParameters.getPeer();

            Currency currency = ServiceLocator.instance().getBlockchainRemoteService().getCurrencyFromPeer(peer);
            ServiceLocator.instance().getCurrencyService().save(currency);
            peer.setCurrencyId(currency.getId());
            peer.setCurrency(currency.getCurrencyName());
            ServiceLocator.instance().getPeerService().save(peer);


            // Compute keypair and wallet
            KeyPair keypair = null;
            if (authParameters.authScrypt) {
                CryptoService cryptoService = ServiceLocator.instance().getCryptoService();
                keypair = cryptoService.getKeyPairFromSeed(
                        cryptoService.getSeed(
                        new String(authParameters.salt),
                        new String(authParameters.password),
                                authParameters.scryptPArams.get(0), // N
                                authParameters.scryptPArams.get(1), // p
                                authParameters.scryptPArams.get(2) // r
                        ));
            }
            else {
                fail("Unknwon authentification type");
                return;
            }

            Wallet wallet = new Wallet(
                    currency.getCurrencyName(),
                    null,
                    keypair.getPubKey(),
                    keypair.getSecKey());
            wallet.setCurrencyId(currency.getId());

            // Send TX document to ONE peer
            if (!peerParameters.broadcast) {
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

    protected void fail(String message) {
        log.error(message);
        System.exit(-1);
    }

    protected void sendToPeer(Peer peer, Wallet wallet) {
        TransactionRemoteService txService = ServiceLocator.instance().getTransactionRemoteService();

        logTxSummary(wallet);

        txService.transfer(peer, wallet, output, amount, comment);
        JCommander.getConsole().println("Transaction successfully sent.");
    }

    protected void sendBroadcast(Peer mainPeer, Currency currency, Wallet wallet, boolean useSsl) {

        TransactionRemoteService txService = ServiceLocator.instance().getTransactionRemoteService();
        PeerService peerService = ServiceLocator.instance().getPeerService();

        log.info("Loading member peers...");

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
            log.warn("No members peers found! Skipping --broadcast option.");
            sendToPeer(mainPeer, wallet);
            return;
        }

        log.info(String.format("%d member peers found for broadcast", peers.size()));

        logTxSummary(wallet);

        peers.stream().forEach(peer -> {
            peer.setCurrencyId(currency.getId());
            peer.setCurrency(currency.getCurrencyName());
            peerService.save(peer);

            log.debug(String.format("Send TX to [%s]...", peer));
            try {
                txService.transfer(peer, wallet, output, amount, comment);

                log.warn(String.format("Successfully sent to [%s]", peer));

                if (peer.getStats() != null) {
                    if (peer.getStats().isMainConsensus()) {
                        mainConsensusPeerCount++;
                    } else if (peer.getStats().isForkConsensus()) {
                        forkConsensusPeerCount++;
                    }
                }

            }
            catch (Exception e) {
                log.warn(String.format("Could not send transaction to [%s]: %s", peer, e.getMessage()));
            }
        });

        if (mainConsensusPeerCount > 0) {
            JCommander.getConsole().println(String.format("Transaction successfully sent (to %d nodes on the main blockchain consensus).", mainConsensusPeerCount));
        }
        else if (forkConsensusPeerCount > 0){
            fail(String.format("Transaction has NOT been sent to the main consensus BlockChain, but ONLY to %d peers on a fork of the blockchain.", forkConsensusPeerCount));
        }
        else {
            fail(String.format("Transaction has NOT been sent. Not a single peer has accepted the transaction."));
        }

    }


    protected void logTxSummary(Wallet wallet) {
        // Log TX summary
        JCommander.getConsole().println(String.format("Generate Transation:\n\t- From:   %s\n\t- To:     %s\n\t- Amount: %s %s",
                Formatters.formatPubkey(wallet.getPubKeyHash()),
                Formatters.formatPubkey(output),
                amount,
                Formatters.currencySymbol(wallet.getCurrency())));
    }

    protected void fail(Exception e) {
        fail(e.getMessage());
    }
}
