package fr.duniter.client.actions.utils;

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

import org.duniter.core.util.StringUtils;

/**
 * Created by blavenie on 24/03/17.
 */
public class Formatters {

    public static String formatPubkey(String pubkey) {
        if (pubkey == null) {
            return "";
        }
        if (pubkey != null && pubkey.length() > 8) {
            return pubkey.substring(0, 8);
        }
        return pubkey;
    }

    public static String formatUid(String uid) {
        if (StringUtils.isBlank(uid)) {
            return "";
        }
        if (uid != null && uid.length() > 20) {
            return uid.substring(0, 19);
        }
        return uid;
    }

    public static String currencySymbol(String currencyName) {
        String[] parts = currencyName.split("-_");
        if (parts.length < 2) {
            if (currencyName.length() <= 3) {
                return currencyName.toUpperCase();
            }
            else {
                return currencyName.toUpperCase().substring(0,1);
            }
        }
        return currencySymbol(parts[0]) + currencySymbol(parts[1]);
    }

    public static String formatBuid(String buid) {
        if (StringUtils.isBlank(buid)) {
            return "";
        }
        int index = buid.indexOf('-');
        if (index + 10 >= buid.length())  {
            return buid;
        }
        return buid.substring(0, index + 10);
    }

}
