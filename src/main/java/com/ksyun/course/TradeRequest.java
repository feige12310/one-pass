package com.ksyun.course;

import java.math.BigDecimal;

public class TradeRequest {
    private Long sourceUid;
    private Long targetUid;
    private BigDecimal amount;

    public Long getSourceUid() {
        return sourceUid;
    }

    public Long getTargetUid() {
        return targetUid;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
