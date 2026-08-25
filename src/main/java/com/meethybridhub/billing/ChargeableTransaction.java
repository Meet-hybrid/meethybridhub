package com.meethybridhub.billing;

import java.math.BigDecimal;

/**
 * A settled transaction that may be subject to a platform charge, as reported
 * by a {@link ChargeableTransactionSource}.
 *
 * @param transactionRef stable unique reference — the idempotency key
 * @param amount         the settled amount the flat fee is charged against
 */
public record ChargeableTransaction(String transactionRef, BigDecimal amount) {
}
