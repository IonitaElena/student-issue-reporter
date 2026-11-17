package com.studentissue.complaint_service.service;

import com.studentissue.complaint_service.dto.ComplaintResponse;
import com.studentissue.complaint_service.dto.CreateComplaintRequest;
import com.studentissue.complaint_service.dto.PageResponse;
import com.studentissue.complaint_service.model.*;
import com.studentissue.complaint_service.repository.ComplaintRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ComplaintService {

    // nume@stud.acs.upb.ro -> "acs"
    private static final Pattern STUDENT_EMAIL = Pattern.compile("^[^@]+@stud\\.([a-z]+)\\.upb\\.ro$");

    private final ComplaintRepository repository;

    public ComplaintService(ComplaintRepository repository) {
        this.repository = repository;
    }

    public ComplaintResponse create(Long userId, String email, CreateComplaintRequest req) {
        Complaint complaint = Complaint.builder()
                .title(req.title().trim())
                .description(req.description().trim())
                .category(req.category())
                .status(Status.NEW)
                .priority(Priority.MEDIUM)   // mai târziu o estimează AI Service
                .userId(userId)
                .faculty(extractFaculty(email))
                .dormitory(req.dormitory())
                .building(req.building())
                .locationText(req.locationText())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .build();
        return ComplaintResponse.from(repository.save(complaint));
    }

    public PageResponse<ComplaintResponse> search(Category category, Status status, String faculty,
                                                  String dormitory, int page, int size) {
        Specification<Complaint> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (faculty != null && !faculty.isBlank())
                predicates.add(cb.equal(root.get("faculty"), faculty.toLowerCase()));
            if (dormitory != null && !dormitory.isBlank())
                predicates.add(cb.equal(root.get("dormitory"), dormitory));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Complaint> result = repository.findAll(spec, pageRequest);

        return new PageResponse<>(
                result.getContent().stream().map(ComplaintResponse::from).toList(),
                result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    public ComplaintResponse getById(Long id) {
        return ComplaintResponse.from(repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesizare inexistentă")));
    }

    public List<ComplaintResponse> getMine(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ComplaintResponse::from).toList();
    }

    private String extractFaculty(String email) {
        if (email == null) return null;
        Matcher m = STUDENT_EMAIL.matcher(email.toLowerCase());
        return m.matches() ? m.group(1) : null;
    }
}