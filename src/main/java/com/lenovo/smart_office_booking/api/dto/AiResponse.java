package com.lenovo.smart_office_booking.api.dto;

public record AiResponse(
        String type,
        boolean success,
        String answer,
        String error
) {
}
