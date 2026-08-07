package com.meethybridhub.billing;

import java.time.Instant;
import java.util.List;

/**
 * Seam between the platform-charge module and the (future) Orders/Payments
 * module.
 *
 * The dormant sweep iterates every registered implementation to find settled
 * transactions that have not yet been charged. No implementation exists yet —
 * {@link PlatformChargeService} injects an empty list and the sweep no-ops —
 * so implementing this interface is the ONLY step needed to wake the nightly
 * sweep once orders exist.
 */
public interface ChargeableTransactionSource {

    /** Settled transactions that closed before {@code cutoff} and are not yet charged. */
    List<ChargeableTransaction> findSettledTransactionsBefore(Instant cutoff);
}
