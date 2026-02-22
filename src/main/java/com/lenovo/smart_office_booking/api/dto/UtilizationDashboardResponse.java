package com.lenovo.smart_office_booking.api.dto;

import java.time.LocalDate;
import java.util.List;

public record UtilizationDashboardResponse(
        LocalDate windowStartDate,
        LocalDate windowEndDate,
        List<String> floors,
        List<Integer> hours,
        List<HeatCell> heatmap
) {
    public record HeatCell(
            String floor,
            int hour,
            long bookingCount
    ) {
    }
}
