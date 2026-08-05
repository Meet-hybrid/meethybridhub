package com.meethybridhub.store;

import com.meethybridhub.common.exception.BadRequestException;

import java.util.Optional;

/**
 * Holds the ID of the store that owns the current request (the "tenant").
 *
 * Populated by {@link StoreFilter} for every request and cleared afterwards.
 * Because it is a {@link ThreadLocal}, each request (and each thread serving
 * it) sees its own value — safe under virtual threads and thread pools.
 *
 * Service/repository code reads the tenant here instead of trusting client
 * input directly, so tenant resolution happens exactly once, at the filter.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> STORE_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static void setStoreId(Long storeId) {
        STORE_ID.set(storeId);
    }

    public static Optional<Long> getStoreId() {
        return Optional.ofNullable(STORE_ID.get());
    }

    /**
     * Like {@link #getStoreId()} but throws for endpoints that cannot work
     * without a store context (e.g. "my store" operations).
     */
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
