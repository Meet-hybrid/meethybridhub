package com.meethybridhub.billing;

import java.math.BigDecimal;


public record ChargeableTransaction(String transactionRef, BigDecimal amount) {
}
