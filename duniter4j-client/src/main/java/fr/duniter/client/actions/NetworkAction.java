package fr.duniter.client.actions;

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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.internal.Lists;
import dnl.utils.text.table.TextTable;
import fr.duniter.client.actions.params.PeerParameters;
import fr.duniter.client.actions.utils.ClearableConsole;
import fr.duniter.client.actions.utils.Formatters;
import org.apache.commons.io.IOUtils;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.client.service.local.NetworkService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.FileUtils;
import org.nuiton.i18n.I18n;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 22/03/17.
 */
@Parameters(resourceBundle = "i18n.duniter4j-client", commandDescription = "Display network peers", commandDescriptionKey = "duniter4j.client.network.action")
public class NetworkAction extends AbstractAction {

    @ParametersDelegate
    public PeerParameters peerParameters = new PeerParameters();

    @Parameter(names = "--continue", description = "Continue scanning ?", descriptionKey = "duniter4j.client.network.params.continue")
    private boolean autoRefresh = false;

    @Parameter(names = "--output", description = "Output file (CSV format)", descriptionKey = "duniter4j.client.network.params.output")
    private File outputFile = null;

    private ClearableConsole console;

    private DateFormat dateFormat;
    private List<String> knownBlocks = Lists.newArrayList();

    public NetworkAction() {
        super();
    }

    @Override
    public void run() {

        peerParameters.parse();
        final Peer mainPeer = peerParameters.getPeer();
        checkOutputFileIfNotNull(); // make sure the file (if any) is writable

        dateFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, I18n.getDefaultLocale());

        console = new ClearableConsole(System.out)
            .putRegexColor(I18n.t("duniter4j.client.network.ssl"), ClearableConsole.Color.green)
            .putRegexColor(I18n.t("duniter4j.client.network.mirror"), ClearableConsole.Color.lightgray);

        System.setOut(console);

        log.info(I18n.t("duniter4j.client.network.loadingPeers"));
        List<Peer> peers = loadPeers(mainPeer);

        showPeersTable(peers, true/*autoRefresh*/);

        if (autoRefresh) {
            BlockchainRemoteService bcService = ServiceLocator.instance().getBlockchainRemoteService();

            peers.stream().forEach(peer -> {
                String buid = peer.getStats().getBlockNumber() + "-" + peer.getStats().getBlockHash();
                if (!knownBlocks.contains(buid)) {
                    knownBlocks.add(buid);
                }
            });

            // Start listening for new peer...
            bcService.addPeerListener(mainPeer, message -> updatePeers(mainPeer, knownBlocks));
            // Start listening for new block...
            bcService.addBlockListener(mainPeer, message -> updatePeers(mainPeer, knownBlocks));

            try {
                while(true) {
                    Thread.sleep(10000); // 10 s

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // TODO: DEV only
       /* else  {
            try {
                int blockCount = 1500;
                while(true) {
                    Thread.sleep(2000); // 2 s

                    List<Peer> updatedPeers = new ArrayList<>();

                    for (int i=0; i<5; i++) {
                        Peer peer = Peer.newBuilder().setHost("p1").setPort(80)
                                .build();
                        peer.getStats().setBlockNumber(blockCount);
                        updatedPeers.add(peer);
                    }
                    updatedPeers.addAll(peers);

                    showPeersTable(updatedPeers, true);
                    blockCount++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

    }

    /* -- protected methods -- */

    public List<Peer> loadPeers(Peer mainPeer) {
        NetworkService service = ServiceLocator.instance().getNetworkService();
        return service.getPeers(mainPeer);
    }

    public void showPeersTable(List<Peer> peers, boolean clearConsole) {

        // Clearing console
        if (clearConsole) {
            clearConsole();
        }

        if (CollectionUtils.isEmpty(peers)) {
            console.println(I18n.t("duniter4j.client.network.noPeers"));
            return;
        }

        Peer mainConsensusPeer = peers.get(0);
        if (mainConsensusPeer.getStats().isMainConsensus()) {
            Long mediantTime = mainConsensusPeer.getStats().getMedianTime();
            if (mediantTime != null) {
                console.println(I18n.t("duniter4j.client.network.medianTime",
                        dateFormat.format(new Date(mediantTime * 1000))));
            }

            knownBlocks.stream().forEach(buid -> {
                console.putRegexColor(Formatters.formatBuid(buid), ClearableConsole.Color.lightgray);
            });

            console.putRegexColor(formatBuid(mainConsensusPeer.getStats()), ClearableConsole.Color.green);
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
                    isUp && peer.isUseSsl() ? I18n.t("duniter4j.client.network.ssl") : "",
                    isUp ? peer.getStats().getVersion() : "",
                    (isUp && peer.getStats().getHardshipLevel() != null) ? peer.getStats().getHardshipLevel() : I18n.t("duniter4j.client.network.mirror"),
                    isUp ? formatBuid(peer.getStats()) : ""
            };
        })
                .collect(Collectors.toList());

        Object[][] rows = new Object[data.size()][];
        int i = 0;
        for (Object[] row : data) {
            rows[i++] = row;
        }


        TextTable tt = new TextTable(columnNames, rows);

        // Write result to filCSV
        if (outputFile != null) {
            checkOutputFileIfNotNull();

            OutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(outputFile));
                tt.toCsv(os);
                os.flush();
            }
            catch (IOException e) {
                fail(e);
            }
            finally {
                IOUtils.closeQuietly(os);
            }
        }

        // Print result to console
        else {
            // this adds the numbering on the left
            tt.setAddRowNumbering(true);
            tt.printTable(console, 0);
        }
    }


    protected void checkOutputFileIfNotNull() {
        if (outputFile != null) {
            if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
                try {
                    FileUtils.forceMkdir(outputFile.getParentFile());
                } catch (IOException e) {
                    fail(e);
                }
            }

            if (outputFile.exists()) {
                if (!outputFile.delete() && !outputFile.canWrite()) {
                    fail(I18n.t("duniter4j.client.network.error.outputFieNotWritable"));
                }
            }
        }
    }

    protected void updatePeers(Peer mainPeer, List<String> knownBlocks) {
        List<Peer> updatedPeers = loadPeers(mainPeer);

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
    }

    protected void clearConsole() {
        console.clearConsole();
    }

    protected String formatBuid(Peer.Stats stats) {
        return Formatters.formatBuid(stats.getBlockNumber() + "-" + stats.getBlockHash());
    }
}
