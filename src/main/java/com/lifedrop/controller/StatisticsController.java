package com.lifedrop.controller;

import com.lifedrop.dto.response.ApiResponse;
import com.lifedrop.dto.response.StatisticsResponse;
import com.lifedrop.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistics", description = "Live database-calculated statistics for dashboard metrics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    @Operation(summary = "Get live database-calculated statistics for LifeDrop platform")
    public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics() {
        StatisticsResponse stats = statisticsService.getLiveStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
