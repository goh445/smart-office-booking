package com.lenovo.smart_office_booking.api.dto;

import java.time.LocalDateTime;

import com.lenovo.smart_office_booking.domain.enums.ResourceType;

public record AiBookingPreviewResponse(
        String type,
        boolean success,
        String answer,
        String title,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ResourceType resourceType,
        Integer minCapacity,
        String notes,
        Long resourceId,
        String resourceCode,
        String resourceName,
        String error
) {
}
