package fr.duniter.cmd.actions;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import dnl.utils.text.table.TextTable;
import fr.duniter.cmd.actions.utils.Formatters;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.ServiceLocator;
import org.duniter.core.client.service.local.NetworkService;
import org.duniter.core.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 22/03/17.
 */
@Parameters(commandDescription = "Display network peers")
public class NetworkAction implements Runnable {

    @Parameter(names = "-host", description = "Duniter host")
    private String host = "g1.duniter.org";

    @Parameter(names = "-port", description = "Duniter port")
    private int port = 10901;

    @Override
    public void run() {
        NetworkService service = ServiceLocator.instance().getNetworkService();
        Peer mainPeer = Peer.newBuilder().setHost(host).setPort(port).build();

        List<Peer> peers = service.getPeers(mainPeer);

        if (CollectionUtils.isEmpty(peers)) {
            System.out.println("No peers found");
        }
        else {

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
                            isUp ? peer.getStats().getHardshipLevel() : "Mirror",
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
        }

    }


}
