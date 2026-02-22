package com.lenovo.smart_office_booking.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lenovo.smart_office_booking.api.dto.PeakAlertResponse;
import com.lenovo.smart_office_booking.api.dto.UtilizationDashboardResponse;
import com.lenovo.smart_office_booking.domain.Booking;
import com.lenovo.smart_office_booking.domain.enums.BookingStatus;
import com.lenovo.smart_office_booking.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.PENDING_APPROVAL,
            BookingStatus.APPROVED
    );

    private static final List<Integer> HOURS = List.of(8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);

    private final BookingRepository bookingRepository;

    public UtilizationDashboardResponse getUtilizationDashboard(LocalDate startDate, int days) {
        int safeDays = Math.min(Math.max(days, 1), 14);
        LocalDate safeStartDate = startDate == null ? LocalDate.now() : startDate;
        LocalDate endDate = safeStartDate.plusDays(safeDays);

        LocalDateTime rangeStart = safeStartDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atStartOfDay();

        List<Booking> bookings = bookingRepository.findByStatusInAndTimeRange(ACTIVE_STATUSES, rangeStart, rangeEnd);
        LinkedHashSet<String> floorsSet = new LinkedHashSet<>();
        Map<String, Map<Integer, Long>> counters = new HashMap<>();

        for (Booking booking : bookings) {
            String floor = normalizeFloor(booking.getResource().getLocation());
            floorsSet.add(floor);
            counters.putIfAbsent(floor, new HashMap<>());

            LocalDateTime bookingStart = booking.getStartTime().isBefore(rangeStart) ? rangeStart : booking.getStartTime();
            LocalDateTime bookingEnd = booking.getEndTime().isAfter(rangeEnd) ? rangeEnd : booking.getEndTime();

            LocalDateTime slot = bookingStart.withMinute(0).withSecond(0).withNano(0);
            while (slot.isBefore(bookingEnd)) {
                int hour = slot.getHour();
                if (HOURS.contains(hour) && overlaps(slot, slot.plusHours(1), bookingStart, bookingEnd)) {
                    counters.get(floor).merge(hour, 1L, Long::sum);
                }
                slot = slot.plusHours(1);
            }
        }

        List<String> floors = floorsSet.stream().sorted().toList();
        List<UtilizationDashboardResponse.HeatCell> heatCells = new ArrayList<>();
        for (String floor : floors) {
            Map<Integer, Long> byHour = counters.getOrDefault(floor, Map.of());
            for (int hour : HOURS) {
                heatCells.add(new UtilizationDashboardResponse.HeatCell(floor, hour, byHour.getOrDefault(hour, 0L)));
            }
        }

        return new UtilizationDashboardResponse(
                safeStartDate,
                endDate.minusDays(1),
                floors,
                HOURS,
                heatCells
        );
    }

    public PeakAlertResponse getNextWeekPeakAlert(LocalDate startDate) {
        LocalDate safeStartDate = startDate == null ? LocalDate.now() : startDate;
        LocalDate nextWeekEndDate = safeStartDate.plusDays(7);

        List<Booking> futureBookings = bookingRepository.findByStatusInAndTimeRange(
                ACTIVE_STATUSES,
                safeStartDate.atStartOfDay(),
                nextWeekEndDate.atStartOfDay()
        );

        LocalDate historyStart = safeStartDate.minusWeeks(4);
        List<Booking> historyBookings = bookingRepository.findByStatusInAndTimeRange(
                ACTIVE_STATUSES,
                historyStart.atStartOfDay(),
                safeStartDate.atStartOfDay()
        );

        Map<LocalDate, Long> futureCounts = countByStartDate(futureBookings);
        Map<DayOfWeek, List<Long>> historyByDayOfWeek = historyCountsByDayOfWeek(historyBookings);

        List<PeakAlertResponse.PeakDayPrediction> predictions = new ArrayList<>();
        List<Double> allScores = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = safeStartDate.plusDays(i);
            long existing = futureCounts.getOrDefault(date, 0L);
            double baseline = average(historyByDayOfWeek.getOrDefault(date.getDayOfWeek(), List.of()));
            double predicted = existing + baseline * 0.7;
            allScores.add(predicted);

            predictions.add(new PeakAlertResponse.PeakDayPrediction(
                    date,
                    date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINA),
                    existing,
                    round1(baseline),
                    round1(predicted),
                    "NORMAL",
                    false,
                    ""
            ));
        }

        double avgScore = average(allScores);
        double warningThreshold = Math.max(3.0, avgScore * 1.2);
        double highThreshold = Math.max(4.0, avgScore * 1.5);

        double minScore = predictions.stream()
                .map(PeakAlertResponse.PeakDayPrediction::predictedLoad)
                .min(Comparator.naturalOrder())
                .orElse(0.0);

        List<PeakAlertResponse.PeakDayPrediction> finalized = predictions.stream().map(p -> {
            boolean warning = p.predictedLoad() >= warningThreshold;
            String level = p.predictedLoad() >= highThreshold ? "HIGH" : (warning ? "MEDIUM" : "NORMAL");
            String recommendation = warning
                    ? buildRecommendation(p.date(), p.predictedLoad(), minScore)
                    : "建议正常安排到岗";
            return new PeakAlertResponse.PeakDayPrediction(
                    p.date(),
                    p.dayOfWeek(),
                    p.existingBookings(),
                    p.historicalBaseline(),
                    p.predictedLoad(),
                    level,
                    warning,
                    recommendation
            );
        }).toList();

        return new PeakAlertResponse(LocalDateTime.now(), finalized);
    }

    private static boolean overlaps(LocalDateTime slotStart, LocalDateTime slotEnd, LocalDateTime bookingStart, LocalDateTime bookingEnd) {
        return slotStart.isBefore(bookingEnd) && slotEnd.isAfter(bookingStart);
    }

    private static Map<LocalDate, Long> countByStartDate(List<Booking> bookings) {
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Booking booking : bookings) {
            counts.merge(booking.getStartTime().toLocalDate(), 1L, Long::sum);
        }
        return counts;
    }

    private static Map<DayOfWeek, List<Long>> historyCountsByDayOfWeek(List<Booking> bookings) {
        Map<LocalDate, Long> dateCounts = countByStartDate(bookings);
        Map<DayOfWeek, List<Long>> result = new HashMap<>();
        for (Map.Entry<LocalDate, Long> entry : dateCounts.entrySet()) {
            result.computeIfAbsent(entry.getKey().getDayOfWeek(), key -> new ArrayList<>()).add(entry.getValue());
        }
        return result;
    }

    private static double average(List<? extends Number> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Number value : values) {
            sum += value.doubleValue();
        }
        return sum / values.size();
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String normalizeFloor(String location) {
        if (location == null || location.isBlank()) {
            return "未知楼层";
        }

        String[] segments = location.split("-");
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.toLowerCase(Locale.ROOT).contains("floor")) {
                return trimmed;
            }
        }

        return location.trim();
    }

    private static String buildRecommendation(LocalDate date, double score, double minScore) {
        if (score - minScore >= 3.0) {
            return "建议选择远程办公或将到岗日期调整到更空闲日期";
        }
        if (date.getDayOfWeek() == DayOfWeek.MONDAY || date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            return "建议避开上午高峰，优先选择 10:30 后到岗";
        }
        return "建议错峰到岗，优先选择下午时段";
    }
}
