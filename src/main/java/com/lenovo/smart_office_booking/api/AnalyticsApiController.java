package com.lenovo.smart_office_booking.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lenovo.smart_office_booking.api.dto.PeakAlertResponse;
import com.lenovo.smart_office_booking.api.dto.UtilizationDashboardResponse;
import com.lenovo.smart_office_booking.service.AnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsApiController {

    private final AnalyticsService analyticsService;

    @GetMapping("/utilization")
    public ResponseEntity<UtilizationDashboardResponse> getUtilization(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "7") int days
    ) {
        return ResponseEntity.ok(analyticsService.getUtilizationDashboard(startDate, days));
    }

    @GetMapping("/peak-alert")
    public ResponseEntity<PeakAlertResponse> getPeakAlert(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate
    ) {
        return ResponseEntity.ok(analyticsService.getNextWeekPeakAlert(startDate));
    }
}
