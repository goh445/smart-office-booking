package com.lenovo.smart_office_booking.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AiPromptRequest(
        @NotBlank(message = "prompt 不能为空")
        String prompt
) {
}
