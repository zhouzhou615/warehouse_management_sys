package org.example.warehouse_management_sys.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.Entity.StockAlert;
import org.example.warehouse_management_sys.Service.AlertService;
import org.example.warehouse_management_sys.Utils.Result;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Resource
    private AlertService alertService;
    @PutMapping("/handle")
    public Result<Boolean> handleAlert(@RequestParam Integer alertId,
                                       @RequestParam String handleRemark) {
        boolean success = alertService.handleAlert(alertId, handleRemark);
        return Result.success("预警处理成功", success);
    }
    @GetMapping("/unhandled")
    public Result<List<StockAlert>> getUnhandledAlerts(
            @RequestParam(required = false) String alertType) {
        List<StockAlert> alerts = alertService.getUnhandledAlerts(alertType);
        return Result.success(alerts);
    }

    @GetMapping("/list")
    public Result<List<StockAlert>> getAllAlerts() {
        List<StockAlert> alerts = alertService.getAllAlerts();
        return Result.success(alerts);
    }

    @GetMapping("/material/{materialId}")
    public Result<List<StockAlert>> getAlertsByMaterialId(@PathVariable String materialId) {
        List<StockAlert> alerts = alertService.getAlertsByMaterialId(materialId);
        return Result.success(alerts);
    }
}
