package com.meethybridhub.billing;

import java.time.Instant;
import java.util.List;


public interface ChargeableTransactionSource {


    List<ChargeableTransaction> findSettledTransactionsBefore(Instant cutoff);
}
