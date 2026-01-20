// [file name]: DB4AIService.java (修改版)
// [file content begin]
package org.example.warehouse_management_sys.Service;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.DTO.AnomalyDetectionDTO;
import org.example.warehouse_management_sys.DTO.DB4AIPredictDTO;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class DB4AIService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 检查数据库模型和视图是否存在
     */
    public Map<String, Boolean> checkDB4AIComponents() {
        Map<String, Boolean> result = new HashMap<>();
        try {
            // 检查模型
            String[] components = {
                    "MATERIAL_NUM",
                    "V_DAILY_STOCK_CHANGE",
                    "stock_xgb_model",
                    "V_OUT_RECORD_ARRAY_4517",
                    "stock_kmeans_model_4517",
                    "V_MATERIAL_OUT_STATS_4517",
                    "V_ANOMALY_INOUT_LOOSE_4517"
            };

            for (String component : components) {
                try {
                    if (component.startsWith("V_") || component.startsWith("MATERIAL_")) {
                        // 检查表或视图
                        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
                        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, component.toLowerCase());
                        result.put(component, count != null && count > 0);
                    } else {
                        // 检查模型
                        String sql = "SELECT COUNT(*) FROM pg_proc WHERE proname = ?";
                        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, component.toLowerCase());
                        result.put(component, count != null && count > 0);
                    }
                } catch (Exception e) {
                    result.put(component, false);
                }
            }

            log.info("DB4AI组件检查结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("检查DB4AI组件失败", e);
            return Collections.emptyMap();
        }
    }



    /**
     * 调用存储过程生成未来2周库存预测预警
     */
    @Transactional
    public Map<String, Object> generateStockPredictions() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("开始执行库存预测预警...");
            // 先检查是否有足够的历史数据
            String checkSql = "SELECT COUNT(*) FROM inout_record WHERE operation_time >= CURRENT_DATE - INTERVAL '60 days'";
            Integer historyCount = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (historyCount == null || historyCount < 10) {
                result.put("code", 400);
                result.put("message", "历史数据不足，至少需要10条过去60天的出入库记录");
                result.put("historyCount", historyCount);
                log.warn("历史数据不足，无法进行预测，仅有 {} 条记录", historyCount);
                return result;
            }
            // 检查预测存储过程是否存在
            String procCheckSql = "SELECT COUNT(*) FROM pg_proc WHERE proname = 'sp_predict_stock_warning'";
            Integer procCount = jdbcTemplate.queryForObject(procCheckSql, Integer.class);

            if (procCount == null || procCount == 0) {
                result.put("code", 404);
                result.put("message", "预测存储过程不存在");
                log.error("预测存储过程 SP_PREDICT_STOCK_WARNING 不存在");
                return result;
            }

            // 先清空之前的预测预警
            String deleteSql = "DELETE FROM stock_alert WHERE alert_type = '低库存' AND handle_remark IS NULL";
            int deletedCount = jdbcTemplate.update(deleteSql);
            log.info("已清除 {} 条旧的预测预警", deletedCount);

            // 调用存储过程
            String callSql = "CALL SP_PREDICT_STOCK_WARNING()";
            jdbcTemplate.execute(callSql);

            // 检查是否生成了预测数据
            String checkPredictionsSql = "SELECT COUNT(*) FROM stock_alert WHERE alert_type = '低库存'";
            Integer predictionCount = jdbcTemplate.queryForObject(checkPredictionsSql, Integer.class);

            result.put("code", 200);
            result.put("message", "库存预测完成");
            result.put("predictionCount", predictionCount);
            result.put("historyCount", historyCount);

            log.info("库存预测预警生成完成，共生成 {} 条预测记录", predictionCount);

        } catch (Exception e) {
            log.error("执行库存预测预警失败", e);
            result.put("code", 500);
            result.put("message", "库存预测失败: " + e.getMessage());
        }
        return result;
    }
    /**
     * 获取预测过程日志
     */
    public Map<String, Object> getPredictionLogs(String batchId, Integer limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sql = "SELECT " +
                    "log_id, batch_id, log_level, material_id, message, " +
                    "to_char(log_time, 'YYYY-MM-DD HH24:MI:SS') as log_time, " +
                    "prediction_count " +
                    "FROM stock_prediction_log " +
                    "WHERE 1=1 ";

            List<Object> params = new ArrayList<>();

            if (batchId != null && !batchId.isEmpty()) {
                sql += " AND batch_id = ? ";
                params.add(batchId);
            }

            sql += " ORDER BY log_id ASC ";

            if (limit != null && limit > 0) {
                sql += " LIMIT ? ";
                params.add(limit);
            }

            List<Map<String, Object>> logs = jdbcTemplate.queryForList(sql, params.toArray());

            // 获取最新的批次信息
            String latestBatchSql = "SELECT batch_id, MAX(log_time) as last_time, " +
                    "COUNT(*) as log_count, " +
                    "SUM(CASE WHEN log_level = 'SUCCESS' THEN 1 ELSE 0 END) as success_count " +
                    "FROM stock_prediction_log " +
                    "GROUP BY batch_id " +
                    "ORDER BY last_time DESC " +
                    "LIMIT 5";

            List<Map<String, Object>> batchList = jdbcTemplate.queryForList(latestBatchSql);

            result.put("code", 200);
            result.put("logs", logs);
            result.put("batchList", batchList);
            result.put("total", logs.size());

            log.info("获取到 {} 条预测日志，{} 个批次", logs.size(), batchList.size());

        } catch (Exception e) {
            log.error("获取预测日志失败", e);
            result.put("code", 500);
            result.put("message", "获取日志失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取预测详情(从日志表解析)
     */
    public Map<String, Object> getPredictionDetails() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 🔥 从日志表获取最新批次
            String latestBatchSql = "SELECT batch_id FROM stock_prediction_log " +
                    "WHERE log_level = 'SUCCESS' AND message LIKE '%批次ID:%' " +
                    "ORDER BY log_time DESC LIMIT 1";

            String batchId = null;
            try {
                batchId = jdbcTemplate.queryForObject(latestBatchSql, String.class);
            } catch (Exception e) {
                log.warn("未找到预测批次", e);
            }

            if (batchId == null) {
                // 没有批次,尝试获取最新的批次
                try {
                    batchId = jdbcTemplate.queryForObject(
                            "SELECT batch_id FROM stock_prediction_log ORDER BY log_time DESC LIMIT 1",
                            String.class
                    );
                } catch (Exception e) {
                    log.warn("未找到任何批次", e);
                    // 没有批次，返回空结果
                    result.put("code", 200);
                    result.put("message", "无预测数据");
                    result.put("stats", Collections.emptyMap());
                    result.put("details", Collections.emptyList());
                    result.put("changeStats", Collections.emptyMap());
                    result.put("batchId", null);
                    return result;
                }
            }

            // 🔥 从日志表解析预测详情
            String logSql = "SELECT " +
                    "material_id, " +
                    "message, " +
                    "log_level, " +
                    "log_time " +
                    "FROM stock_prediction_log " +
                    "WHERE batch_id = ? " +
                    "AND material_id IS NOT NULL " +
                    "AND message LIKE '物料%当前=%' " +
                    "ORDER BY log_id";

            List<Map<String, Object>> logs = jdbcTemplate.queryForList(logSql, batchId);

            // 解析日志消息
            List<Map<String, Object>> details = new ArrayList<>();
            int increaseCount = 0;
            int decreaseCount = 0;
            double totalChange = 0;
            int validCount = 0;

            for (Map<String, Object> log : logs) {
                String message = (String) log.get("message");
                String materialId = (String) log.get("material_id");

                // 解析消息: "物料 MAT00003: 当前=354.78, 变化=17.32, 预测=372.10"
                Map<String, Object> detail = parseLogMessage(message, materialId);
                if (detail != null) {
                    details.add(detail);

                    // 统计变化
                    BigDecimal change = (BigDecimal) detail.get("predicted_change");
                    if (change != null) {
                        if (change.compareTo(BigDecimal.ZERO) > 0) {
                            increaseCount++;
                        } else if (change.compareTo(BigDecimal.ZERO) < 0) {
                            decreaseCount++;
                        }
                        totalChange += change.doubleValue();
                        validCount++;
                    }
                }
            }

            // 补充物料名称和安全库存
            for (Map<String, Object> detail : details) {
                String materialId = (String) detail.get("material_id");
                try {
                    String materialSql = "SELECT material_name, safe_stock_min FROM material WHERE material_id = ?";
                    Map<String, Object> material = jdbcTemplate.queryForMap(materialSql, materialId);
                    detail.put("material_name", material.get("material_name"));

                    // 处理 safe_stock_min 类型转换问题
                    Object safeStockMinObj = material.get("safe_stock_min");
                    BigDecimal safeStockMin = null;
                    if (safeStockMinObj instanceof BigDecimal) {
                        safeStockMin = (BigDecimal) safeStockMinObj;
                    } else if (safeStockMinObj instanceof Number) {
                        safeStockMin = new BigDecimal(((Number) safeStockMinObj).doubleValue());
                    } else if (safeStockMinObj != null) {
                        safeStockMin = new BigDecimal(safeStockMinObj.toString());
                    } else {
                        safeStockMin = BigDecimal.ZERO;
                    }
                    detail.put("safe_stock_min", safeStockMin);

                    // 判断状态
                    BigDecimal predictedStock = (BigDecimal) detail.get("predicted_stock");
                    BigDecimal change = (BigDecimal) detail.get("predicted_change");

                    String status;
                    if (predictedStock.compareTo(safeStockMin) < 0) {
                        status = "需要预警";
                    } else if (change.compareTo(BigDecimal.ZERO) < 0) {
                        status = "库存下降";
                    } else {
                        status = "库存上升";
                    }
                    detail.put("prediction_status", status);
                } catch (Exception e) {
                    log.warn("获取物料 {} 信息失败", materialId, e);
                    // 设置默认值
                    detail.put("material_name", materialId);
                    detail.put("safe_stock_min", BigDecimal.ZERO);
                    detail.put("prediction_status", "未知");
                }
            }

            // 统计信息 - 修复类型转换问题
            Map<String, Object> stats = new HashMap<>();
            stats.put("total_predictions", details.size());

            // 使用 long 类型计数，避免 Integer 转换问题
            long lowStockCount = details.stream()
                    .filter(d -> "需要预警".equals(d.get("prediction_status")))
                    .count();
            stats.put("low_stock_count", lowStockCount);
            stats.put("safe_count", details.size() - lowStockCount);

            // 变化统计
            Map<String, Object> changeStats = new HashMap<>();
            changeStats.put("increase_count", increaseCount);
            changeStats.put("decrease_count", decreaseCount);
            changeStats.put("avg_change", validCount > 0 ? totalChange / validCount : 0);

            // 计算最小和最大变化
            BigDecimal minChange = details.stream()
                    .map(d -> (BigDecimal) d.get("predicted_change"))
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            BigDecimal maxChange = details.stream()
                    .map(d -> (BigDecimal) d.get("predicted_change"))
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            changeStats.put("min_change", minChange);
            changeStats.put("max_change", maxChange);

            result.put("code", 200);
            result.put("message", "获取预测详情成功");
            result.put("stats", stats);
            result.put("details", details);
            result.put("changeStats", changeStats);
            result.put("batchId", batchId);

            log.info("从日志表解析出 {} 条预测详情", details.size());

        } catch (Exception e) {
            log.error("获取预测详情失败", e);
            result.put("code", 500);
            result.put("message", "获取预测详情失败: " + e.getMessage());
            result.put("details", Collections.emptyList());
            result.put("stats", Collections.emptyMap());
            result.put("changeStats", Collections.emptyMap());
        }
        return result;
    }

    /**
     * 解析日志消息
     * 示例: "物料 MAT00003: 当前=354.78, 变化=17.32, 预测=372.10"
     */
    private Map<String, Object> parseLogMessage(String message, String materialId) {
        try {
            if (message == null || !message.contains("当前=")) {
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("material_id", materialId);

            // 使用正则表达式提取数值
            String currentPattern = "当前=([0-9.]+)";
            String changePattern = "变化=(-?[0-9.]+)";
            String predictedPattern = "预测=([0-9.]+)";

            java.util.regex.Pattern pCurrent = java.util.regex.Pattern.compile(currentPattern);
            java.util.regex.Pattern pChange = java.util.regex.Pattern.compile(changePattern);
            java.util.regex.Pattern pPredicted = java.util.regex.Pattern.compile(predictedPattern);

            java.util.regex.Matcher mCurrent = pCurrent.matcher(message);
            java.util.regex.Matcher mChange = pChange.matcher(message);
            java.util.regex.Matcher mPredicted = pPredicted.matcher(message);

            if (mCurrent.find()) {
                result.put("current_stock", new BigDecimal(mCurrent.group(1)));
            }

            if (mChange.find()) {
                result.put("predicted_change", new BigDecimal(mChange.group(1)));
            }

            if (mPredicted.find()) {
                result.put("predicted_stock", new BigDecimal(mPredicted.group(1)));
            }

            return result;

        } catch (Exception e) {
            log.warn("解析日志消息失败: {}", message, e);
            return null;
        }
    }

    /**
     * 获取采购推荐清单(仅AI预测结果)
     */
    public List<DB4AIPredictDTO> getPurchaseRecommendations() {
        try {
            List<DB4AIPredictDTO> predictions = new ArrayList<>();

            // 🔥 只查询AI预测结果（stock_alert中的数据）
            String predictionSql = "SELECT " +
                    "m.material_id, " +
                    "m.material_name, " +
                    "m.current_stock, " +
                    "m.safe_stock_min, " +
                    "m.safe_stock_max, " +
                    "sa.current_stock as predicted_stock, " +
                    "sa.safe_threshold, " +
                    "(sa.current_stock - m.current_stock) as predicted_change " +
                    "FROM stock_alert sa " +
                    "JOIN material m ON sa.material_id = m.material_id " +
                    "WHERE sa.alert_type = '低库存' " +
                    "AND sa.status = '未处理' " +
                    // 🔥 只显示有预测变化的（排除变化为0的）
                    "AND ABS(sa.current_stock - m.current_stock) > 0.01 " +
                    "ORDER BY (m.safe_stock_min - sa.current_stock) DESC";

            log.info("查询AI预测数据...");

            predictions = jdbcTemplate.query(predictionSql, (rs, rowNum) -> {
                DB4AIPredictDTO dto = new DB4AIPredictDTO();
                dto.setMaterialId(rs.getString("material_id"));
                dto.setMaterialName(rs.getString("material_name"));
                dto.setCurrentStock(rs.getBigDecimal("current_stock"));
                dto.setSafeStockMin(rs.getBigDecimal("safe_stock_min"));
                dto.setPredictedStock(rs.getBigDecimal("predicted_stock"));
                dto.setPredictedChange(rs.getBigDecimal("predicted_change"));
                dto.setDayNum(14);
                dto.setPredictionSource("AI预测");
                return dto;
            });

            log.info("获取到 {} 条AI预测数据", predictions.size());

            return predictions;

        } catch (Exception e) {
            log.error("获取采购推荐失败", e);
            return Collections.emptyList();
        }
    }


    /**
     * 获取异常出入库记录
     */
    public List<AnomalyDetectionDTO> getAnomalyRecords(LocalDate startDate, LocalDate endDate, boolean useMock) {
            // 先检查异常检测视图是否存在
            String viewCheckSql = "SELECT COUNT(*) FROM information_schema.views WHERE table_name = 'v_anomaly_inout_loose_4517'";
            Integer viewCount = jdbcTemplate.queryForObject(viewCheckSql, Integer.class);
            if (viewCount == null || viewCount == 0) {
                log.warn("异常检测视图不存在，使用模拟数据");

            }
            StringBuilder sql = new StringBuilder(
                    "SELECT " +
                            "record_id, material_id, material_name, quantity, " +
                            "operator_id, operator_name, operation_time, remark, " +
                            "before_stock, after_stock, cluster, " +
                            "anomaly_reason, z_score " +
                            "FROM v_anomaly_inout_loose_4517 " +
                            "WHERE anomaly_reason LIKE '%超出历史均值%' "
            );
            List<Object> params = new ArrayList<>();

            if (startDate != null) {
                sql.append(" AND operation_time >= ? ");
                params.add(startDate.atStartOfDay());
            }
            if (endDate != null) {
                sql.append(" AND operation_time <= ? ");
                params.add(endDate.atTime(23, 59, 59));
            }
            sql.append(" ORDER BY z_score DESC NULLS LAST LIMIT 100");

            log.info("执行异常检测查询: {}", sql.toString());

            List<AnomalyDetectionDTO> result = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
                AnomalyDetectionDTO dto = new AnomalyDetectionDTO();
                dto.setRecordId(rs.getString("record_id"));
                dto.setMaterialId(rs.getString("material_id"));
                dto.setMaterialName(rs.getString("material_name"));
                dto.setQuantity(rs.getBigDecimal("quantity"));
                dto.setOperatorId(rs.getString("operator_id"));
                dto.setOperatorName(rs.getString("operator_name"));
                dto.setOperationTime(rs.getTimestamp("operation_time").toLocalDateTime());
                dto.setRemark(rs.getString("remark"));
                dto.setBeforeStock(rs.getBigDecimal("before_stock"));
                dto.setAfterStock(rs.getBigDecimal("after_stock"));
                dto.setCluster(rs.getInt("cluster"));
                dto.setAnomalyReason(rs.getString("anomaly_reason"));

                BigDecimal zScore = rs.getBigDecimal("z_score");
                if (!rs.wasNull()) {
                    dto.setZScore(zScore.setScale(2, BigDecimal.ROUND_HALF_UP));
                }

                return dto;
            });

            log.info("获取到 {} 条异常记录", result.size());
            return result;
    }


    /**
     * 获取系统状态和DB4AI组件信息
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            // 1. 检查数据库连接
            status.put("database", "已连接");

            // 2. 检查表数据量
            String[] tables = {"material", "inout_record", "supplier", "stock_alert"};
            Map<String, Integer> tableStats = new HashMap<>();
            for (String table : tables) {
                try {
                    String sql = "SELECT COUNT(*) FROM " + table;
                    Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
                    tableStats.put(table, count != null ? count : 0);
                } catch (Exception e) {
                    tableStats.put(table, 0);
                }
            }
            status.put("tableStats", tableStats);

            // 3. 检查DB4AI组件
            Map<String, Boolean> components = checkDB4AIComponents();
            status.put("db4aiComponents", components);

            // 4. 检查历史数据
            String historySql = "SELECT COUNT(*) FROM inout_record WHERE operation_time >= CURRENT_DATE - INTERVAL '60 days'";
            Integer historyCount = jdbcTemplate.queryForObject(historySql, Integer.class);
            status.put("recentHistoryCount", historyCount != null ? historyCount : 0);

            // 5. 检查预测数据
            String predictionSql = "SELECT COUNT(*) FROM stock_alert WHERE alert_type = '低库存'";
            Integer predictionCount = jdbcTemplate.queryForObject(predictionSql, Integer.class);
            status.put("predictionCount", predictionCount != null ? predictionCount : 0);

            // 6. 检查物料数据
            String materialSql = "SELECT COUNT(*) FROM material WHERE status = '正常' AND safe_stock_min > 0";
            Integer materialCount = jdbcTemplate.queryForObject(materialSql, Integer.class);
            status.put("validMaterialCount", materialCount != null ? materialCount : 0);

            log.info("系统状态检查完成: {}", status);

        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            status.put("error", e.getMessage());
        }
        return status;
    }


    /**
     * 测试DB4AI模型预测
     */
    public Map<String, Object> testDB4AIModels() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("开始测试DB4AI模型...");

            // 1. 测试XGBoost模型
            String xgboostTestSql = "SELECT COUNT(*) FROM V_DAILY_STOCK_CHANGE LIMIT 1";
            try {
                Integer xgboostCount = jdbcTemplate.queryForObject(xgboostTestSql, Integer.class);
                result.put("xgboostModel", xgboostCount != null && xgboostCount > 0 ? "正常" : "无数据");
            } catch (Exception e) {
                result.put("xgboostModel", "不可用: " + e.getMessage());
            }

            // 2. 测试K-means模型
            String kmeansTestSql = "SELECT COUNT(*) FROM V_OUT_RECORD_ARRAY_4517 LIMIT 1";
            try {
                Integer kmeansCount = jdbcTemplate.queryForObject(kmeansTestSql, Integer.class);
                result.put("kmeansModel", kmeansCount != null && kmeansCount > 0 ? "正常" : "无数据");
            } catch (Exception e) {
                result.put("kmeansModel", "不可用: " + e.getMessage());
            }

            // 3. 检查预测存储过程
            String procCheckSql = "SELECT COUNT(*) FROM pg_proc WHERE proname = 'sp_predict_stock_warning'";
            try {
                Integer procCount = jdbcTemplate.queryForObject(procCheckSql, Integer.class);
                result.put("predictionProcedure", procCount != null && procCount > 0 ? "存在" : "不存在");
            } catch (Exception e) {
                result.put("predictionProcedure", "检查失败: " + e.getMessage());
            }

            // 4. 检查视图
            String[] views = {
                    "V_DAILY_STOCK_CHANGE",
                    "V_OUT_RECORD_ARRAY_4517",
                    "V_MATERIAL_OUT_STATS_4517",
                    "V_ANOMALY_INOUT_LOOSE_4517"
            };

            Map<String, Boolean> viewStatus = new HashMap<>();
            for (String view : views) {
                try {
                    String checkSql = "SELECT COUNT(*) FROM information_schema.views WHERE table_name = ?";
                    Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, view.toLowerCase());
                    viewStatus.put(view, count != null && count > 0);
                } catch (Exception e) {
                    viewStatus.put(view, false);
                }
            }
            result.put("viewStatus", viewStatus);

            // 5. 数据统计
            String dataStatsSql = "SELECT " +
                    "(SELECT COUNT(*) FROM material WHERE status = '正常') as material_count, " +
                    "(SELECT COUNT(*) FROM inout_record) as record_count, " +
                    "(SELECT COUNT(*) FROM stock_alert WHERE alert_type = '低库存') as prediction_count";

            Map<String, Object> dataStats = jdbcTemplate.queryForMap(dataStatsSql);
            result.put("dataStats", dataStats);

            result.put("code", 200);
            result.put("message", "模型测试完成");
            result.put("testTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            log.info("DB4AI模型测试结果: {}", result);

        } catch (Exception e) {
            log.error("测试DB4AI模型失败", e);
            result.put("code", 500);
            result.put("message", "测试失败: " + e.getMessage());
        }
        return result;
    }
}
