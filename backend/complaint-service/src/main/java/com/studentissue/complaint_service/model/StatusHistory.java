package com.studentissue.complaint_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", nullable = false)
    private Long complaintId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private Status oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private Status newStatus;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    private String comment;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void onCreate() { changedAt = LocalDateTime.now(); }
}