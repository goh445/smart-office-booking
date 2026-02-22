package com.lenovo.smart_office_booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lenovo.smart_office_booking.domain.AppUser;
import com.lenovo.smart_office_booking.domain.ApprovalRequest;
import com.lenovo.smart_office_booking.domain.Booking;
import com.lenovo.smart_office_booking.domain.enums.ApprovalStatus;
import com.lenovo.smart_office_booking.domain.enums.BookingStatus;
import com.lenovo.smart_office_booking.repository.AppUserRepository;
import com.lenovo.smart_office_booking.repository.ApprovalRequestRepository;
import com.lenovo.smart_office_booking.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final BookingRepository bookingRepository;
    private final AppUserRepository appUserRepository;

    public ApprovalRequest createApprovalRequestForBooking(Booking booking, String reason) {
        if (!booking.isRequiresApproval()) {
            throw new IllegalArgumentException("Booking does not require approval");
        }

        ApprovalRequest request = new ApprovalRequest();
        request.setBooking(booking);
        request.setStatus(ApprovalStatus.PENDING);
        request.setReason(reason);

        return approvalRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> getPendingRequests() {
        return approvalRequestRepository.findDetailedByStatus(ApprovalStatus.PENDING);
    }

    public ApprovalRequest approve(Long approvalRequestId, String approverUsername, String comment) {
        ApprovalRequest request = approvalRequestRepository.findDetailedById(approvalRequestId)
                .orElseThrow(() -> new IllegalArgumentException("审批请求不存在: " + approvalRequestId));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("审批请求已处理");
        }

        Booking booking = request.getBooking();
        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("预订状态不允许审批: " + booking.getStatus());
        }

        AppUser approver = appUserRepository.findByUsername(approverUsername)
                .orElseThrow(() -> new IllegalArgumentException("审批人不存在: " + approverUsername));

        request.setApprover(approver);
        request.setStatus(ApprovalStatus.APPROVED);
        request.setDecisionComment(comment);
        request.setDecidedAt(LocalDateTime.now());

        booking.setStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        approvalRequestRepository.save(request);
        return approvalRequestRepository.findDetailedById(approvalRequestId)
            .orElseThrow(() -> new IllegalArgumentException("审批请求不存在: " + approvalRequestId));
    }

    public ApprovalRequest reject(Long approvalRequestId, String approverUsername, String comment) {
        ApprovalRequest request = approvalRequestRepository.findDetailedById(approvalRequestId)
                .orElseThrow(() -> new IllegalArgumentException("审批请求不存在: " + approvalRequestId));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("审批请求已处理");
        }

        Booking booking = request.getBooking();
        if (booking.getStatus() != BookingStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("预订状态不允许审批: " + booking.getStatus());
        }

        AppUser approver = appUserRepository.findByUsername(approverUsername)
                .orElseThrow(() -> new IllegalArgumentException("审批人不存在: " + approverUsername));

        request.setApprover(approver);
        request.setStatus(ApprovalStatus.REJECTED);
        request.setDecisionComment(comment);
        request.setDecidedAt(LocalDateTime.now());

        booking.setStatus(BookingStatus.REJECTED);
        bookingRepository.save(booking);

        approvalRequestRepository.save(request);
        return approvalRequestRepository.findDetailedById(approvalRequestId)
            .orElseThrow(() -> new IllegalArgumentException("审批请求不存在: " + approvalRequestId));
    }
}
