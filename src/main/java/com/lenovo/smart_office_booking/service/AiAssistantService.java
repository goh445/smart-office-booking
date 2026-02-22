package com.lenovo.smart_office_booking.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lenovo.smart_office_booking.domain.AiInteractionLog;
import com.lenovo.smart_office_booking.domain.AppUser;
import com.lenovo.smart_office_booking.domain.Booking;
import com.lenovo.smart_office_booking.domain.Resource;
import com.lenovo.smart_office_booking.domain.enums.AiInteractionType;
import com.lenovo.smart_office_booking.domain.enums.ResourceType;
import com.lenovo.smart_office_booking.repository.AiInteractionLogRepository;
import com.lenovo.smart_office_booking.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final AiInteractionLogRepository aiInteractionLogRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;
    private final ResourceService resourceService;
    private final BookingService bookingService;

    @Value("${spring.ai.google.genai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-pro}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String parseBooking(String username, String prompt) {
        String systemPrompt = """
            你是办公室预约解析助手。请将用户自然语言解析为可执行预约建议，并按以下结构输出：
            1) 预约摘要
            2) 结构化字段：日期、开始时间、结束时间、资源类型、人数、用途
            3) 缺失信息（若有）
            4) 执行建议（信息完整时给出：可直接提交预约）
            请使用简洁中文，不要输出与预约无关内容。
            """;
        return askAndLog(username, AiInteractionType.PARSE_BOOKING, systemPrompt, prompt);
    }

    public String recommendResource(String username, String prompt) {
        String systemPrompt = """
            你是办公室资源推荐助手。请基于用户偏好与场景给出个性化推荐，输出：
            1) 首选推荐（位置/资源类型 + 简短理由）
            2) 备选方案（1-2条）
            3) 一句确认话术（例如：需要为你预留吗？）
            请使用简洁中文，语气专业友好。
            """;
        return askAndLog(username, AiInteractionType.RECOMMENDATION, systemPrompt, prompt);
    }

    public String assistantQa(String username, String prompt) {
        String systemPrompt = """
            你是联想 Smart Office Booking 智能助理。请回答办公室规则、设备使用、预约与审批流程问题。
            回答格式：结论 + 关键步骤（必要时）+ 注意事项。
            如果信息不足，请明确说明需要的补充信息。
            """;
        return askAndLog(username, AiInteractionType.ASSISTANT_QA, systemPrompt, prompt);
    }

        public AiBookingCreationResult parseAndCreateBooking(String username, String prompt) {
            AiBookingPreviewResult preview = parseBookingPreview(username, prompt);
            return createBookingFromParsed(
                username,
                preview.resource().getId(),
                preview.draft().title(),
                preview.draft().startTime(),
                preview.draft().endTime(),
                preview.draft().notes()
            );
            }

            public AiBookingPreviewResult parseBookingPreview(String username, String prompt) {
            String systemPrompt = """
                你是办公室预约解析与执行助手。请严格输出 JSON 对象，不要输出 markdown 或多余文字。
                字段要求：
                title: string（简短预约标题）
                startTime: string（ISO-8601，本地时间，例如 2026-02-18T14:00:00）
                endTime: string（ISO-8601，本地时间）
                resourceType: string（MEETING_ROOM / DESK / OTHER）
                minCapacity: number（可选）
                notes: string（可选）
                重要：startTime 和 endTime 必须是未来时间；当用户提到“明天”时，必须解析为当前本地日期+1天。
                如果用户没有明确结束时间，默认时长 1 小时。
                """;

            String aiRaw = askAndLog(username, AiInteractionType.PARSE_BOOKING, systemPrompt, prompt);
            ParsedBookingDraft draft = parseBookingDraft(aiRaw, prompt);

            Resource selectedResource = selectAvailableResource(
                draft.resourceType(),
                draft.minCapacity(),
                draft.startTime(),
                draft.endTime()
            );

            String answer = """
                已完成预约信息解析，当前为预览状态。
                资源：%s（%s）
                时间：%s 到 %s
                标题：%s
                请确认后提交预订。
                """.formatted(
                selectedResource.getName(),
                selectedResource.getCode(),
                draft.startTime(),
                draft.endTime(),
                draft.title()
            );

            ParsedBookingView view = new ParsedBookingView(
                draft.title(),
                draft.startTime(),
                draft.endTime(),
                draft.resourceType(),
                draft.minCapacity(),
                draft.notes()
            );
            return new AiBookingPreviewResult(answer, view, selectedResource);
            }

            public AiBookingCreationResult createBookingFromParsed(
                String username,
                Long resourceId,
                String title,
                LocalDateTime startTime,
                LocalDateTime endTime,
                String notes
            ) {
            Resource selectedResource = resourceService.getResourceById(resourceId);

            if (!resourceService.isResourceAvailable(resourceId, startTime, endTime)) {
                throw new IllegalStateException("该资源在所选时间段已不可用，请重新解析后再提交");
            }

            String safeTitle = (title == null || title.isBlank()) ? "AI自动预约" : title.trim();
            String safeNotes = (notes == null || notes.isBlank())
                ? "来自 AI 自然语言预约"
                : notes.trim();

            Booking booking = bookingService.createBooking(
                username,
                selectedResource.getId(),
                safeTitle,
                startTime,
                endTime,
                safeNotes
            );

            String answer = """
                已根据你的自然语言需求完成预订提交。
                资源：%s（%s）
                时间：%s 到 %s
                标题：%s
                """.formatted(
                selectedResource.getName(),
                selectedResource.getCode(),
                startTime,
                endTime,
                safeTitle
            );

            return new AiBookingCreationResult(answer, booking);
        }

    private String askAndLog(String username, AiInteractionType type, String systemPrompt, String userPrompt) {
        AppUser user = appUserRepository.findByUsername(username).orElse(null);
        AiInteractionLog log = new AiInteractionLog();
        log.setUser(user);
        log.setType(type);
        log.setUserPrompt(userPrompt);
        log.setModelName(model);

        try {
            if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("REPLACE_")) {
                throw new IllegalStateException("AI API Key 未配置，请在 application.properties 中将 REPLACE_WITH_YOUR_GOOGLE_GENAI_API_KEY 替换为你的 key，或设置环境变量 GOOGLE_GENAI_API_KEY");
            }

            String answer = callGemini(systemPrompt, userPrompt);

            log.setSuccess(true);
            log.setAiResponse(answer);
            aiInteractionLogRepository.save(log);
            return answer;
        } catch (Exception ex) {
            log.setSuccess(false);
            log.setErrorMessage(ex.getMessage());
            aiInteractionLogRepository.save(log);
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private String callGemini(String systemPrompt, String userPrompt) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> payload = new HashMap<>();
        payload.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        ));
        payload.put("contents", List.of(Map.of(
                "parts", List.of(Map.of("text", userPrompt))
        )));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        String raw = restTemplate.postForObject(endpoint, request, String.class);

        JsonNode root = objectMapper.readTree(raw);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("AI 未返回有效结果");
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("AI 返回内容为空");
        }

        StringBuilder answer = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                answer.append(part.get("text").asText());
            }
        }

        String result = answer.toString().trim();
        if (result.isEmpty()) {
            throw new IllegalStateException("AI 返回空文本");
        }
        return result;
    }

    private ParsedBookingDraft parseBookingDraft(String raw, String userPrompt) {
        try {
            String jsonText = extractJson(raw);
            JsonNode node = objectMapper.readTree(jsonText);

            LocalDateTime startTime = parseDateTime(node.path("startTime").asText(null), "startTime");
            String endText = node.path("endTime").asText(null);
            LocalDateTime endTime = (endText == null || endText.isBlank()) ? startTime.plusHours(1)
                    : parseDateTime(endText, "endTime");

            TimeRange timeRange = normalizeParsedTime(startTime, endTime, userPrompt);
            startTime = timeRange.startTime();
            endTime = timeRange.endTime();

            if (!endTime.isAfter(startTime)) {
                throw new IllegalStateException("AI 解析时间无效：endTime 必须晚于 startTime");
            }

            if (startTime.isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("AI 解析到的开始时间仍早于当前时间，请在输入中明确具体日期（例如：明天中午12点）");
            }

            String resourceTypeText = node.path("resourceType").asText("DESK");
            ResourceType resourceType = parseResourceType(resourceTypeText);

            Integer minCapacity = null;
            if (node.hasNonNull("minCapacity") && node.path("minCapacity").canConvertToInt()) {
                minCapacity = Math.max(1, node.path("minCapacity").asInt());
            }

            String title = node.path("title").asText("AI自动预约");
            String notes = node.path("notes").asText("");

            return new ParsedBookingDraft(title, startTime, endTime, resourceType, minCapacity, notes);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new IllegalStateException("AI 解析结果无法转换为预约信息，请补充更明确的时间与资源需求", ex);
        }
    }

    private TimeRange normalizeParsedTime(LocalDateTime startTime, LocalDateTime endTime, String userPrompt) {
        if (startTime.isBefore(LocalDateTime.now()) && containsTomorrowHint(userPrompt)) {
            while (startTime.isBefore(LocalDateTime.now())) {
                startTime = startTime.plusDays(1);
                endTime = endTime.plusDays(1);
            }
        }
        return new TimeRange(startTime, endTime);
    }

    private boolean containsTomorrowHint(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase();
        return normalized.contains("明天") || normalized.contains("tomorrow");
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("AI 返回内容为空，无法创建预订");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("AI 未返回结构化 JSON，无法自动创建预订");
        }
        return raw.substring(start, end + 1);
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("AI 解析缺少字段: " + fieldName);
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("AI 解析时间格式错误(" + fieldName + "): " + value, ex);
        }
    }

    private ResourceType parseResourceType(String value) {
        if (value == null || value.isBlank()) {
            return ResourceType.DESK;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("MEETING") || normalized.contains("会议")) {
            return ResourceType.MEETING_ROOM;
        }
        if (normalized.contains("DESK") || normalized.contains("工位") || normalized.contains("座位")) {
            return ResourceType.DESK;
        }
        if (normalized.contains("OTHER") || normalized.contains("设备")) {
            return ResourceType.OTHER;
        }
        return ResourceType.valueOf(normalized);
    }

    private Resource selectAvailableResource(
            ResourceType type,
            Integer minCapacity,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return resourceService.getAllActiveResources().stream()
                .filter(resource -> resource.getType() == type)
                .filter(resource -> minCapacity == null
                        || resource.getCapacity() == null
                        || resource.getCapacity() >= minCapacity)
                .sorted(Comparator.comparingInt(resource -> {
                    Integer capacity = resource.getCapacity();
                    return capacity == null ? Integer.MAX_VALUE : capacity;
                }))
                .filter(resource -> resourceService.isResourceAvailable(resource.getId(), startTime, endTime))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("当前没有符合条件且可用的资源，请调整时间或资源要求后重试"));
    }

    public record AiBookingCreationResult(String answer, Booking booking) {
    }

        public record AiBookingPreviewResult(String answer, ParsedBookingView draft, Resource resource) {
        }

        public record ParsedBookingView(
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ResourceType resourceType,
            Integer minCapacity,
            String notes
        ) {
    }

    private record ParsedBookingDraft(
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ResourceType resourceType,
            Integer minCapacity,
            String notes
    ) {
    }

    private record TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
