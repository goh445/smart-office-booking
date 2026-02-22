package com.lenovo.smart_office_booking.api.dto;

import com.lenovo.smart_office_booking.domain.enums.ResourceType;

/**
 * Response DTO representing a Resource in API responses
 */
public record ResourceResponse(
        Long id,
        String code,
        String name,
        ResourceType type,
        String location,
        Integer capacity,
        String features,
        boolean hasDisplay,
        boolean hasVideoConference,
        boolean active
) {
}
