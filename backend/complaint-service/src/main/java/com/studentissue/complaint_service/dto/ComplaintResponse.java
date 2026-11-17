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
        LocalDateTime resolvedAt) {

    public static ComplaintResponse from(Complaint c) {
        return new ComplaintResponse(
                c.getId(), c.getTitle(), c.getDescription(),
                c.getCategory().name(), c.getStatus().name(), c.getPriority().name(),
                c.getUserId(), c.getFaculty(), c.getDormitory(), c.getBuilding(),
                c.getLocationText(), c.getLatitude(), c.getLongitude(),
                c.getAssignedDepartment(), c.getCreatedAt(), c.getUpdatedAt(), c.getResolvedAt());
    }
}