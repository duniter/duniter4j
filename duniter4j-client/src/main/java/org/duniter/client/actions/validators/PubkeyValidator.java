package org.duniter.client.actions.validators;

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