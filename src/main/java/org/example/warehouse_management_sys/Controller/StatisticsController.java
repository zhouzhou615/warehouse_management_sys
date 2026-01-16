package org.example.warehouse_management_sys.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.MonthlyQueryDTO;
import org.example.warehouse_management_sys.DTO.StatisticsDTO;
import org.example.warehouse_management_sys.Service.StatisticsService;
import org.example.warehouse_management_sys.Utils.Result;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @PostMapping("/monthly")
    public Result<List<StatisticsDTO>> getMonthlyStatistics(@Valid @RequestBody MonthlyQueryDTO dto) {
        List<StatisticsDTO> stats = statisticsService.getMonthlyStatistics(
                dto.getYear(),
                dto.getMonth()
        );
        return Result.success(stats);
    }
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> data = statisticsService.getOverview();
        return Result.success(data);
    }

    @GetMapping("/categoryFlow")
    public Result<List<StatisticsDTO>> getCategoryFlowStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<StatisticsDTO> stats = statisticsService.getCategoryFlowStatistics(
                startDate,
                endDate
        );
        return Result.success(stats);
    }

    @GetMapping("/trend/{materialId}")
    public Result<List<Map<String, Object>>> getMaterialTrend(
            @PathVariable String materialId,
            @RequestParam(defaultValue = "6") Integer months) {
        List<Map<String, Object>> trend = statisticsService.getMaterialTrend(
                materialId,
                months
        );
        return Result.success(trend);
    }

//    @GetMapping("/overview")
//    public Result<Map<String, Object>> getStockOverview() {
//        Map<String, Object> overview = statisticsService.getStockOverview();
//        return Result.success(overview);
//    }

    @GetMapping("/alertStats")
    public Result<Map<String, Object>> getAlertStatistics() {
        Map<String, Object> stats = statisticsService.getAlertStatistics();
        return Result.success(stats);
    }

    @GetMapping("/supplierStats")
    public Result<List<Map<String, Object>>> getSupplierStatistics() {
        List<Map<String, Object>> stats = statisticsService.getSupplierStatistics();
        return Result.success(stats);
    }
}
