package fr.duniter.cmd.actions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;
import fr.duniter.cmd.actions.params.WalletParameters;
import fr.duniter.cmd.actions.utils.Formatters;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.BlockchainParameters;
import org.duniter.core.client.model.local.Currency;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.model.local.Wallet;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.client.service.bma.TransactionRemoteService;
import org.duniter.core.service.CryptoService;
import org.duniter.core.util.crypto.CryptoUtils;
import org.duniter.core.util.crypto.KeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by blavenie on 22/03/17.
 */
public class SentMoneyAction implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(SentMoneyAction.class);

    @ParametersDelegate
    private WalletParameters walletParams = new WalletParameters();

    @Parameter(names = "--amount", description = "Amount", required = true)
    public int amount;

    @Parameter(names = "--dest", description = "Destination pubkey", required = true)
    public String destPubkey;

    @Parameter(names = "--comment", description = "TX Comment")
    public String comment;

    @Override
    public void run() {

        CryptoService cryptoService = ServiceLocator.instance().getCryptoService();
        TransactionRemoteService txService = ServiceLocator.instance().getTransactionRemoteService();
        Configuration config = Configuration.instance();

        Peer peer = Peer.newBuilder().setHost(config.getNodeHost())
                .setPort(config.getNodePort())
                .build();

        Currency currency = ServiceLocator.instance().getBlockchainRemoteService().getCurrencyFromPeer(peer);
        ServiceLocator.instance().getCurrencyService().save(currency);
        peer.setCurrencyId(currency.getId());
        peer.setCurrency(currency.getCurrencyName());
        ServiceLocator.instance().getPeerService().save(peer);

        // Compute keypair and wallet
        KeyPair keypair = cryptoService.getKeyPair(walletParams.salt, walletParams.password);
        Wallet wallet = new Wallet(
                currency.getCurrencyName(),
                null,
                keypair.getPubKey(),
                keypair.getSecKey());
        wallet.setCurrencyId(currency.getId());

        System.out.println("Connected to wallet: " + wallet.getPubKeyHash());

        txService.transfer(wallet, destPubkey, amount, comment);


        System.out.println(String.format("Successfully sent [%d %s] to [%s]",
                amount,
                Formatters.currencySymbol(currency.getCurrencyName()),
                Formatters.formatPubkey(destPubkey)));
    }
}
