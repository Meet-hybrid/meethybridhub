package com.meethybridhub.store;

import com.meethybridhub.common.exception.BadRequestException;

import java.util.Optional;


public final class TenantContext {

    private static final ThreadLocal<Long> STORE_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static void setStoreId(Long storeId) {
        STORE_ID.set(storeId);
    }

    public static Optional<Long> getStoreId() {
        return Optional.ofNullable(STORE_ID.get());
    }


    public static long requireStoreId() {
        Long storeId = STORE_ID.get();
        if (storeId == null) {
            throw new BadRequestException(
                    "No store context. Send an X-Store-Id header or access through your store subdomain.");
        }
        return storeId;
    }

    public static void clear() {
        STORE_ID.remove();
    }
}
