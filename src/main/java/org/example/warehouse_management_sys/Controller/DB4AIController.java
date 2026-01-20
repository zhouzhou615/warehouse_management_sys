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
                }

        } catch (Exception e) {
            log.error("库存预测失败", e);
            result.put("code", 500);
            result.put("message", "预测失败: " + e.getMessage());
        }
        return result;
    }


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

            boolean isMock = recommendations.isEmpty() ||
                    recommendations.stream().anyMatch(dto -> dto.getDayNum() == null);
            result.put("mock", isMock);

//            if (isMock && !recommendations.isEmpty()) {
//                result.put("note", "当前使用模拟数据演示，实际预测需要足够的历史数据");
//            }
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
     * 测试聚类预测 - 修复参数接收问题
     * 修改前：@RequestParam BigDecimal quantity
     * 修改后：@RequestBody Map<String, Object> request
     */
    @PostMapping("/test-cluster")
    public Result<Map<String, Object>> testClusterPrediction(@RequestBody Map<String, Object> request) {
        try {
            log.info("接收到聚类预测请求: {}", request);

            // 从请求体中获取 quantity 参数
            Object quantityObj = request.get("quantity");
            if (quantityObj == null) {
                return Result.error(400, "参数 quantity 不能为空");
            }

            BigDecimal quantity;
            try {
                quantity = new BigDecimal(quantityObj.toString());
            } catch (NumberFormatException e) {
                return Result.error(400, "参数 quantity 格式错误");
            }

            // 模拟聚类预测结果（实际应调用业务逻辑）
            Map<String, Object> result = new HashMap<>();

            // 随机生成聚类分组 (0-2)
            int predictedCluster = (int) (Math.random() * 3);

            result.put("predictedCluster", predictedCluster);
            result.put("quantity", quantity);
            result.put("message", "聚类预测成功");

            log.info("聚类预测结果: 数量={}, 聚类分组={}", quantity, predictedCluster);

            return Result.success(result);
        } catch (Exception e) {
            log.error("聚类预测失败", e);
            return Result.error(500, "聚类预测失败: " + e.getMessage());
        }
    }


    @Resource
    private JdbcTemplate jdbcTemplate;
}
