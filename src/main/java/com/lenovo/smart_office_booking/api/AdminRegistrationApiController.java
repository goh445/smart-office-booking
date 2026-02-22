package com.lenovo.smart_office_booking.api;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lenovo.smart_office_booking.api.dto.RegistrationReviewRequest;
import com.lenovo.smart_office_booking.api.dto.UserRegistrationRequestResponse;
import com.lenovo.smart_office_booking.domain.UserRegistrationRequest;
import com.lenovo.smart_office_booking.service.UserRegistrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/registrations")
@RequiredArgsConstructor
public class AdminRegistrationApiController {

    private final UserRegistrationService userRegistrationService;

    @GetMapping("/pending")
    public ResponseEntity<List<UserRegistrationRequestResponse>> pending() {
        List<UserRegistrationRequestResponse> payload = userRegistrationService.getPending()
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<UserRegistrationRequestResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationReviewRequest request,
            Principal principal
    ) {
        String comment = request == null ? null : request.comment();
        UserRegistrationRequest updated = userRegistrationService.approve(id, principal.getName(), comment);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<UserRegistrationRequestResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationReviewRequest request,
            Principal principal
    ) {
        String comment = request == null ? null : request.comment();
        UserRegistrationRequest updated = userRegistrationService.reject(id, principal.getName(), comment);
        return ResponseEntity.ok(toResponse(updated));
    }

    private UserRegistrationRequestResponse toResponse(UserRegistrationRequest request) {
        return new UserRegistrationRequestResponse(
                request.getId(),
                request.getUsername(),
                request.getDisplayName(),
                request.getEmail(),
                request.getRequestedRole(),
                request.getStatus(),
                request.getReviewer() == null ? null : request.getReviewer().getUsername(),
                request.getReviewComment(),
                request.getCreatedAt(),
                request.getDecidedAt()
        );
    }
}
