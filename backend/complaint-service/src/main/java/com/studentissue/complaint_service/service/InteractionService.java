package com.studentissue.complaint_service.service;

import com.studentissue.complaint_service.dto.CommentResponse;
import com.studentissue.complaint_service.dto.CreateCommentRequest;
import com.studentissue.complaint_service.dto.VoteSummary;
import com.studentissue.complaint_service.model.Comment;
import com.studentissue.complaint_service.model.Complaint;
import com.studentissue.complaint_service.model.Vote;
import com.studentissue.complaint_service.repository.CommentRepository;
import com.studentissue.complaint_service.repository.ComplaintRepository;
import com.studentissue.complaint_service.repository.VoteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InteractionService {

    private final ComplaintRepository complaintRepository;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;

    public InteractionService(ComplaintRepository complaintRepository,
                              VoteRepository voteRepository,
                              CommentRepository commentRepository) {
        this.complaintRepository = complaintRepository;
        this.voteRepository = voteRepository;
        this.commentRepository = commentRepository;
    }

    // ---------- voturi ----------

    @Transactional
    public VoteSummary vote(Long complaintId, Long userId) {
        Complaint complaint = findComplaint(complaintId);

        if (complaint.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu poți vota propria sesizare");
        }
        if (voteRepository.existsByComplaintIdAndUserId(complaintId, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ai votat deja această sesizare");
        }
        try {
            voteRepository.saveAndFlush(Vote.builder().complaintId(complaintId).userId(userId).build());
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ai votat deja această sesizare");
        }
        return summary(complaintId, userId);
    }

    @Transactional
    public VoteSummary removeVote(Long complaintId, Long userId) {
        findComplaint(complaintId);
        if (voteRepository.deleteByComplaintIdAndUserId(complaintId, userId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nu ai votat această sesizare");
        }
        return summary(complaintId, userId);
    }

    public VoteSummary getVotes(Long complaintId, Long userId) {
        findComplaint(complaintId);
        return summary(complaintId, userId);
    }

    // ---------- comentarii ----------

    @Transactional
    public CommentResponse addComment(Long complaintId, Long userId, String email, CreateCommentRequest req) {
        findComplaint(complaintId);
        Comment comment = Comment.builder()
                .complaintId(complaintId)
                .userId(userId)
                .authorEmail(email == null ? "necunoscut" : email)
                .content(req.content().trim())
                .build();
        return CommentResponse.from(commentRepository.save(comment));
    }

    public List<CommentResponse> getComments(Long complaintId) {
        findComplaint(complaintId);
        return commentRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
                .map(CommentResponse::from).toList();
    }

    @Transactional
    public void deleteComment(Long complaintId, Long commentId, Long userId, String role) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> c.getComplaintId().equals(complaintId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentariu inexistent"));

        if (!comment.getUserId().equals(userId) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Poți șterge doar propriile comentarii");
        }
        commentRepository.delete(comment);
    }

    // ---------- ajutatoare ----------

    private Complaint findComplaint(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesizare inexistentă"));
    }

    private VoteSummary summary(Long complaintId, Long userId) {
        return new VoteSummary(voteRepository.countByComplaintId(complaintId),
                voteRepository.existsByComplaintIdAndUserId(complaintId, userId));
    }
}