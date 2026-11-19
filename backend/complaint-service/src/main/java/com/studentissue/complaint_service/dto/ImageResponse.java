package com.studentissue.complaint_service.dto;

import com.studentissue.complaint_service.model.ComplaintImage;

import java.time.LocalDateTime;

public record ImageResponse(Long id, Long complaintId, String originalFilename,
                            String contentType, long sizeBytes,
                            LocalDateTime createdAt, String url) {

    public static ImageResponse from(ComplaintImage i) {
        return new ImageResponse(i.getId(), i.getComplaintId(), i.getOriginalFilename(),
                i.getContentType(), i.getSizeBytes(), i.getCreatedAt(),
                "/api/complaints/" + i.getComplaintId() + "/images/" + i.getId() + "/content");
    }
}