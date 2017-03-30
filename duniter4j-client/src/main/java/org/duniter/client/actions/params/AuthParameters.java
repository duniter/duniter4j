package org.duniter.client.actions.params;

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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.CommaParameterSplitter;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.collect.ImmutableList;
import org.duniter.core.service.Ed25519CryptoServiceImpl;
import org.duniter.core.util.StringUtils;
import org.nuiton.i18n.I18n;

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
            JCommander.getConsole().print(I18n.t("duniter4j.client.params.authScrypt.ask.salt"));
            salt = JCommander.getConsole().readPassword(true);
        }
        if (StringUtils.isBlank(password) && authScrypt){
            JCommander.getConsole().print(I18n.t("duniter4j.client.params.authScrypt.ask.passwd"));
            password = JCommander.getConsole().readPassword(true);
        }
        if (scryptPArams == null && authScrypt) {
            JCommander.getConsole().print(I18n.t("duniter4j.client.params.authScrypt.ask.scryptParams",
                    Ed25519CryptoServiceImpl.SCRYPT_PARAMS_N,
                    Ed25519CryptoServiceImpl.SCRYPT_PARAMS_r,
                    Ed25519CryptoServiceImpl.SCRYPT_PARAMS_p));
            char[] scryptsParamsStr = JCommander.getConsole().readPassword(false);
            if (StringUtils.isNotBlank(scryptsParamsStr)) {
                String[] parts = new String(scryptsParamsStr).split(",");
                if (parts.length != 3) {
                    throw new ParameterException(I18n.t("duniter4j.client.params.authScrypt.error.scryptParams"));
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
