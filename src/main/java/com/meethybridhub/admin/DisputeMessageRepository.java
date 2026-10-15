package com.meethybridhub.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeMessageRepository extends JpaRepository<DisputeMessage, Long> {

    List<DisputeMessage> findByDisputeIdOrderByCreatedAtAsc(Long disputeId);
}
