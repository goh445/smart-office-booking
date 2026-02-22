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

import com.lenovo.smart_office_booking.api.dto.ApprovalDecisionRequest;
import com.lenovo.smart_office_booking.api.dto.ApprovalQueueItemResponse;
import com.lenovo.smart_office_booking.domain.ApprovalRequest;
import com.lenovo.smart_office_booking.service.ApprovalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalApiController {

    private final ApprovalService approvalService;

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalQueueItemResponse>> getPendingRequests() {
        List<ApprovalQueueItemResponse> payload = approvalService.getPendingRequests()
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalQueueItemResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalDecisionRequest request,
            Principal principal
    ) {
        String comment = request == null ? null : request.comment();
        ApprovalRequest approved = approvalService.approve(id, principal.getName(), comment);
        return ResponseEntity.ok(toResponse(approved));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApprovalQueueItemResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalDecisionRequest request,
            Principal principal
    ) {
        String comment = request == null ? null : request.comment();
        ApprovalRequest rejected = approvalService.reject(id, principal.getName(), comment);
        return ResponseEntity.ok(toResponse(rejected));
    }

    private ApprovalQueueItemResponse toResponse(ApprovalRequest approvalRequest) {
        return new ApprovalQueueItemResponse(
                approvalRequest.getId(),
                approvalRequest.getBooking().getId(),
                approvalRequest.getBooking().getRequester().getUsername(),
                approvalRequest.getBooking().getRequester().getDisplayName(),
                approvalRequest.getBooking().getResource().getCode(),
                approvalRequest.getBooking().getResource().getName(),
                approvalRequest.getBooking().getTitle(),
                approvalRequest.getBooking().getStartTime(),
                approvalRequest.getBooking().getEndTime(),
                approvalRequest.getReason(),
                approvalRequest.getStatus(),
                approvalRequest.getBooking().getStatus(),
                approvalRequest.getCreatedAt(),
                approvalRequest.getDecidedAt(),
                approvalRequest.getApprover() == null ? null : approvalRequest.getApprover().getUsername(),
                approvalRequest.getDecisionComment()
        );
    }
}
