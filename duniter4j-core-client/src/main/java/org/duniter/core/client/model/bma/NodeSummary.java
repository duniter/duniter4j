package org.duniter.core.client.model.bma;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants
public class NodeSummary {

    private DuniterSoftware duniter;

    @Data
    @FieldNameConstants
    @Builder
    public static class DuniterSoftware {
        private String software;
        private String version;
        private Integer forkWindowSize;
    }
}
