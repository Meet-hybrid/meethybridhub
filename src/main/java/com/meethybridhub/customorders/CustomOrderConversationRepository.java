package com.meethybridhub.customorders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomOrderConversationRepository extends JpaRepository<CustomOrderConversation, Long> {

    List<CustomOrderConversation> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
