package com.lenovo.smart_office_booking.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PeakAlertResponse(
        LocalDateTime generatedAt,
        List<PeakDayPrediction> predictions
) {
    public record PeakDayPrediction(
            LocalDate date,
            String dayOfWeek,
            long existingBookings,
            double historicalBaseline,
            double predictedLoad,
            String level,
            boolean warning,
            String recommendation
    ) {
    }
}
