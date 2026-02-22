package com.lenovo.smart_office_booking.api.dto;

import java.time.LocalDateTime;

import com.lenovo.smart_office_booking.domain.enums.RegistrationStatus;
import com.lenovo.smart_office_booking.domain.enums.RoleName;

public record UserRegistrationRequestResponse(
        Long id,
        String username,
        String displayName,
        String email,
        RoleName requestedRole,
        RegistrationStatus status,
        String reviewer,
        String reviewComment,
        LocalDateTime createdAt,
        LocalDateTime decidedAt
) {
}
