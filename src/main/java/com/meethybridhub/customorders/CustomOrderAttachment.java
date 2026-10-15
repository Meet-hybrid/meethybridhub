package com.meethybridhub.customorders;

import com.meethybridhub.identity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "custom_order_attachments")
@EntityListeners(AuditingEntityListener.class)
public class CustomOrderAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private CustomOrderRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private CustomOrderConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CustomOrderAttachment() {}

    public CustomOrderAttachment(CustomOrderRequest request, User uploader,
                                  String fileUrl, String fileName, String fileType, Long fileSizeBytes) {
        this.request = request;
        this.uploader = uploader;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
    }

    public Long getId() { return id; }
    public CustomOrderRequest getRequest() { return request; }
    public CustomOrderConversation getConversation() { return conversation; }
    public void setConversation(CustomOrderConversation conversation) { this.conversation = conversation; }
    public Long getUploaderId() { return uploader == null ? null : uploader.getId(); }
    public String getFileUrl() { return fileUrl; }
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
}
