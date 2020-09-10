package com.sahajamit.k8s.domain;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(includeFieldNames = true)
public class GridConsoleStatus {
    private int availableNodesCount;
    private int busyNodesCount;
    private int waitingRequestsCount;
}
