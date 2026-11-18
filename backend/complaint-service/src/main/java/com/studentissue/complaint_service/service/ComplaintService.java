package com.studentissue.complaint_service.service;

import com.studentissue.complaint_service.dto.*;
import com.studentissue.complaint_service.model.*;
import com.studentissue.complaint_service.repository.ComplaintRepository;
import com.studentissue.complaint_service.repository.StatusHistoryRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ComplaintService {

    // nume@stud.acs.upb.ro -> "acs"
    private static final Pattern STUDENT_EMAIL = Pattern.compile("^[^@]+@stud\\.([a-z]+)\\.upb\\.ro$");

    private final ComplaintRepository repository;
    private final StatusHistoryRepository historyRepository;

    public ComplaintService(ComplaintRepository repository, StatusHistoryRepository historyRepository) {
        this.repository = repository;
        this.historyRepository = historyRepository;
    }

    @Transactional
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
        complaint = repository.saveAndFlush(complaint);

        historyRepository.save(StatusHistory.builder()
                .complaintId(complaint.getId())
                .oldStatus(null)
                .newStatus(Status.NEW)
                .changedBy(userId)
                .comment("Sesizare creată")
                .build());

        return ComplaintResponse.from(complaint);
    }

    @Transactional
    public ComplaintResponse updateStatus(Long id, Long adminId, String role, UpdateStatusRequest req) {
        requireAdmin(role);

        Complaint complaint = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesizare inexistentă"));

        Status oldStatus = complaint.getStatus();
        boolean statusChanged = oldStatus != req.status();
        boolean departmentGiven = req.assignedDepartment() != null && !req.assignedDepartment().isBlank();

        if (!statusChanged && !departmentGiven) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sesizarea are deja acest status și nu s-a indicat un departament");
        }

        if (departmentGiven) {
            complaint.setAssignedDepartment(req.assignedDepartment().trim());
        }

        if (statusChanged) {
            complaint.setStatus(req.status());
            complaint.setResolvedAt(req.status() == Status.RESOLVED ? LocalDateTime.now() : null);
        }

        complaint = repository.saveAndFlush(complaint);

        historyRepository.save(StatusHistory.builder()
                .complaintId(complaint.getId())
                .oldStatus(oldStatus)
                .newStatus(complaint.getStatus())
                .changedBy(adminId)
                .comment(buildComment(req, statusChanged, departmentGiven))
                .build());

        return ComplaintResponse.from(complaint);
    }

    public List<StatusHistoryResponse> getHistory(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesizare inexistentă");
        }
        return historyRepository.findByComplaintIdOrderByChangedAtAsc(id).stream()
                .map(StatusHistoryResponse::from).toList();
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

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Doar administratorii pot modifica statusul sesizărilor");
        }
    }

    private String buildComment(UpdateStatusRequest req, boolean statusChanged, boolean departmentGiven) {
        StringBuilder sb = new StringBuilder();
        if (req.comment() != null && !req.comment().isBlank()) {
            sb.append(req.comment().trim());
        }
        if (departmentGiven) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Departament: ").append(req.assignedDepartment().trim());
        }
        String text = sb.toString();
        return text.length() > 500 ? text.substring(0, 500) : (text.isEmpty() ? null : text);
    }

    private String extractFaculty(String email) {
        if (email == null) return null;
        Matcher m = STUDENT_EMAIL.matcher(email.toLowerCase());
        return m.matches() ? m.group(1) : null;
    }
}