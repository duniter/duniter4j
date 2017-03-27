package fr.duniter.cmd.actions;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.internal.Lists;
import dnl.utils.text.table.SeparatorPolicy;
import dnl.utils.text.table.TextTable;
import fr.duniter.cmd.actions.params.PeerParameters;
import fr.duniter.cmd.actions.utils.Formatters;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.local.NetworkService;
import org.duniter.core.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 22/03/17.
 */
@Parameters(commandDescription = "Display network peers")
public class NetworkAction implements Runnable {
    private static Logger log = LoggerFactory.getLogger(NetworkAction.class);

    @ParametersDelegate
    public PeerParameters peerParameters = new PeerParameters();

    @Parameter(names = "--continue", description = "Continue scanning ?")
    private boolean autoRefresh = false;

    private int previousRowDisplayed = 0;

    @Override
    public void run() {

        peerParameters.parse();
        Peer mainPeer = peerParameters.getPeer();

        log.info("Loading peers...");
        NetworkService service = ServiceLocator.instance().getNetworkService();
        List<Peer> peers = service.getPeers(mainPeer);
        showPeersTable(peers, autoRefresh);

        if (autoRefresh) {
            final List<String> knownBlocks = Lists.newArrayList();
            peers.stream().forEach(peer -> {
                String buid = peer.getStats().getBlockNumber() + "-" + peer.getStats().getBlockHash();
                if (!knownBlocks.contains(buid)) {
                    knownBlocks.add(buid);
                }
            });

            // Start listening for new block...
            CompletableFuture.runAsync(() ->
                ServiceLocator.instance().getBlockchainRemoteService().addPeerListener(mainPeer, message -> {
                    List<Peer> updatedPeers = service.getPeers(mainPeer);

                    int knowBlockSize = knownBlocks.size();
                    updatedPeers.stream().forEach(peer -> {
                        String buid = peer.getStats().getBlockNumber() + "-" + peer.getStats().getBlockHash();
                        if (!knownBlocks.contains(buid)) {
                            knownBlocks.add(buid);
                        }
                    });

                    // new block received: refresh console
                    if (knowBlockSize < knownBlocks.size()) {
                        showPeersTable(updatedPeers, true);
                    }

                }));

            try {
                while(true) {
                    Thread.sleep(10000); // 10 s
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    /* -- protected methods -- */

    public void showPeersTable(List<Peer> peers, boolean clearConsole) {

        // Clearing console
        if (clearConsole) {
            clearConsole();
        }

        if (CollectionUtils.isEmpty(peers)) {
            JCommander.getConsole().println("No peers found");
            return;
        }
        String[] columnNames = {
                "Uid",
                "Pubkey",
                "Address",
                "Status",
                "API",
                "Version",
                "Difficulty",
                "Block #"};

        List<Object[]> data = peers.stream().map(peer -> {
            boolean isUp = peer.getStats().getStatus() == Peer.PeerStatus.UP;
            return new Object[] {
                    Formatters.formatUid(peer.getStats().getUid()),
                    Formatters.formatPubkey(peer.getPubkey()),
                    peer.getHost() + ":" + peer.getPort(),
                    peer.getStats().getStatus().name(),
                    isUp && peer.isUseSsl() ? "SSL" : null,
                    isUp ? peer.getStats().getVersion() : null,
                    (isUp && peer.getStats().getHardshipLevel() != null) ? peer.getStats().getHardshipLevel() : "Mirror",
                    isUp ? peer.getStats().getBlockNumber() : null
            };
        })
                .collect(Collectors.toList());

        Object[][] rows = new Object[data.size()][];
        int i = 0;
        for (Object[] row : data) {
            rows[i++] = row;
        }


        TextTable tt = new TextTable(columnNames, rows);
        // this adds the numbering on the left
        tt.setAddRowNumbering(true);
        tt.printTable();

        previousRowDisplayed = 3/*header rows*/ + rows.length;
    }

    protected void moveCursor(int nbLinesUp) {
        System.out.print(String.format("\033[%dA",nbLinesUp)); // Move up
        System.out.print("\033[2K"); // Erase line content
    }

    protected void clearConsole() {
        System.out.print(String.format("\033[2J"));
    }
}
