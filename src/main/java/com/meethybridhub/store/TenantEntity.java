package com.meethybridhub.store;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;


@MappedSuperclass
public abstract class TenantEntity {

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }
}
