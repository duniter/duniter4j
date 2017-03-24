package fr.duniter.cmd.actions.params;

import com.beust.jcommander.Parameter;

/**
 * Created by blavenie on 22/03/17.
 */
public class WalletParameters {
    @Parameter(names = "--salt", description = "Salt (to generate the keypair)", required = true)
    public String salt;

    @Parameter(names = "--passwd", description = "Password (to generate the keypair)", required = true)
    public String password;
}
