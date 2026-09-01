package com.meethybridhub.customorders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomOrderAttachmentRepository extends JpaRepository<CustomOrderAttachment, Long> {

    List<CustomOrderAttachment> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
