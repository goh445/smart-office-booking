package com.lenovo.smart_office_booking.api.dto;

import java.time.LocalDateTime;

public record AiBookingActionResponse(
        String type,
        boolean success,
        String answer,
        Long bookingId,
        String resourceCode,
        String resourceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        String error
) {
}
