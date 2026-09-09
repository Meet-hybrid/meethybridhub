package com.meethybridhub.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


@Repository
public interface PlatformChargeRepository extends JpaRepository<PlatformCharge, Long> {


    boolean existsByTransactionRef(String transactionRef);


    List<PlatformCharge> findByTransactionRefIn(Collection<String> transactionRefs);

    long countByStatus(PlatformCharge.Status status);
}
