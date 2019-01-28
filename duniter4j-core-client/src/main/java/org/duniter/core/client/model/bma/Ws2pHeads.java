package org.duniter.core.client.model.bma;

import org.duniter.core.client.model.bma.jackson.Ws2pHeadDeserializer;
import org.duniter.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ws2pHeads {

    private static final Logger log = LoggerFactory.getLogger(Ws2pHeads.class);

    public static final String WS2P_PREFIX = "^WS2P(?:O([CT][SAM]))?(?:I([CT]))?$";

    public static final Pattern WS2P_PREFIX_PATTERN = Pattern.compile(WS2P_PREFIX);

    private Ws2pHeads() {
        // helper class
    }

    public static Ws2pHead parse(String message) throws IOException {

        try {
            String[] parts = message.split(":");
            if (parts.length < 3 || !parts[0].startsWith("WS2P")) {
                throw new IOException("Invalid WS2P message format: " + message);
            }
            // Head message
            if ("HEAD".equals(parts[1])) {
                if (parts.length < 4) {
                    throw new IllegalArgumentException("Invalid WS2P message format: " + message);
                }

                // Duniter version < 1.6.9
                if (parts.length == 4) {
                    Ws2pHead result = new Ws2pHead();
                    result.setPubkey(parts[2]);
                    result.setBlock(parts[3]);
                } else {
                    int version = Integer.parseInt(parts[2]);
                    if (version >= 1) {
                        Ws2pHead result = new Ws2pHead();
                        String prefix = parts[0];

                        // Private/public options
                        if (prefix.length() > 4) {

                            Matcher matches = WS2P_PREFIX_PATTERN.matcher(prefix);
                            if (!matches.matches()) {
                                throw new IllegalArgumentException("Invalid WS2P message format: " + message);
                            }

                            // Private options
                            String privateOptions = matches.group(1);
                            if (StringUtils.isNotBlank(privateOptions)) {
                                Ws2pHead.AccessConfig privateConfig = result.getPrivateConfig();
                                privateConfig.setUseTor(privateOptions.startsWith("T"));
                                String mode = privateOptions.substring(1);
                                switch (mode) {
                                    case "A":
                                        privateConfig.setMode("all");
                                        break;
                                    case "M":
                                        privateConfig.setMode("mixed");
                                        break;
                                    case "S":
                                        privateConfig.setMode("strict");
                                        break;
                                }
                            }

                            // Public options
                            String publicOptions = matches.group(2);
                            if (StringUtils.isNotBlank(publicOptions)) {
                                Ws2pHead.AccessConfig publicConfig = result.getPrivateConfig();
                                publicConfig.setUseTor(publicOptions.startsWith("T"));
                                publicConfig.setMode("all");
                            }

                            // For DEBUG only:
                            log.debug(String.format("Parsing WS2P prefix {%s} into: private %s, public %s",
                                     prefix,
                                      ((result.getPrivateConfig().isUseTor() ? "TOR " : "" ) + (result.getPrivateConfig().getMode())),
                                      ((result.getPublicConfig().isUseTor() ? "TOR " : "" ) + (result.getPublicConfig().getMode()))
                            ));
                        }

                        result.setVersion(version);
                        result.setPubkey(parts[3]);
                        result.setBlock(parts[4]);
                        result.setWs2pid(parts[5]);
                        result.setSoftware(parts[6]);
                        result.setSoftwareVersion(parts[7]);
                        result.setPowPrefix(parts[8]);

                        return result;
                    }
                }

            }

            return null;
        }
        catch(Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
