package com.meethybridhub.store;

/**
 * Lifecycle states of a {@link Store} tenant.
 */
public enum StoreStatus {
    PENDING,    // Created, awaiting activation (e.g. domain verification)
    ACTIVE,     // Live storefront
    SUSPENDED   // Temporarily blocked (policy/abuse)
}
