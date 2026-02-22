package com.lenovo.smart_office_booking.api.dto;

import java.time.LocalDateTime;

import com.lenovo.smart_office_booking.domain.enums.BookingStatus;

/**
 * Response DTO representing a Booking in API responses
 */
public record BookingResponse(
        Long id,
        String requesterDisplayName,
        Long resourceId,
        String resourceCode,
        String resourceName,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status,
        boolean requiresApproval,
        String notes,
        String rejectionComment,
        LocalDateTime createdAt
) {
}
