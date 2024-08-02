package com.ksyun.course;

import java.util.List;

public class BatchPayRequest {
    private String batchPayId;
    private List<Long> uids;

    public void setBatchPayId(String batchPayId) {
        this.batchPayId = batchPayId;
    }

    public void setUids(List<Long> uids) {
        this.uids = uids;
    }

    public String getBatchPayId() {
        return batchPayId;
    }

    public List<Long> getUids() {
        return uids;
    }
public BatchPayRequest() {}
    public BatchPayRequest(String batchPayId, List<Long> uids) {
        this.batchPayId = batchPayId;
        this.uids = uids;
    }
    // getters and setters
}