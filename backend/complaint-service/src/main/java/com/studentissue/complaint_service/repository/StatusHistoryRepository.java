package com.studentissue.complaint_service.repository;

import com.studentissue.complaint_service.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByComplaintIdOrderByChangedAtAsc(Long complaintId);
}