package com.lenovo.smart_office_booking.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lenovo.smart_office_booking.domain.AppUser;
import com.lenovo.smart_office_booking.domain.Booking;
import com.lenovo.smart_office_booking.domain.Resource;
import com.lenovo.smart_office_booking.domain.enums.ApprovalStatus;
import com.lenovo.smart_office_booking.domain.enums.BookingStatus;
import com.lenovo.smart_office_booking.repository.AppUserRepository;
import com.lenovo.smart_office_booking.repository.ApprovalRequestRepository;
import com.lenovo.smart_office_booking.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AppUserRepository appUserRepository;
    private final ResourceService resourceService;
    private final ApprovalService approvalService;
    private final ApprovalRequestRepository approvalRequestRepository;

    @Value("${app.approval.duration-hours-threshold}")
    private int approvalDurationThresholdHours;

    @Value("${app.approval.capacity-threshold}")
    private int approvalCapacityThreshold;

    /**
     * Create a new booking
     * Automatically determines if approval is required based on thresholds
     */
    public Booking createBooking(
            String username,
            Long resourceId,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String notes
    ) {
        // Validate inputs
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("开始时间不能早于当前时间");
        }

        // Load requester and resource
        AppUser requester = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));

        Resource resource = resourceService.getResourceById(resourceId);

        // Check availability
        if (!resourceService.isResourceAvailable(resourceId, startTime, endTime)) {
            throw new IllegalStateException("该时间段资源已被预订");
        }

        // Determine if approval is required
        boolean requiresApproval = determineIfApprovalRequired(startTime, endTime, resource.getCapacity());

        // Create booking
        Booking booking = new Booking();
        booking.setRequester(requester);
        booking.setResource(resource);
        booking.setTitle(title);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setNotes(notes);
        booking.setRequiresApproval(requiresApproval);

        // Set initial status
        if (requiresApproval) {
            booking.setStatus(BookingStatus.PENDING_APPROVAL);
        } else {
            booking.setStatus(BookingStatus.APPROVED);
        }

        Booking savedBooking = bookingRepository.save(booking);

        if (requiresApproval) {
            approvalService.createApprovalRequestForBooking(
                    savedBooking,
                    buildApprovalReason(startTime, endTime, resource.getCapacity())
            );
        }

        return savedBooking;
    }

    /**
     * Get all bookings for a specific user
     */
    public List<Booking> getUserBookings(String username) {
        appUserRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + username));

        return bookingRepository.findByRequesterUsernameDetailed(username);
    }

    /**
     * Get booking by ID
     */
    public Booking getBookingById(Long id) {
        return bookingRepository.findDetailedById(id)
            .orElseThrow(() -> new IllegalArgumentException("预订不存在: " + id));
    }

    public Booking updateBooking(
            Long bookingId,
            String username,
            Long resourceId,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String notes
    ) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new IllegalArgumentException("开始时间必须早于结束时间");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("开始时间不能早于当前时间");
        }

        Booking booking = getBookingById(bookingId);

        if (!booking.getRequester().getUsername().equals(username)) {
            throw new IllegalStateException("只能修改自己的预订");
        }

        assertFutureManageable(booking, "修改");

        Resource resource = resourceService.getResourceById(resourceId);

        if (hasConflictWithOtherBookings(bookingId, resourceId, startTime, endTime)) {
            throw new IllegalStateException("该时间段资源已被预订");
        }

        boolean requiresApproval = determineIfApprovalRequired(startTime, endTime, resource.getCapacity());

        booking.setResource(resource);
        booking.setTitle(title);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setNotes(notes);
        booking.setRequiresApproval(requiresApproval);
        booking.setStatus(requiresApproval ? BookingStatus.PENDING_APPROVAL : BookingStatus.APPROVED);

        approvalRequestRepository.findByBookingId(bookingId)
                .ifPresent(request -> {
                    request.setStatus(ApprovalStatus.PENDING);
                    request.setDecisionComment(null);
                    request.setDecidedAt(null);
                    request.setApprover(null);
                    request.setReason(buildApprovalReason(startTime, endTime, resource.getCapacity()));
                    approvalRequestRepository.save(request);
                });

        if (requiresApproval && approvalRequestRepository.findByBookingId(bookingId).isEmpty()) {
            approvalService.createApprovalRequestForBooking(
                    booking,
                    buildApprovalReason(startTime, endTime, resource.getCapacity())
            );
        }

        return bookingRepository.save(booking);
    }

    /**
     * Cancel a booking
     * Only the requester can cancel their own booking
     */
    public Booking cancelBooking(Long bookingId, String username) {
        Booking booking = getBookingById(bookingId);

        // Verify ownership
        if (!booking.getRequester().getUsername().equals(username)) {
            throw new IllegalStateException("只能取消自己的预订");
        }

        assertFutureManageable(booking, "取消");

        booking.setStatus(BookingStatus.CANCELLED);

        approvalRequestRepository.findByBookingId(booking.getId())
                .filter(request -> request.getStatus() == ApprovalStatus.PENDING)
                .ifPresent(request -> {
                    request.setStatus(ApprovalStatus.REJECTED);
                    request.setDecisionComment("申请人已取消预订");
                    request.setDecidedAt(LocalDateTime.now());
                    approvalRequestRepository.save(request);
                });

        return bookingRepository.save(booking);
    }

    private void assertFutureManageable(Booking booking, String action) {
        if (!booking.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("仅能" + action + "未来预约");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            throw new IllegalStateException("当前状态不允许" + action + ": " + booking.getStatus());
        }
    }

    private boolean hasConflictWithOtherBookings(
            Long bookingId,
            Long resourceId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING_APPROVAL,
                BookingStatus.APPROVED
        );

        return bookingRepository.findOverlappingBookings(resourceId, startTime, endTime, activeStatuses)
                .stream()
                .anyMatch(existing -> !existing.getId().equals(bookingId));
    }

    /**
     * Determine if approval is required based on business rules:
     * 1. Duration exceeds threshold (e.g., 4 hours)
     * 2. Resource capacity exceeds threshold (e.g., 10 people)
     */
    private boolean determineIfApprovalRequired(LocalDateTime startTime, LocalDateTime endTime, Integer capacity) {
        // Calculate duration in hours
        Duration duration = Duration.between(startTime, endTime);
        long durationHours = duration.toHours();

        // Check thresholds
        boolean durationExceeded = durationHours > approvalDurationThresholdHours;
        boolean capacityExceeded = capacity != null && capacity > approvalCapacityThreshold;

        return durationExceeded || capacityExceeded;
    }

    private String buildApprovalReason(LocalDateTime startTime, LocalDateTime endTime, Integer capacity) {
        Duration duration = Duration.between(startTime, endTime);
        long durationHours = duration.toHours();

        boolean durationExceeded = durationHours > approvalDurationThresholdHours;
        boolean capacityExceeded = capacity != null && capacity > approvalCapacityThreshold;

        if (durationExceeded && capacityExceeded) {
            return "预订时长超过" + approvalDurationThresholdHours + "小时，且资源容量超过" + approvalCapacityThreshold + "人";
        }
        if (durationExceeded) {
            return "预订时长超过" + approvalDurationThresholdHours + "小时";
        }
        if (capacityExceeded) {
            return "资源容量超过" + approvalCapacityThreshold + "人";
        }
        return "命中审批规则";
    }
}
