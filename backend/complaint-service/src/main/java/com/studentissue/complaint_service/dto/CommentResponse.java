package com.studentissue.complaint_service.dto;

import com.studentissue.complaint_service.model.Comment;

import java.time.LocalDateTime;

public record CommentResponse(Long id, Long complaintId, Long userId,
                              String authorEmail, String content, LocalDateTime createdAt) {

    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getComplaintId(), c.getUserId(),
                c.getAuthorEmail(), c.getContent(), c.getCreatedAt());
    }
}