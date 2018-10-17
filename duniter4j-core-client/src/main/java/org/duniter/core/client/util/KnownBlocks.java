package org.duniter.core.client.util;

import org.duniter.core.client.model.bma.BlockchainBlock;
import org.duniter.core.exception.TechnicalException;

public class KnownBlocks {


    private KnownBlocks() {
        // helper class
    }

    public static BlockchainBlock getFirstBlock(String currencyId) {

        BlockchainBlock result = new BlockchainBlock();
        result.setNumber(0);

        // G1 currency
        switch (currencyId) {

            case KnownCurrencies.G1 :
                result.setCurrency("g1");
                result.setHash("000003D02B95D3296A4F06DBAC51775C4336A4DC09D0E958DC40033BE7E20F3D");
                result.setTime(1488987127L);
                result.setMedianTime(1488987127L);
                result.setIssuer("2ny7YAdmzReQxAayyJZsyVYwYhVyax2thKcGknmQy5nQ");
                result.setSignature("49OD/8pj0bU0Lg6HB4p+5TOcRbgtj8Ubxmhen4IbOXM+g33V/I56GfF+QbD9U138Ek04E9o0lSjaDIVI/BrkCw==");
                break;

            case KnownCurrencies.G1_TEST :
                result.setCurrency("g1-test");
                result.setHash("0000DEFA598EA82BC8FF19BC56B49A686E63617DCC7304FAF7F0461FA34E0F9C");
                result.setTime(1496842431L);
                result.setMedianTime(1496842431L);
                result.setIssuer("3dnbnYY9i2bHMQUGyFp5GVvJ2wBkVpus31cDJA5cfRpj");
                result.setSignature("OQQJ8TVISMgpz8SmdVGHYAUQMDnHpXqeFal4+/q2hV37uyrpC8iF6d50Wgg2TMKhsB/9zelOXZgbuzutAOZ5AA==");
                break;

            default:
                throw new TechnicalException(String.format("First block for currency %s not defined !", currencyId));
        }

        return result;
    }
}
