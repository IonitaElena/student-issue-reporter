package com.studentissue.complaint_service.controller;

import com.studentissue.complaint_service.dto.ComplaintResponse;
import com.studentissue.complaint_service.dto.CreateComplaintRequest;
import com.studentissue.complaint_service.dto.PageResponse;
import com.studentissue.complaint_service.model.Category;
import com.studentissue.complaint_service.model.Status;
import com.studentissue.complaint_service.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/complaints")
public class ComplaintController {

    private final ComplaintService service;

    public ComplaintController(ComplaintService service) {
        this.service = service;
    }

    // Headerele X-User-* sunt puse de API Gateway după validarea JWT-ului
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse create(@RequestHeader("X-User-Id") Long userId,
                                    @RequestHeader(value = "X-User-Email", required = false) String email,
                                    @Valid @RequestBody CreateComplaintRequest request) {
        return service.create(userId, email, request);
    }

    @GetMapping
    public PageResponse<ComplaintResponse> list(@RequestParam(required = false) Category category,
                                                @RequestParam(required = false) Status status,
                                                @RequestParam(required = false) String faculty,
                                                @RequestParam(required = false) String dormitory,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.search(category, status, faculty, dormitory, page, size);
    }

    @GetMapping("/my")
    public List<ComplaintResponse> mine(@RequestHeader("X-User-Id") Long userId) {
        return service.getMine(userId);
    }

    @GetMapping("/{id}")
    public ComplaintResponse get(@PathVariable Long id) {
        return service.getById(id);
    }
}