package com.studentissue.complaint_service.repository;

import com.studentissue.complaint_service.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ComplaintRepository
        extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);
}