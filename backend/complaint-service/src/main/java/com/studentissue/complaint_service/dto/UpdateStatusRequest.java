package com.studentissue.complaint_service.dto;

import com.studentissue.complaint_service.model.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
        @NotNull Status status,
        @Size(max = 500) String comment,
        @Size(max = 100) String assignedDepartment) {
}