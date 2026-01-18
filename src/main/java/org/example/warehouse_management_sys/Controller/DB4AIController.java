// [file name]: DB4AIController.java (增强版)
// [file content begin]
package org.example.warehouse_management_sys.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.AnomalyDetectionDTO;
import org.example.warehouse_management_sys.DTO.DB4AIPredictDTO;
import org.example.warehouse_management_sys.Service.DB4AIService;
import org.example.warehouse_management_sys.Utils.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/db4ai")
public class DB4AIController {

    @Resource
    private DB4AIService db4aiService;

    /**
     * 手动触发库存预测
     */
    @PostMapping("/predict-stock")
    public Map<String, Object> predictStock(@RequestParam(defaultValue = "false") boolean forceMock) {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("执行库存预测，forceMock: {}", forceMock);

            if (forceMock) {
                // 强制使用模拟数据
                List<DB4AIPredictDTO> mockPredictions = db4aiService.getPurchaseRecommendations();
                result.put("code", 200);
                result.put("message", "使用模拟预测数据");
                result.put("data", mockPredictions);
                result.put("mock", true);
                result.put("count", mockPredictions.size());
            } else {
                // 尝试真实预测
                Map<String, Object> predictionResult = db4aiService.generateStockPredictions();

                if ("200".equals(predictionResult.get("code").toString())) {
                    List<DB4AIPredictDTO> predictions = db4aiService.getPurchaseRecommendations();
                    result.put("code", 200);
                    result.put("message", predictionResult.get("message"));
                    result.put("data", predictions);
                    result.put("mock", predictions.isEmpty() || predictions.get(0).getDayNum() == null);
                    result.put("count", predictions.size());
                    result.put("predictionCount", predictionResult.get("predictionCount"));
                    result.put("historyCount", predictionResult.get("historyCount"));
                } else {
                    // 预测失败，使用模拟数据
                    List<DB4AIPredictDTO> mockPredictions = db4aiService.getPurchaseRecommendations();
                    result.put("code", 200);
                    result.put("message", predictionResult.get("message") + "，使用模拟数据演示");
                    result.put("data", mockPredictions);
                    result.put("mock", true);
                    result.put("count", mockPredictions.size());
                    result.put("warning", predictionResult.get("message"));
                }
            }
        } catch (Exception e) {
            log.error("库存预测失败", e);
            result.put("code", 500);
            result.put("message", "预测失败: " + e.getMessage());
        }
        return result;
    }
//    // 获取预测详情
//    @GetMapping("/prediction-details")
//    public Result getPredictionDetails() {
//        Map<String, Object> details = db4aiService.getPredictionDetails();
//        return Result.success(details);
//    }

    /**
     * 获取预测详情和日志
     */
    @GetMapping("/prediction-details")
    public Map<String, Object> getPredictionDetails() {
        return db4aiService.getPredictionDetails();
    }

    /**
     * 获取预测执行日志
     */
    @GetMapping("/prediction-logs")
    public Map<String, Object> getPredictionLogs(
            @RequestParam(required = false) String batchId,
            @RequestParam(defaultValue = "100") Integer limit) {
        return db4aiService.getPredictionLogs(batchId, limit);
    }
    /**
     * 获取未来2周采购推荐
     */
    @GetMapping("/purchase-recommendations")
    public Map<String, Object> getPurchaseRecommendations() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DB4AIPredictDTO> recommendations = db4aiService.getPurchaseRecommendations();
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", recommendations);
            result.put("total", recommendations.size());

            // 标记是否为模拟数据
            boolean isMock = recommendations.isEmpty() ||
                    recommendations.stream().anyMatch(dto -> dto.getDayNum() == null);
            result.put("mock", isMock);

            if (isMock && !recommendations.isEmpty()) {
                result.put("note", "当前使用模拟数据演示，实际预测需要足够的历史数据");
            }
        } catch (Exception e) {
            log.error("获取采购推荐失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 查询异常出入库记录
     */
    @GetMapping("/anomaly-records")
    public Map<String, Object> getAnomalyRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "false") boolean mock) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDate start = startDate != null ?
                    LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE) :
                    LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ?
                    LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE) :
                    LocalDate.now();

            List<AnomalyDetectionDTO> anomalies = db4aiService.getAnomalyRecords(start, end, mock);
            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", anomalies);
            result.put("total", anomalies.size());
            result.put("mock", mock);
            result.put("period", start + " 至 " + end);

            if (mock) {
                result.put("note", "当前使用模拟异常数据演示");
            }
        } catch (Exception e) {
            log.error("查询异常记录失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取系统状态和诊断信息
     */
    @GetMapping("/system-status")
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> status = db4aiService.getSystemStatus();
            result.put("code", 200);
            result.put("message", "系统状态获取成功");
            result.put("data", status);
            result.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            result.put("code", 500);
            result.put("message", "获取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 测试DB4AI模型
     */
    @GetMapping("/test-models")
    public Map<String, Object> testDB4AIModels() {
        return db4aiService.testDB4AIModels();
    }

    /**
     * 初始化演示数据
     */
    @PostMapping("/init-demo-data")
    public Map<String, Object> initializeDemoData() {
        return db4aiService.initializeDemoData();
    }

    /**
     * 获取物料出库统计
     */
    @GetMapping("/material-stats/{materialId}")
    public Map<String, Object> getMaterialStats(@PathVariable String materialId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sql = "SELECT " +
                    "AVG(quantity::double precision) AS avg_quantity, " +
                    "STDDEV(quantity::double precision) AS std_quantity, " +
                    "COUNT(*) AS record_count " +
                    "FROM inout_record " +
                    "WHERE material_id = ? AND inout_type = '出库'";

            Map<String, Object> stats = jdbcTemplate.queryForMap(sql, materialId);

            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", stats);
        } catch (Exception e) {
            log.error("获取物料统计失败", e);
            // 返回模拟数据
            result.put("code", 200);
            result.put("message", "使用模拟统计");
            result.put("data", Map.of(
                    "avg_quantity", 50.0,
                    "std_quantity", 15.0,
                    "record_count", 10
            ));
            result.put("mock", true);
        }
        return result;
    }

    /**
     * 预测单个物料未来库存变化
     */
    @GetMapping("/predict-material")
    public Map<String, Object> predictMaterialStock(
            @RequestParam String materialId,
            @RequestParam(defaultValue = "14") Integer daysAhead) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取物料当前信息
            String materialSql = "SELECT material_name, current_stock, safe_stock_min FROM material WHERE material_id = ?";
            Map<String, Object> material = jdbcTemplate.queryForMap(materialSql, materialId);

            // 模拟预测逻辑（因为DB4AI模型可能不可用）
            BigDecimal currentStock = new BigDecimal(material.get("current_stock").toString());
            BigDecimal safeStockMin = new BigDecimal(material.get("safe_stock_min").toString());

            // 基于历史趋势模拟预测
            String historySql = "SELECT " +
                    "AVG(CASE WHEN inout_type = '入库' THEN quantity ELSE -quantity END) as daily_change " +
                    "FROM inout_record " +
                    "WHERE material_id = ? AND operation_time >= CURRENT_DATE - INTERVAL '30 days'";

            BigDecimal dailyChange = jdbcTemplate.queryForObject(historySql, BigDecimal.class, materialId);
            if (dailyChange == null) {
                dailyChange = BigDecimal.valueOf(-0.1); // 默认每天减少0.1
            }

            BigDecimal predictedChange = dailyChange.multiply(BigDecimal.valueOf(daysAhead))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal predictedStock = currentStock.add(predictedChange);

            result.put("code", 200);
            result.put("message", "预测成功");
            result.put("data", Map.of(
                    "materialId", materialId,
                    "materialName", material.get("material_name"),
                    "currentStock", currentStock,
                    "safeStockMin", safeStockMin,
                    "daysAhead", daysAhead,
                    "predictedChange", predictedChange,
                    "predictedStock", predictedStock,
                    "needPurchase", predictedStock.compareTo(safeStockMin) < 0,
                    "mock", dailyChange.compareTo(BigDecimal.valueOf(-0.1)) == 0
            ));
        } catch (Exception e) {
            log.error("预测物料库存失败", e);
            result.put("code", 500);
            result.put("message", "预测失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取聚类分析结果
     */
    @GetMapping("/cluster-analysis")
    public Map<String, Object> getClusterAnalysis() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 尝试从视图获取聚类分析
            String sql = "SELECT " +
                    "cluster, " +
                    "COUNT(*) AS record_count, " +
                    "ROUND(AVG(quantity)::numeric, 2) AS avg_quantity, " +
                    "MIN(quantity) AS min_quantity, " +
                    "MAX(quantity) AS max_quantity " +
                    "FROM v_anomaly_inout_loose_4517 " +
                    "WHERE cluster IS NOT NULL " +
                    "GROUP BY cluster " +
                    "ORDER BY cluster";

            List<Map<String, Object>> clusters = jdbcTemplate.queryForList(sql);

            if (clusters.isEmpty()) {
                // 生成模拟聚类数据
                clusters = List.of(
                        Map.of("cluster", 0, "record_count", 45, "avg_quantity", 12.5, "min_quantity", 1.0, "max_quantity", 25.0),
                        Map.of("cluster", 1, "record_count", 28, "avg_quantity", 85.3, "min_quantity", 26.0, "max_quantity", 150.0),
                        Map.of("cluster", 2, "record_count", 12, "avg_quantity", 210.7, "min_quantity", 151.0, "max_quantity", 500.0)
                );
                result.put("mock", true);
            }

            result.put("code", 200);
            result.put("message", "查询成功");
            result.put("data", clusters);
        } catch (Exception e) {
            log.error("获取聚类分析失败", e);
            result.put("code", 500);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 测试K-means模型预测
     */
    @PostMapping("/test-cluster")
    public Map<String, Object> testClusterPrediction(@RequestParam BigDecimal quantity) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 简化版聚类预测
            int cluster;
            if (quantity.compareTo(BigDecimal.valueOf(25)) < 0) {
                cluster = 0;
            } else if (quantity.compareTo(BigDecimal.valueOf(150)) < 0) {
                cluster = 1;
            } else {
                cluster = 2;
            }

            result.put("code", 200);
            result.put("message", "预测成功");
            result.put("data", Map.of(
                    "quantity", quantity,
                    "predictedCluster", cluster,
                    "clusterDescription", cluster == 0 ? "小批量出库" : cluster == 1 ? "中批量出库" : "大批量出库",
                    "mock", true
            ));
        } catch (Exception e) {
            log.error("聚类预测测试失败", e);
            result.put("code", 500);
            result.put("message", "预测失败: " + e.getMessage());
        }
        return result;
    }

    @Resource
    private JdbcTemplate jdbcTemplate;
}
// [file content end]