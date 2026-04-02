package com.log_analytics_server.Controller;

import com.log_analytics_server.Model.AnalyticsEventEntity;
import com.log_analytics_server.Service.AnalyticsEventConsumerService;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsEventConsumerService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsEventConsumerService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping(value = "/analytics/account", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getAccountAnalytics(
            @RequestParam String accountId,
            @RequestParam(defaultValue = "7") int days) {

        return analyticsService.getAccountAnalytics(accountId, days)
                .collectList()
                .map(events -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("totalRequests", events.size());
                    result.put("days", days);
                    result.put("accountId", accountId);

                    // Calculate unique users
                    long uniqueUsers = events.stream()
                            .map(AnalyticsEventEntity::getIp)
                            .distinct()
                            .count();
                    result.put("uniqueUsers", uniqueUsers);

                    // Calculate error count (status >= 400)
                    long errorCount = events.stream()
                            .filter(e -> e.getStatus() != null && e.getStatus() >= 400)
                            .count();
                    result.put("errorCount", errorCount);

                    // Calculate error rate
                    double errorRate = events.size() > 0 ? (double) errorCount / events.size() * 100 : 0;
                    result.put("errorRate", String.format("%.2f", errorRate));

                    // Calculate average response time
                    double avgDuration = events.stream()
                            .filter(e -> e.getDuration() != null)
                            .mapToLong(AnalyticsEventEntity::getDuration)
                            .average()
                            .orElse(0);
                    result.put("avgResponseTime", String.format("%.2f", avgDuration));

                    // Total bytes transferred
                    long totalBytesIn = events.stream()
                            .filter(e -> e.getBytesIn() != null)
                            .mapToLong(AnalyticsEventEntity::getBytesIn)
                            .sum();
                    long totalBytesOut = events.stream()
                            .filter(e -> e.getBytesOut() != null)
                            .mapToLong(AnalyticsEventEntity::getBytesOut)
                            .sum();

                    result.put("totalBytesIn", totalBytesIn);
                    result.put("totalBytesOut", totalBytesOut);

                    // Request breakdown by method
                    Map<String, Long> methodBreakdown = new HashMap<>();
                    events.stream()
                            .forEach(e -> methodBreakdown.merge(e.getMethod(), 1L, Long::sum));
                    result.put("methodBreakdown", methodBreakdown);

                    // Request breakdown by source
                    Map<String, Long> sourceBreakdown = new HashMap<>();
                    events.stream()
                            .forEach(e -> sourceBreakdown.merge(
                                    e.getSource() != null ? e.getSource() : "unknown", 1L, Long::sum));
                    result.put("sourceBreakdown", sourceBreakdown);

                    // Top paths
                    Map<String, Long> topPaths = new HashMap<>();
                    events.stream()
                            .forEach(e -> topPaths.merge(e.getPath(), 1L, Long::sum));
                    result.put("topPaths", topPaths);

                    result.put("events", events);
                    return result;
                });
    }

    @GetMapping(value = "/analytics/projects/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getProjectAnalytics(
            @PathVariable String slug,
            @RequestParam(defaultValue = "7") int days) {

        System.out.println(slug);

        return analyticsService.getProjectAnalytics(slug, days)
                .collectList()
                .map(events -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("totalRequests", events.size());
                    result.put("days", days);
                    result.put("projectId", slug);

                    System.out.println(result.get("totalRequests"));

                    // Calculate unique users
                    long uniqueUsers = events.stream()
                            .map(AnalyticsEventEntity::getIp)
                            .distinct()
                            .count();
                    result.put("uniqueUsers", uniqueUsers);

                    System.out.println(result.get("uniqueUsers"));

                    // Calculate error count (status >= 400)
                    long errorCount = events.stream()
                            .filter(e -> e.getStatus() != null && e.getStatus() >= 400)
                            .count();
                    result.put("errorCount", errorCount);

                    System.out.println(result.get("errorCount"));

                    // Calculate error rate
                    double errorRate = events.size() > 0 ? (double) errorCount / events.size() * 100 : 0;
                    result.put("errorRate", String.format("%.2f", errorRate));

                    // Calculate average response time
                    double avgDuration = events.stream()
                            .filter(e -> e.getDuration() != null)
                            .mapToLong(AnalyticsEventEntity::getDuration)
                            .average()
                            .orElse(0);
                    result.put("avgResponseTime", String.format("%.2f", avgDuration));

                    // Total bytes transferred
                    long totalBytesIn = events.stream()
                            .filter(e -> e.getBytesIn() != null)
                            .mapToLong(AnalyticsEventEntity::getBytesIn)
                            .sum();
                    long totalBytesOut = events.stream()
                            .filter(e -> e.getBytesOut() != null)
                            .mapToLong(AnalyticsEventEntity::getBytesOut)
                            .sum();

                    result.put("totalBytesIn", totalBytesIn);
                    result.put("totalBytesOut", totalBytesOut);

                    // Request breakdown by method
                    Map<String, Long> methodBreakdown = new HashMap<>();
                    events.stream()
                            .forEach(e -> methodBreakdown.merge(e.getMethod(), 1L, Long::sum));
                    result.put("methodBreakdown", methodBreakdown);

                    // Request breakdown by source (frontend vs backend)
                    Map<String, Long> sourceBreakdown = new HashMap<>();
                    events.stream()
                            .forEach(e -> sourceBreakdown.merge(
                                    e.getSource() != null ? e.getSource() : "unknown", 1L, Long::sum));
                    result.put("sourceBreakdown", sourceBreakdown);

                    // Status code breakdown
                    Map<Integer, Long> statusBreakdown = new HashMap<>();
                    events.stream()
                            .forEach(e -> statusBreakdown.merge(e.getStatus(), 1L, Long::sum));
                    result.put("statusBreakdown", statusBreakdown);

                    // Top paths
                    Map<String, Long> topPaths = new HashMap<>();
                    events.stream()
                            .forEach(e -> topPaths.merge(e.getPath(), 1L, Long::sum));
                    result.put("topPaths", topPaths);

                    result.put("events", events);
                    System.out.println(result.get("events"));
                    return result;
                });
    }

    @GetMapping(value = "/analytics/projects/{slug}/raw", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<AnalyticsEventEntity> getProjectAnalyticsRaw(
            @PathVariable String slug,
            @RequestParam(defaultValue = "7") int days) {
        return analyticsService.getProjectAnalytics(slug, days);
    }

    @GetMapping(value = "/analytics/account/timeseries", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getAccountTimeSeriesAnalytics(
            @RequestParam String accountId,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "day") String interval) {

        return analyticsService.getAccountAnalytics(accountId, days)
                .collectList()
                .map(events -> {
                    Map<String, Object> result = new HashMap<>();
                    
                    // Choose formatter based on interval
                    DateTimeFormatter formatter;
                    if ("hour".equalsIgnoreCase(interval)) {
                        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
                    } else {
                        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    }
                    
                    // Group events by time interval using TreeMap to keep it sorted
                    Map<String, Long> timeSeriesData = new TreeMap<>();
                    events.stream()
                            .forEach(e -> {
                                String timeKey = e.getTimestamp().format(formatter);
                                timeSeriesData.merge(timeKey, 1L, Long::sum);
                            });
                    
                    result.put("timeseries", timeSeriesData);
                    result.put("interval", interval);
                    result.put("accountId", accountId);
                    result.put("totalRequests", events.size());
                    
                    return result;
                });
    }

    @GetMapping(value = "/analytics/projects/{slug}/timeseries", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> getProjectTimeSeriesAnalytics(
            @PathVariable String slug,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "day") String interval) {

        return analyticsService.getProjectAnalytics(slug, days)
                .collectList()
                .map(events -> {
                    Map<String, Object> result = new HashMap<>();
                    
                    // Choose formatter based on interval
                    DateTimeFormatter formatter;
                    if ("hour".equalsIgnoreCase(interval)) {
                        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
                    } else {
                        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    }
                    
                    // Group events by time interval using TreeMap to keep it sorted
                    Map<String, Long> timeSeriesData = new TreeMap<>();
                    events.stream()
                            .forEach(e -> {
                                String timeKey = e.getTimestamp().format(formatter);
                                timeSeriesData.merge(timeKey, 1L, Long::sum);
                            });
                    
                    result.put("timeseries", timeSeriesData);
                    result.put("interval", interval);
                    result.put("projectId", slug);
                    result.put("totalRequests", events.size());
                    
                    return result;
                });
    }
}

