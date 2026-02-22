package com.lenovo.smart_office_booking.api.dto;

import java.util.Set;

public record UserProfileResponse(
        Long id,
        String username,
        String displayName,
        String email,
        Set<String> roles,
        boolean enabled
) {
}