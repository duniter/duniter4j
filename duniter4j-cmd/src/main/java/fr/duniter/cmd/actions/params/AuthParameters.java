package fr.duniter.cmd.actions.params;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.CommaParameterSplitter;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.collect.ImmutableList;
import org.duniter.core.service.Ed25519CryptoServiceImpl;
import org.duniter.core.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by blavenie on 22/03/17.
 */
public class AuthParameters {

    @Parameter(names = "--auth-scrypt", description = "Authenticate using Scrypt ?")
    public boolean authScrypt = true;

    @Parameter(names = "--salt", description = "Salt (to generate the keypair)", password = true)
    public char[] salt;

    @Parameter(names = "--passwd", description = "Password (to generate the keypair)", password = true)
    public char[] password;

    @Parameter(names = "--scrypt-params", description = "Scrypt parameters (N,r,p)",
            splitter = CommaParameterSplitter.class,
            validateWith = PositiveInteger.class)
    public List<Integer> scryptPArams;

    public void parse() {
        // Compute keypair and wallet
        if (StringUtils.isBlank(salt) && authScrypt) {
            JCommander.getConsole().print("Please enter your Scrypt Salt (Secret identifier): ");
            salt = JCommander.getConsole().readPassword(true);
        }
        if (StringUtils.isBlank(password) && authScrypt){
            JCommander.getConsole().print("Please enter your Scrypt password (masked): ");
            password = JCommander.getConsole().readPassword(true);
        }
        if (scryptPArams == null && authScrypt) {
            JCommander.getConsole().print(String.format("Please enter your Scrypt parameters (N,r,p): [%d,%d,%d] ",
                    Ed25519CryptoServiceImpl.SCRYPT_PARAMS_N,
                    Ed25519CryptoServiceImpl.SCRYPT_PARAMS_r,
                    Ed25519CryptoServiceImpl.SCRYPT_PARAMS_p));
            char[] scryptsParamsStr = JCommander.getConsole().readPassword(false);
            if (StringUtils.isNotBlank(scryptsParamsStr)) {
                String[] parts = new String(scryptsParamsStr).split(",");
                if (parts.length != 3) {
                    throw new ParameterException("Invalid Scrypt parameters (expected 3 values)");
                }
                scryptPArams = Arrays.asList(parts).stream().map(part -> Integer.parseInt(part)).collect(Collectors.toList());
            }
            else {
                scryptPArams = ImmutableList.of(
                        Ed25519CryptoServiceImpl.SCRYPT_PARAMS_N,
                        Ed25519CryptoServiceImpl.SCRYPT_PARAMS_r,
                        Ed25519CryptoServiceImpl.SCRYPT_PARAMS_p);
            }
        }
    }

}
