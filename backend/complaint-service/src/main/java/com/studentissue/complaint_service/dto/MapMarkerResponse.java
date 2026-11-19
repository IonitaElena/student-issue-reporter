package com.studentissue.complaint_service.dto;

public record MapMarkerResponse(Long id, String title, String category, String status,
                                String priority, Double latitude, Double longitude,
                                long voteCount, Double distanceMeters) {

    public static MapMarkerResponse from(MapMarker m) {
        return new MapMarkerResponse(m.getId(), m.getTitle(), m.getCategory(), m.getStatus(),
                m.getPriority(), m.getLatitude(), m.getLongitude(),
                m.getVoteCount() == null ? 0 : m.getVoteCount(), m.getDistanceMeters());
    }
}