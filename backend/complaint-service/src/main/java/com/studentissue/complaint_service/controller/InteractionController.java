package com.studentissue.complaint_service.controller;

import com.studentissue.complaint_service.dto.CommentResponse;
import com.studentissue.complaint_service.dto.CreateCommentRequest;
import com.studentissue.complaint_service.dto.VoteSummary;
import com.studentissue.complaint_service.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/complaints/{complaintId}")
public class InteractionController {

    private final InteractionService service;

    public InteractionController(InteractionService service) {
        this.service = service;
    }

    @PostMapping("/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public VoteSummary vote(@PathVariable Long complaintId,
                            @RequestHeader("X-User-Id") Long userId) {
        return service.vote(complaintId, userId);
    }

    @DeleteMapping("/votes")
    public VoteSummary removeVote(@PathVariable Long complaintId,
                                  @RequestHeader("X-User-Id") Long userId) {
        return service.removeVote(complaintId, userId);
    }

    @GetMapping("/votes")
    public VoteSummary votes(@PathVariable Long complaintId,
                             @RequestHeader("X-User-Id") Long userId) {
        return service.getVotes(complaintId, userId);
    }

    @PostMapping("/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable Long complaintId,
                                      @RequestHeader("X-User-Id") Long userId,
                                      @RequestHeader(value = "X-User-Email", required = false) String email,
                                      @Valid @RequestBody CreateCommentRequest request) {
        return service.addComment(complaintId, userId, email, request);
    }

    @GetMapping("/comments")
    public List<CommentResponse> comments(@PathVariable Long complaintId) {
        return service.getComments(complaintId);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long complaintId,
                              @PathVariable Long commentId,
                              @RequestHeader("X-User-Id") Long userId,
                              @RequestHeader("X-User-Role") String role) {
        service.deleteComment(complaintId, commentId, userId, role);
    }
}