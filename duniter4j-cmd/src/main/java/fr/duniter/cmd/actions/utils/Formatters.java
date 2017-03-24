package fr.duniter.cmd.actions.utils;

/**
 * Created by blavenie on 24/03/17.
 */
public class Formatters {

    public static String formatPubkey(String pubkey) {
        if (pubkey != null && pubkey.length() > 8) {
            return pubkey.substring(0, 8);
        }
        return pubkey;
    }

    public static String formatUid(String uid) {
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
}
