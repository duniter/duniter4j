package fr.duniter.cmd.actions.params;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.util.Preconditions;
import org.duniter.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by blavenie on 22/03/17.
 */
public class PeerParameters {

    private static Logger log = LoggerFactory.getLogger(PeerParameters.class);

    @Parameter(names = {"-p", "--peer"}, description = "Peer address (use format: 'host:port')")
    public String peerStr;

    @Parameter(names = "--broadcast", description = "Broadcast document sent to all nodes")
    public boolean broadcast = false;

    @Parameter(names = "--ssl", description = "Using SSL connection to node")
    public boolean useSsl = false;

    @Parameter(names = "--timeout", description = "HTTP request timeout, in millisecond")
    public Long timeout = null;

    private Peer peer = null;

    public void parse() {
        if (StringUtils.isNotBlank(peerStr)) {
            String[] parts = peerStr.split(":");
            if (parts.length > 2) {
                throw new ParameterException("Invalid --peer parameter");
            }
            String host = parts[0];
            Integer port = parts.length == 2 ? Integer.parseInt(parts[1]) : null;

            Peer.Builder peerBuilder = Peer.newBuilder().setHost(host);
            if (port != null) {
                peerBuilder.setPort(port);
            }
            if (useSsl){
                peerBuilder.setUseSsl(useSsl);
            }
            peer = peerBuilder.build();

            log.info(String.format("Duniter node: [%s:%s]", peer.getHost(), peer.getPort()));
        }
        else {
            Configuration config = Configuration.instance();
            peer = Peer.newBuilder().setHost(config.getNodeHost())
                    .setPort(config.getNodePort())
                    .build();
            log.info(String.format("Fallback to default Duniter node: [%s:%d]", peer.getHost(), peer.getPort()));
        }
    }

    public Peer getPeer() {
        Preconditions.checkNotNull(peer, "Please call parse() before getPeer().");
        return peer;
    }
}
