package org.duniter.core.client.model;

/*
 * #%L
 * UCoin Java :: Core Client API
 * %%
 * Copyright (C) 2014 - 2016 EIS
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

import java.util.*;

import org.duniter.core.util.Preconditions;
import org.duniter.core.client.model.local.Certification;
import org.duniter.core.client.model.local.Movement;
import org.duniter.core.util.CollectionUtils;

/**
 * Helper class on model entities
 * Created by eis on 04/04/15.
 */
public class ModelUtils {

    /**
     * Order certification by cert time (DESC), uid ASC, pubkey (ASC)
     * @return a new comparator
     */
    public static Comparator<Certification> newWotCertificationComparatorByDate() {
        return new Comparator<Certification>() {
            @Override
            public int compare(Certification lhs, Certification rhs) {
                int result = 0;

                // cert time (order DESC)
                long lct = lhs.getTimestamp();
                long rct = rhs.getTimestamp();
                if (lct != rct) {
                    return lct < rct ? 1 : -1;
                }

                // uid
                if (lhs.getUid() != null) {
                    result = lhs.getUid().compareToIgnoreCase(rhs.getUid());
                    if (result != 0) {
                        return result;
                    }
                }
                else if (rhs.getUid() != null) {
                    return 1;
                }

                // pub key
                if (lhs.getPubkey() != null) {
                    result = lhs.getPubkey().compareToIgnoreCase(rhs.getPubkey());
                    if (result != 0) {
                        return result;
                    }
                }
                else if (rhs.getPubkey() != null) {
                    return 1;
                }
                return 0;
            }
        };
    }


    /**
     * Order certification by uid (ASC), pubkey (ASC), cert time (DESC)
     * @return a new comparator
     */
    public static Comparator<Certification> newWotCertificationComparatorByUid() {
        return new Comparator<Certification>() {
            @Override
            public int compare(Certification lhs, Certification rhs) {
                int result = 0;
                // uid
                if (lhs.getUid() != null) {
                    result = lhs.getUid().compareToIgnoreCase(rhs.getUid());
                    if (result != 0) {
                        return result;
                    }
                }
                else if (rhs.getUid() != null) {
                    return 1;
                }

                // pub key
                if (lhs.getPubkey() != null) {
                    result = lhs.getPubkey().compareToIgnoreCase(rhs.getPubkey());
                    if (result != 0) {
                        return result;
                    }
                }
                else if (rhs.getPubkey() != null) {
                    return 1;
                }

                // cert time (order DESC)
                long lct = lhs.getTimestamp();
                long rct = rhs.getTimestamp();
                return lct < rct ? 1 : (lct == rct ? 0 : -1);
            }
        };
    }

    /**
     * Return a small string, for the given pubkey.
     * @param pubkey
     * @return
     */
    public static String minifyPubkey(String pubkey) {
        if (pubkey == null || pubkey.length() < 6) {
            return pubkey;
        }
        return pubkey.substring(0, 8);
    }

    public static String joinPubkeys(Set<String> pubkeys, String separator, boolean minify) {
        Preconditions.checkNotNull(pubkeys);
        Preconditions.checkArgument(pubkeys.size()>0);
        if (pubkeys.size() == 1) {
            String pubkey = pubkeys.iterator().next();
            return (minify ? ModelUtils.minifyPubkey(pubkey) : pubkey);
        }

        StringBuilder sb = new StringBuilder();
        pubkeys.stream().forEach((pubkey)-> {
            sb.append(separator);
            sb.append(minify ? ModelUtils.minifyPubkey(pubkey) : pubkey);
        });

        return sb.substring(separator.length());
    }
}
