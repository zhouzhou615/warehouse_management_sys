// [file name]: DB4AIController.java (修复版)
package org.example.warehouse_management_sys.Controller;

import org.example.warehouse_management_sys.Service.DB4AIService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/db4ai")
public class DB4AIController {

    @Resource
    private DB4AIService db4aiService;

    /**
     * 手动触发库存预测
     */
    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predictStock() {
        try {
            List<Map<String, Object>> purchaseList = db4aiService.predictStockAndGenerateAlerts();

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "库存预测完成");
            result.put("data", purchaseList);
            result.put("count", purchaseList.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "预测失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 手动触发异常检测
     */
    @PostMapping("/detect-anomalies")
    public ResponseEntity<Map<String, Object>> detectAnomalies() {
        try {
            List<Map<String, Object>> anomalies = db4aiService.detectAnomalousInoutRecords();

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "异常检测完成");
            result.put("data", anomalies);
            result.put("count", anomalies.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "异常检测失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取采购推荐清单
     */
    @GetMapping("/purchase-recommendations")
    public ResponseEntity<Map<String, Object>> getPurchaseRecommendations() {
        try {
            List<Map<String, Object>> recommendations = db4aiService.getPurchaseRecommendations();

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取采购推荐成功");
            result.put("data", recommendations);
            result.put("count", recommendations.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "获取采购推荐失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 手动记录库存快照
     */
    @PostMapping("/record-snapshot")
    public ResponseEntity<Map<String, Object>> recordSnapshot() {
        try {
            db4aiService.recordDailyStockSnapshot();

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "库存快照记录完成"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "code", 500,
                            "message", "记录失败: " + e.getMessage(),
                            "error", e.getClass().getSimpleName()
                    ));
        }
    }

    /**
     * 清除旧的预测数据
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        try {
            db4aiService.cleanupOldPredictions();

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "清理旧数据完成"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "code", 500,
                            "message", "清理失败: " + e.getMessage()
                    ));
        }
    }

    /**
     * 获取预测准确率统计
     */
    @GetMapping("/accuracy")
    public ResponseEntity<Map<String, Object>> getAccuracy() {
        try {
            Map<String, Object> stats = db4aiService.getPredictionAccuracy();

            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "获取预测准确率成功");
            result.put("data", stats);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "code", 500,
                            "message", "获取准确率失败: " + e.getMessage()
                    ));
        }
    }
}