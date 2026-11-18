package com.studentissue.complaint_service.repository;

import com.studentissue.complaint_service.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByComplaintIdAndUserId(Long complaintId, Long userId);
    long countByComplaintId(Long complaintId);
    long deleteByComplaintIdAndUserId(Long complaintId, Long userId);
}