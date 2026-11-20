package com.studentissue.complaint_service.dto;

public record SimilarComplaintResponse(Long id, String title, String status,
                                       double similarity, Integer distanceMeters) {
}