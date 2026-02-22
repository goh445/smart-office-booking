package com.lenovo.smart_office_booking.api.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a booking
 */
public record CreateBookingRequest(
        @NotNull(message = "资源ID不能为空")
        Long resourceId,

        @NotBlank(message = "标题不能为空")
        String title,

        @NotNull(message = "开始时间不能为空")
        @Future(message = "开始时间必须在未来")
        LocalDateTime startTime,

        @NotNull(message = "结束时间不能为空")
        LocalDateTime endTime,

        String notes
) {
}
