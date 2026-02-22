package com.lenovo.smart_office_booking.api;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lenovo.smart_office_booking.api.dto.AiBookingActionResponse;
import com.lenovo.smart_office_booking.api.dto.AiBookingPreviewResponse;
import com.lenovo.smart_office_booking.api.dto.AiBookingSubmitRequest;
import com.lenovo.smart_office_booking.api.dto.AiPromptRequest;
import com.lenovo.smart_office_booking.api.dto.AiResponse;
import com.lenovo.smart_office_booking.service.AiAssistantService;
import com.lenovo.smart_office_booking.service.AiAssistantService.AiBookingCreationResult;
import com.lenovo.smart_office_booking.service.AiAssistantService.AiBookingPreviewResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Validated
public class AiApiController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/parse-booking")
    public ResponseEntity<AiResponse> parseBooking(
            @Valid @RequestBody AiPromptRequest request,
            Principal principal
    ) {
        String answer = aiAssistantService.parseBooking(principal.getName(), request.prompt());
        return ResponseEntity.ok(new AiResponse("PARSE_BOOKING", true, answer, null));
    }

    @PostMapping("/recommend")
    public ResponseEntity<AiResponse> recommend(
            @Valid @RequestBody AiPromptRequest request,
            Principal principal
    ) {
        String answer = aiAssistantService.recommendResource(principal.getName(), request.prompt());
        return ResponseEntity.ok(new AiResponse("RECOMMENDATION", true, answer, null));
    }

    @PostMapping("/ask")
    public ResponseEntity<AiResponse> ask(
            @Valid @RequestBody AiPromptRequest request,
            Principal principal
    ) {
        String answer = aiAssistantService.assistantQa(principal.getName(), request.prompt());
        return ResponseEntity.ok(new AiResponse("ASSISTANT_QA", true, answer, null));
    }

    @PostMapping("/parse-and-book")
    public ResponseEntity<AiBookingActionResponse> parseAndBook(
            @Valid @RequestBody AiPromptRequest request,
            Principal principal
    ) {
        AiBookingCreationResult result = aiAssistantService.parseAndCreateBooking(principal.getName(), request.prompt());
        var booking = result.booking();

        return ResponseEntity.ok(new AiBookingActionResponse(
                "PARSE_AND_BOOK",
                true,
                result.answer(),
                booking.getId(),
                booking.getResource().getCode(),
                booking.getResource().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus().name(),
                null
        ));
    }

            @PostMapping("/parse-preview")
            public ResponseEntity<AiBookingPreviewResponse> parsePreview(
                @Valid @RequestBody AiPromptRequest request,
                Principal principal
            ) {
            AiBookingPreviewResult result = aiAssistantService.parseBookingPreview(principal.getName(), request.prompt());

            return ResponseEntity.ok(new AiBookingPreviewResponse(
                "PARSE_PREVIEW",
                true,
                result.answer(),
                result.draft().title(),
                result.draft().startTime(),
                result.draft().endTime(),
                result.draft().resourceType(),
                result.draft().minCapacity(),
                result.draft().notes(),
                result.resource().getId(),
                result.resource().getCode(),
                result.resource().getName(),
                null
            ));
            }

            @PostMapping("/submit-booking")
            public ResponseEntity<AiBookingActionResponse> submitParsedBooking(
                @Valid @RequestBody AiBookingSubmitRequest request,
                Principal principal
            ) {
            AiBookingCreationResult result = aiAssistantService.createBookingFromParsed(
                principal.getName(),
                request.resourceId(),
                request.title(),
                request.startTime(),
                request.endTime(),
                request.notes()
            );
            var booking = result.booking();

            return ResponseEntity.ok(new AiBookingActionResponse(
                "PARSE_AND_BOOK",
                true,
                result.answer(),
                booking.getId(),
                booking.getResource().getCode(),
                booking.getResource().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus().name(),
                null
            ));
            }
}
