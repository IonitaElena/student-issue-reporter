package com.studentissue.complaint_service.dto;

public interface MapMarker {
    Long getId();
    String getTitle();
    String getCategory();
    String getStatus();
    String getPriority();
    Double getLatitude();
    Double getLongitude();
    Long getVoteCount();
    Double getDistanceMeters();
}