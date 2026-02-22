package com.lenovo.smart_office_booking.api;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lenovo.smart_office_booking.api.dto.BookingResponse;
import com.lenovo.smart_office_booking.api.dto.CreateBookingRequest;
import com.lenovo.smart_office_booking.api.dto.UpdateBookingRequest;
import com.lenovo.smart_office_booking.domain.Booking;
import com.lenovo.smart_office_booking.repository.ApprovalRequestRepository;
import com.lenovo.smart_office_booking.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Validated
public class BookingApiController {

    private final BookingService bookingService;
        private final ApprovalRequestRepository approvalRequestRepository;

    /**
     * POST /api/bookings - Create a new booking
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Principal principal
    ) {
        String username = principal.getName();

        Booking booking = bookingService.createBooking(
                username,
                request.resourceId(),
                request.title(),
                request.startTime(),
                request.endTime(),
                request.notes()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toBookingResponse(booking, findRejectionComment(booking.getId())));
    }

    /**
     * GET /api/bookings - Get all bookings for current user
     */
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getUserBookings(Principal principal) {
        String username = principal.getName();

        List<Booking> userBookings = bookingService.getUserBookings(username);
        Map<Long, String> rejectionCommentsByBookingId = findRejectionComments(
                userBookings.stream().map(Booking::getId).toList()
        );

        List<BookingResponse> bookings = userBookings
                .stream()
                .map(booking -> toBookingResponse(booking, rejectionCommentsByBookingId.get(booking.getId())))
                .toList();

        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/bookings/{id} - Get booking by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        Booking booking = bookingService.getBookingById(id);
                return ResponseEntity.ok(toBookingResponse(booking, findRejectionComment(booking.getId())));
    }

    /**
     * POST /api/bookings/{id}/cancel - Cancel a booking
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            Principal principal
    ) {
        String username = principal.getName();
        Booking booking = bookingService.cancelBooking(id, username);
        return ResponseEntity.ok(toBookingResponse(booking, findRejectionComment(booking.getId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequest request,
            Principal principal
    ) {
        Booking booking = bookingService.updateBooking(
                id,
                principal.getName(),
                request.resourceId(),
                request.title(),
                request.startTime(),
                request.endTime(),
                request.notes()
        );
                return ResponseEntity.ok(toBookingResponse(booking, findRejectionComment(booking.getId())));
    }

    // Helper method to convert entity to DTO

        private BookingResponse toBookingResponse(Booking booking, String rejectionComment) {
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
                                rejectionComment,
                booking.getCreatedAt()
        );
    }

        private Map<Long, String> findRejectionComments(List<Long> bookingIds) {
                if (bookingIds == null || bookingIds.isEmpty()) {
                        return Collections.emptyMap();
                }

                return approvalRequestRepository.findByBookingIdIn(bookingIds)
                                .stream()
                                .filter(request -> request.getDecisionComment() != null && !request.getDecisionComment().isBlank())
                                .collect(Collectors.toMap(
                                                request -> request.getBooking().getId(),
                                                request -> request.getDecisionComment(),
                                                (existing, replacement) -> existing
                                ));
        }

        private String findRejectionComment(Long bookingId) {
                return findRejectionComments(List.of(bookingId)).get(bookingId);
        }
}
