package org.duniter.client.actions.validators;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import org.duniter.core.client.model.bma.Constants;

import java.util.regex.Pattern;

public class PubkeyValidator implements IParameterValidator {

    private final static Pattern PUBKEY_PATTERN = Pattern.compile("^" + Constants.Regex.PUBKEY + "$");

    public PubkeyValidator() {
    }

    public void validate(String option, String value) throws ParameterException {
        if(!PUBKEY_PATTERN.matcher(value).matches()) {
            throw new ParameterException("Parameter " + option + " should be a valid public key (found " + value + ")");
        }
    }
}