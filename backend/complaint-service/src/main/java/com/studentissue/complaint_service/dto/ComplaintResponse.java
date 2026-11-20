package com.studentissue.complaint_service.dto;

import com.studentissue.complaint_service.model.Complaint;

import java.time.LocalDateTime;

public record ComplaintResponse(
        Long id,
        String title,
        String description,
        String category,
        String status,
        String priority,
        Long userId,
        String faculty,
        String dormitory,
        String building,
        String locationText,
        Double latitude,
        Double longitude,
        String assignedDepartment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        long voteCount,
        boolean votedByMe) {

    /** Fără informații despre voturi (create, updateStatus). */
    public static ComplaintResponse from(Complaint c) {
        return from(c, 0, false);
    }

    public static ComplaintResponse from(Complaint c, long voteCount, boolean votedByMe) {
        return new ComplaintResponse(
                c.getId(), c.getTitle(), c.getDescription(),
                c.getCategory().name(), c.getStatus().name(), c.getPriority().name(),
                c.getUserId(), c.getFaculty(), c.getDormitory(), c.getBuilding(),
                c.getLocationText(), c.getLatitude(), c.getLongitude(),
                c.getAssignedDepartment(), c.getCreatedAt(), c.getUpdatedAt(), c.getResolvedAt(),
                voteCount, votedByMe);
    }
}