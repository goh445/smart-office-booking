package com.lenovo.smart_office_booking.api;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lenovo.smart_office_booking.api.dto.AvailabilityResponse;
import com.lenovo.smart_office_booking.api.dto.BookingResponse;
import com.lenovo.smart_office_booking.api.dto.ResourceResponse;
import com.lenovo.smart_office_booking.domain.Booking;
import com.lenovo.smart_office_booking.domain.Resource;
import com.lenovo.smart_office_booking.domain.enums.ResourceType;
import com.lenovo.smart_office_booking.service.ResourceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceApiController {

    private final ResourceService resourceService;

    /**
     * GET /api/resources - List all active resources
     */
    @GetMapping
        public ResponseEntity<Page<ResourceResponse>> getAllResources(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) Integer maxCapacity,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
        ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Page<ResourceResponse> resources = resourceService.searchActiveResources(
                type,
                location,
                minCapacity,
                maxCapacity,
                search,
                PageRequest.of(safePage, safeSize)
            )
            .map(this::toResourceResponse);

        return ResponseEntity.ok(resources);
    }

    /**
     * GET /api/resources/{id} - Get resource details by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Long id) {
        Resource resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(toResourceResponse(resource));
    }

    /**
     * GET /api/resources/{id}/availability - Check resource availability for a time range
     *
     * Query parameters:
     * - start: Start time in ISO-8601 format (e.g., 2025-02-15T09:00:00)
     * - end: End time in ISO-8601 format
     */
    @GetMapping("/{id}/availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        Resource resource = resourceService.getResourceById(id);
        boolean available = resourceService.isResourceAvailable(id, start, end);
        
        List<BookingResponse> overlappingBookings = resourceService.getOverlappingBookings(id, start, end)
                .stream()
                .map(this::toBookingResponse)
                .toList();

        AvailabilityResponse response = new AvailabilityResponse(
                resource.getId(),
                resource.getCode(),
                resource.getName(),
                available,
                overlappingBookings
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/resources/{id}/booked-slots - List booked slots for a specific date
     *
     * Query parameters:
     * - date: Date in ISO format (e.g., 2026-02-18)
     */
    @GetMapping("/{id}/booked-slots")
    public ResponseEntity<List<BookingResponse>> getBookedSlotsByDate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        resourceService.getResourceById(id);

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<BookingResponse> bookings = resourceService.getOverlappingBookings(id, dayStart, dayEnd)
                .stream()
                .map(this::toBookingResponse)
                .toList();

        return ResponseEntity.ok(bookings);
    }

    // Helper methods to convert entities to DTOs

    private ResourceResponse toResourceResponse(Resource resource) {
        String normalizedFeatures = resource.getFeatures() == null ? "" : resource.getFeatures().toLowerCase();
        boolean hasDisplay = normalizedFeatures.contains("display")
            || normalizedFeatures.contains("monitor")
            || normalizedFeatures.contains("projector");
        boolean hasVideoConference = normalizedFeatures.contains("video conference")
            || normalizedFeatures.contains("camera")
            || normalizedFeatures.contains("telepresence");

        return new ResourceResponse(
                resource.getId(),
                resource.getCode(),
                resource.getName(),
                resource.getType(),
                resource.getLocation(),
                resource.getCapacity(),
                resource.getFeatures(),
            hasDisplay,
            hasVideoConference,
                resource.isActive()
        );
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getRequester().getDisplayName(),
                booking.getResource().getId(),
                booking.getResource().getCode(),
                booking.getResource().getName(),
                booking.getTitle(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus(),
                booking.isRequiresApproval(),
                booking.getNotes(),
                null,
                booking.getCreatedAt()
        );
    }
}
