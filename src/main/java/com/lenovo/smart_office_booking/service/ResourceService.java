package com.lenovo.smart_office_booking.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lenovo.smart_office_booking.domain.Booking;
import com.lenovo.smart_office_booking.domain.Resource;
import com.lenovo.smart_office_booking.domain.enums.BookingStatus;
import com.lenovo.smart_office_booking.domain.enums.ResourceType;
import com.lenovo.smart_office_booking.repository.BookingRepository;
import com.lenovo.smart_office_booking.repository.ResourceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;

    /**
     * Get all active resources
     */
    public List<Resource> getAllActiveResources() {
        return resourceRepository.findAll()
                .stream()
                .filter(Resource::isActive)
                .toList();
    }

    public Page<Resource> searchActiveResources(
            ResourceType type,
            String location,
            Integer minCapacity,
            Integer maxCapacity,
            String search,
            Pageable pageable
    ) {
        Specification<Resource> spec = (root, query, cb) -> cb.isTrue(root.get("active"));

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }

        if (location != null && !location.isBlank()) {
            String locationLike = "%" + location.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("location")), locationLike));
        }

        if (minCapacity != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("capacity"), minCapacity));
        }

        if (maxCapacity != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("capacity"), maxCapacity));
        }

        if (search != null && !search.isBlank()) {
            String keyword = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("code")), keyword),
                    cb.like(cb.lower(root.get("name")), keyword),
                    cb.like(cb.lower(root.get("location")), keyword),
                    cb.like(cb.lower(root.get("features")), keyword)
            ));
        }

        return resourceRepository.findAll(spec, pageable);
    }

    /**
     * Get resource by ID
     */
    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with id: " + id));
    }

    /**
     * Get resource by code
     */
    public Resource getResourceByCode(String code) {
        return resourceRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with code: " + code));
    }

    /**
     * Check if resource is available for a time range
     * Returns true if no overlapping bookings exist
     */
    public boolean isResourceAvailable(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new IllegalArgumentException("Invalid time range: start must be before end");
        }

        List<BookingStatus> activeStatuses = List.of(
            BookingStatus.PENDING_APPROVAL,
            BookingStatus.APPROVED
        );

        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                resourceId,
                startTime,
                endTime,
                activeStatuses
        );

        return overlappingBookings.isEmpty();
    }

    /**
     * Get overlapping bookings for a resource in a time range
     */
    public List<Booking> getOverlappingBookings(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        List<BookingStatus> activeStatuses = List.of(
            BookingStatus.PENDING_APPROVAL,
            BookingStatus.APPROVED
        );

        return bookingRepository.findOverlappingBookings(
                resourceId,
                startTime,
                endTime,
                activeStatuses
        );
    }
}
