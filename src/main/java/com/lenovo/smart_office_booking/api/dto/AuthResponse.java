package com.lenovo.smart_office_booking.api.dto;

import java.util.Set;

public record AuthResponse(
        String username,
        String displayName,
        Set<String> roles,
        boolean authenticated,
        String message
) {
}