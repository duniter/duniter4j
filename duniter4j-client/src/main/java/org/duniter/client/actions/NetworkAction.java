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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import dnl.utils.text.table.TextTable;
import org.duniter.client.actions.params.PeerParameters;
import org.duniter.client.actions.utils.RegexAnsiConsole;
import org.duniter.client.actions.utils.Formatters;
import org.apache.commons.io.IOUtils;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.config.ConfigurationOption;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.local.NetworkService;
import org.duniter.core.util.CollectionUtils;
import org.duniter.core.util.FileUtils;
import org.fusesource.jansi.Ansi;
import org.nuiton.i18n.I18n;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
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

    private RegexAnsiConsole console;

    private DateFormat dateFormat;

    public NetworkAction() {
        super();
    }

    @Override
    public void run() {


        peerParameters.parse();
        final Peer mainPeer = peerParameters.getPeer();
        checkOutputFileIfNotNull(); // make sure the file (if any) is writable

        // Reducing node timeout when broadcast
        if (peerParameters.timeout != null) {
            Configuration.instance().getApplicationConfig().setOption(ConfigurationOption.NETWORK_TIMEOUT.getKey(), peerParameters.timeout.toString());
        }

        dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM, I18n.getDefaultLocale());

        console = new RegexAnsiConsole();
        System.setOut(console);

        log.info(I18n.t("duniter4j.client.network.loadingPeers"));

        NetworkService service = ServiceLocator.instance().getNetworkService();

        if (!autoRefresh) {
            List<Peer> peers = service.getPeers(mainPeer);
            showPeersTable(peers, false);
        }
        else {
            service.addPeersChangeListener(mainPeer, peers -> showPeersTable(peers, true));

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


    public void showPeersTable(Collection<Peer> peers, boolean clearConsole) {

        // Clearing console
        if (clearConsole) {
            clearConsole();
        }

        if (CollectionUtils.isEmpty(peers)) {
            console.println(I18n.t("duniter4j.client.network.noPeers"));
            return;
        }

        Peer mainConsensusPeer = peers.iterator().next();
        Peer.Stats mainConsensusStats = mainConsensusPeer.getStats();
        if (mainConsensusStats.isMainConsensus()) {
            Long mediantTime = mainConsensusStats.getMedianTime();
            String medianTime = dateFormat.format(new Date(mediantTime * 1000));
            String mainBuid = formatBuid(mainConsensusStats);

            console.reset()
                   .fgString(I18n.t("duniter4j.client.network.ssl"), Ansi.Color.MAGENTA)
                   .fgString(I18n.t("duniter4j.client.network.mirror"), Ansi.Color.CYAN)
                   .fgString(mainBuid, Ansi.Color.GREEN)
                   .fgString(medianTime, Ansi.Color.GREEN);

            peers.stream()
                    .filter(peer -> peer.getStats().isForkConsensus())
                    .map(peer -> formatBuid(peer.getStats()))
                    .forEach(forkConsensusBuid -> console.fgString(Formatters.formatBuid(forkConsensusBuid), Ansi.Color.YELLOW));

            // Log blockchain info
            console.println("\t" + I18n.t("duniter4j.client.network.header",
                    mainBuid,
                    medianTime,
                    mainConsensusStats.getConsensusPct()
            ));
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

    protected void clearConsole() {
        console.eraseScreen();
    }

    protected String formatBuid(Peer.Stats stats) {
        return Formatters.formatBuid(stats.getBlockNumber() + "-" + stats.getBlockHash());
    }
}
