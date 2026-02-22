package com.lenovo.smart_office_booking.api.dto;

import java.util.List;

/**
 * Response DTO for resource availability check
 */
public record AvailabilityResponse(
        Long resourceId,
        String resourceCode,
        String resourceName,
        boolean available,
        List<BookingResponse> overlappingBookings
) {
}
