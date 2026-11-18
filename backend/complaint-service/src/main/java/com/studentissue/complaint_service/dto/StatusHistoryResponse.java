package com.studentissue.complaint_service.dto;

import com.studentissue.complaint_service.model.StatusHistory;

import java.time.LocalDateTime;

public record StatusHistoryResponse(Long id, String oldStatus, String newStatus,
                                    Long changedBy, String comment, LocalDateTime changedAt) {

    public static StatusHistoryResponse from(StatusHistory h) {
        return new StatusHistoryResponse(
                h.getId(),
                h.getOldStatus() == null ? null : h.getOldStatus().name(),
                h.getNewStatus().name(),
                h.getChangedBy(), h.getComment(), h.getChangedAt());
    }
}