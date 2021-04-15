package org.duniter.core.client.model.local;

import lombok.Builder;
import lombok.Data;
import org.duniter.core.model.IEntity;

import java.math.BigInteger;

@Data
@Builder
public class Dividend implements IEntity<String> {

    public static final String computeId(Dividend entity) {
        return entity.currency + "-" + entity.number;
    }

    private String id;
    private String currency;
    private Integer number;
    private Long dividend;

}
