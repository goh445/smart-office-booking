package com.lenovo.smart_office_booking.api.dto;

import java.time.LocalDateTime;

import com.lenovo.smart_office_booking.domain.enums.ApprovalStatus;
import com.lenovo.smart_office_booking.domain.enums.BookingStatus;

public record ApprovalQueueItemResponse(
        Long approvalRequestId,
        Long bookingId,
        String requesterUsername,
        String requesterDisplayName,
        String resourceCode,
        String resourceName,
        String bookingTitle,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String reason,
        ApprovalStatus approvalStatus,
        BookingStatus bookingStatus,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        String approverUsername,
        String decisionComment
) {
}
