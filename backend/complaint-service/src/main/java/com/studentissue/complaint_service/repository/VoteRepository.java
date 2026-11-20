package com.studentissue.complaint_service.repository;

import com.studentissue.complaint_service.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByComplaintIdAndUserId(Long complaintId, Long userId);
    long countByComplaintId(Long complaintId);
    long deleteByComplaintIdAndUserId(Long complaintId, Long userId);

    interface VoteCount {
        Long getComplaintId();
        Long getCnt();
    }

    @Query("select v.complaintId as complaintId, count(v) as cnt from Vote v " +
           "where v.complaintId in :ids group by v.complaintId")
    List<VoteCount> countByComplaintIds(@Param("ids") Collection<Long> ids);

    @Query("select v.complaintId from Vote v where v.userId = :userId and v.complaintId in :ids")
    List<Long> findVotedComplaintIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}