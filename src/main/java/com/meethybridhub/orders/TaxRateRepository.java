package com.meethybridhub.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaxRateRepository extends JpaRepository<TaxRate, String> {
    List<TaxRate> findByStoreId(String storeId);
    Optional<TaxRate> findFirstByStoreIdAndCountryAndStateOrderByStateDesc(String storeId, String country, String state);
}
