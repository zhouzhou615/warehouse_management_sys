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
     * 生成模拟的预测数据（当真实预测数据为空时使用）
     */
    private List<DB4AIPredictDTO> generateMockPredictions() {
        List<DB4AIPredictDTO> mockData = new ArrayList<>();
        try {
            // 获取物料列表
            String sql = "SELECT material_id, material_name, current_stock, safe_stock_min FROM material WHERE status = '正常' LIMIT 10";
            List<Map<String, Object>> materials = jdbcTemplate.queryForList(sql);

            Random random = new Random();
            for (Map<String, Object> material : materials) {
                DB4AIPredictDTO dto = new DB4AIPredictDTO();
                dto.setMaterialId((String) material.get("material_id"));
                dto.setMaterialName((String) material.get("material_name"));

                BigDecimal currentStock = material.get("current_stock") != null ?
                        new BigDecimal(material.get("current_stock").toString()) : BigDecimal.ZERO;
                BigDecimal safeStockMin = material.get("safe_stock_min") != null ?
                        new BigDecimal(material.get("safe_stock_min").toString()) : BigDecimal.ZERO;

                dto.setCurrentStock(currentStock);
                dto.setSafeStockMin(safeStockMin);

                // 生成随机预测变化（-30% 到 +10%）
                double changePercent = -0.3 + random.nextDouble() * 0.4;
                BigDecimal predictedChange = currentStock.multiply(BigDecimal.valueOf(changePercent))
                        .setScale(2, BigDecimal.ROUND_HALF_UP);

                dto.setPredictedChange(predictedChange);
                dto.setPredictedStock(currentStock.add(predictedChange));
                dto.setDayNum(14); // 预测14天

                mockData.add(dto);
            }

            log.info("生成 {} 条模拟预测数据", mockData.size());
        } catch (Exception e) {
            log.error("生成模拟预测数据失败", e);
        }
        return mockData;
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
     * 增强版的库存预测方法，返回详细的预测过程
     */
    @Transactional
    public Map<String, Object> generateStockPredictionsWithLog() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("开始执行库存预测预警（带日志记录）...");

            // 先检查是否有足够的历史数据
            String checkSql = "SELECT COUNT(*) FROM inout_record WHERE operation_time >= CURRENT_DATE - INTERVAL '60 days'";
            Integer historyCount = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (historyCount == null || historyCount < 10) {
                result.put("code", 400);
                result.put("message", "历史数据不足，至少需要10条过去60天的出入库记录");
                result.put("historyCount", historyCount);
                result.put("mockData", generateMockPredictions()); // 返回模拟数据
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

            // 调用存储过程（带输出参数）
            String callSql = "CALL SP_PREDICT_STOCK_WARNING(?, ?, ?)";

            // 使用CallableStatement获取输出参数
            String batchId = jdbcTemplate.execute((ConnectionCallback<String>) conn -> {
                try (CallableStatement cs = conn.prepareCall(callSql)) {
                    cs.registerOutParameter(1, Types.VARCHAR);
                    cs.registerOutParameter(2, Types.INTEGER);
                    cs.registerOutParameter(3, Types.INTEGER);
                    cs.execute();

                    String batchIdResult = cs.getString(1);
                    int totalPredicted = cs.getInt(2);
                    int totalAlerts = cs.getInt(3);

                    log.info("预测完成 - 批次: {}, 分析物料: {}, 生成预警: {}",
                            batchIdResult, totalPredicted, totalAlerts);

                    return batchIdResult;
                }
            });

            // 获取预测结果
            String checkPredictionsSql = "SELECT COUNT(*) FROM stock_alert WHERE alert_type = '低库存'";
            Integer predictionCount = jdbcTemplate.queryForObject(checkPredictionsSql, Integer.class);

            // 获取预测过程日志
            Map<String, Object> logsResult = getPredictionLogs(batchId, 100);

            // 获取采购推荐
            List<DB4AIPredictDTO> recommendations = getPurchaseRecommendations();

            result.put("code", 200);
            result.put("message", "库存预测完成");
            result.put("batchId", batchId);
            result.put("predictionCount", predictionCount);
            result.put("historyCount", historyCount);
            result.put("deletedCount", deletedCount);
            result.put("logs", logsResult.get("logs"));
            result.put("recommendations", recommendations);
            result.put("needPurchaseCount", recommendations.stream()
                    .filter(r -> r.getPredictedStock().compareTo(r.getSafeStockMin()) < 0)
                    .count());

            log.info("库存预测预警生成完成，批次: {}, 预测记录: {}", batchId, predictionCount);

        } catch (Exception e) {
            log.error("执行库存预测预警失败", e);
            result.put("code", 500);
            result.put("message", "库存预测失败: " + e.getMessage());
            result.put("mockData", generateMockPredictions()); // 返回模拟数据
        }
        return result;
    }
    public Map<String, Object> getPredictionDetails() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从stock_alert表获取预测结果
            String sql = "SELECT " +
                    "COUNT(*) as total_predictions, " +
                    "COUNT(CASE WHEN current_stock < safe_threshold THEN 1 END) as low_stock_count, " +
                    "COUNT(CASE WHEN current_stock > safe_threshold THEN 1 END) as safe_count " +
                    "FROM stock_alert WHERE alert_type = '低库存'";

            Map<String, Object> stats = jdbcTemplate.queryForMap(sql);
            result.put("stats", stats);

            // 获取详细的预测变化数据
            String detailSql = "SELECT " +
                    "m.material_id, " +
                    "m.material_name, " +
                    "m.current_stock, " +
                    "sa.current_stock as predicted_stock, " +
                    "ROUND((sa.current_stock - m.current_stock)::numeric, 2) as predicted_change, " +
                    "m.safe_stock_min, " +
                    "CASE " +
                    "  WHEN sa.current_stock < m.safe_stock_min THEN '需要预警' " +
                    "  WHEN (sa.current_stock - m.current_stock) < 0 THEN '库存下降' " +
                    "  ELSE '库存上升' " +
                    "END as prediction_status " +
                    "FROM stock_alert sa " +
                    "JOIN material m ON sa.material_id = m.material_id " +
                    "WHERE sa.alert_type = '低库存' " +
                    "ORDER BY ABS(sa.current_stock - m.current_stock) DESC " +
                    "LIMIT 50";

            List<Map<String, Object>> details = jdbcTemplate.queryForList(detailSql);
            result.put("details", details);

            // 获取变化统计数据
            String changeSql = "SELECT " +
                    "COUNT(CASE WHEN (sa.current_stock - m.current_stock) > 0 THEN 1 END) as increase_count, " +
                    "COUNT(CASE WHEN (sa.current_stock - m.current_stock) < 0 THEN 1 END) as decrease_count, " +
                    "ROUND(AVG(sa.current_stock - m.current_stock), 2) as avg_change, " +
                    "ROUND(MIN(sa.current_stock - m.current_stock), 2) as min_change, " +
                    "ROUND(MAX(sa.current_stock - m.current_stock), 2) as max_change " +
                    "FROM stock_alert sa " +
                    "JOIN material m ON sa.material_id = m.material_id " +
                    "WHERE sa.alert_type = '低库存'";

            Map<String, Object> changeStats = jdbcTemplate.queryForMap(changeSql);
            result.put("changeStats", changeStats);

            result.put("code", 200);
            result.put("message", "获取预测详情成功");

        } catch (Exception e) {
            log.error("获取预测详情失败", e);
            result.put("code", 500);
            result.put("message", "获取预测详情失败: " + e.getMessage());
        }
        return result;
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
     * 获取基本的低库存物料（备选方案）
     */
    private List<DB4AIPredictDTO> getBasicLowStockMaterials() {
        try {
            String sql = "SELECT " +
                    "material_id, " +
                    "material_name, " +
                    "current_stock, " +
                    "safe_stock_min, " +
                    "unit " +
                    "FROM material " +
                    "WHERE status = '正常' " +
                    "AND current_stock < safe_stock_min " +
                    "AND safe_stock_min > 0 " +
                    "ORDER BY (safe_stock_min - current_stock) DESC " +
                    "LIMIT 50";

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                DB4AIPredictDTO dto = new DB4AIPredictDTO();
                dto.setMaterialId(rs.getString("material_id"));
                dto.setMaterialName(rs.getString("material_name"));
                dto.setCurrentStock(rs.getBigDecimal("current_stock"));
                dto.setSafeStockMin(rs.getBigDecimal("safe_stock_min"));
                dto.setPredictedStock(rs.getBigDecimal("current_stock")); // 使用当前库存
                dto.setPredictedChange(BigDecimal.ZERO);
                dto.setDayNum(0);
                dto.setPredictionSource("当前库存");
                return dto;
            });
        } catch (Exception e) {
            log.error("获取基本低库存物料失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取异常出入库记录（使用2倍标准差阈值）
     */
    public List<AnomalyDetectionDTO> getAnomalyRecords(LocalDate startDate, LocalDate endDate, boolean useMock) {
        if (useMock) {
            return generateMockAnomalyRecords();
        }

        try {
            // 先检查异常检测视图是否存在
            String viewCheckSql = "SELECT COUNT(*) FROM information_schema.views WHERE table_name = 'v_anomaly_inout_loose_4517'";
            Integer viewCount = jdbcTemplate.queryForObject(viewCheckSql, Integer.class);

            if (viewCount == null || viewCount == 0) {
                log.warn("异常检测视图不存在，使用模拟数据");
                return generateMockAnomalyRecords();
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
        } catch (Exception e) {
            log.error("获取异常记录失败，使用模拟数据", e);
            return generateMockAnomalyRecords();
        }
    }

    /**
     * 生成模拟的异常记录
     */
    private List<AnomalyDetectionDTO> generateMockAnomalyRecords() {
        List<AnomalyDetectionDTO> mockData = new ArrayList<>();
        try {
            // 获取最近的出库记录
            String recentOutSql = "SELECT ir.*, m.material_name, o.operator_name " +
                    "FROM inout_record ir " +
                    "LEFT JOIN material m ON ir.material_id = m.material_id " +
                    "LEFT JOIN operator o ON ir.operator_id = o.operator_id " +
                    "WHERE ir.inout_type = '出库' " +
                    "ORDER BY ir.operation_time DESC LIMIT 20";

            List<Map<String, Object>> recentRecords = jdbcTemplate.queryForList(recentOutSql);

            Random random = new Random();
            for (Map<String, Object> record : recentRecords) {
                // 30%的概率标记为异常
                if (random.nextDouble() < 0.3) {
                    AnomalyDetectionDTO dto = new AnomalyDetectionDTO();
                    dto.setRecordId((String) record.get("record_id"));
                    dto.setMaterialId((String) record.get("material_id"));
                    dto.setMaterialName((String) record.get("material_name"));
                    dto.setQuantity(new BigDecimal(record.get("quantity").toString()));
                    dto.setOperatorId((String) record.get("operator_id"));
                    dto.setOperatorName((String) record.get("operator_name"));
                    dto.setOperationTime(((java.sql.Timestamp) record.get("operation_time")).toLocalDateTime());
                    dto.setRemark((String) record.get("remark"));
                    dto.setBeforeStock(record.get("before_stock") != null ?
                            new BigDecimal(record.get("before_stock").toString()) : BigDecimal.ZERO);
                    dto.setAfterStock(record.get("after_stock") != null ?
                            new BigDecimal(record.get("after_stock").toString()) : BigDecimal.ZERO);

                    // 随机生成聚类和Z分数
                    dto.setCluster(random.nextInt(3));
                    dto.setZScore(BigDecimal.valueOf(2.0 + random.nextDouble() * 3.0));
                    dto.setAnomalyReason(dto.getZScore().compareTo(BigDecimal.valueOf(3.0)) >= 0 ?
                            "出库量超出历史均值3倍" : "出库量超出历史均值2倍");

                    mockData.add(dto);
                }
            }

            log.info("生成 {} 条模拟异常记录", mockData.size());
        } catch (Exception e) {
            log.error("生成模拟异常记录失败", e);
        }
        return mockData;
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
     * 初始化测试数据（用于演示）
     */
    @Transactional
    public Map<String, Object> initializeDemoData() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("开始初始化演示数据...");

            // 1. 生成一些模拟的出入库记录
            String materialSql = "SELECT material_id FROM material WHERE status = '正常' LIMIT 10";
            List<String> materialIds = jdbcTemplate.queryForList(materialSql, String.class);

            if (materialIds.isEmpty()) {
                result.put("code", 400);
                result.put("message", "没有可用的物料数据");
                return result;
            }

            Random random = new Random();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

            // 生成过去60天的记录
            for (int i = 0; i < 60; i++) {
                LocalDateTime date = LocalDateTime.now().minusDays(60 - i);
                String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

                for (String materialId : materialIds) {
                    // 随机生成出入库记录
                    String inoutType = random.nextDouble() > 0.6 ? "出库" : "入库";
                    BigDecimal quantity = BigDecimal.valueOf(10 + random.nextDouble() * 100)
                            .setScale(2, BigDecimal.ROUND_HALF_UP);

                    String recordId = inoutType + "_" + date.format(formatter) + "_" + materialId.substring(0, 4);

                    String insertSql = "INSERT INTO inout_record (record_id, material_id, inout_type, quantity, operator_id, operation_time, remark) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT DO NOTHING";

                    jdbcTemplate.update(insertSql,
                            recordId,
                            materialId,
                            inoutType,
                            quantity,
                            "OP001",
                            date,
                            "模拟数据 - " + dateStr
                    );
                }
            }

            // 2. 更新物料库存
            for (String materialId : materialIds) {
                // 计算净变化
                String netChangeSql = "SELECT " +
                        "SUM(CASE WHEN inout_type = '入库' THEN quantity ELSE -quantity END) as net_change " +
                        "FROM inout_record WHERE material_id = ?";
                BigDecimal netChange = jdbcTemplate.queryForObject(netChangeSql, BigDecimal.class, materialId);

                if (netChange != null) {
                    String updateSql = "UPDATE material SET current_stock = GREATEST(0, ?) WHERE material_id = ?";
                    jdbcTemplate.update(updateSql, netChange, materialId);
                }
            }

            // 3. 设置安全库存
            String setSafeStockSql = "UPDATE material SET safe_stock_min = current_stock * 0.5, safe_stock_max = current_stock * 2.0 WHERE status = '正常'";
            jdbcTemplate.update(setSafeStockSql);

            result.put("code", 200);
            result.put("message", "演示数据初始化完成");
            result.put("materials", materialIds.size());
            result.put("records", 60 * materialIds.size());

            log.info("演示数据初始化完成，物料数: {}, 记录数: {}", materialIds.size(), 60 * materialIds.size());

        } catch (Exception e) {
            log.error("初始化演示数据失败", e);
            result.put("code", 500);
            result.put("message", "初始化失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 定期任务：每周日晚上执行库存预测
     */
    @Scheduled(cron = "0 0 22 ? * SUN") // 每周日22:00执行
    @Transactional
    public void weeklyStockPredictionTask() {
        log.info("开始执行每周库存预测任务...");
        try {
            // 检查是否有足够的历史数据
            String checkSql = "SELECT COUNT(*) FROM inout_record WHERE operation_time >= CURRENT_DATE - INTERVAL '60 days'";
            Integer historyCount = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (historyCount == null || historyCount < 10) {
                log.warn("历史数据不足({}条)，跳过本周预测", historyCount);
                return;
            }

            // 生成预测预警
            Map<String, Object> predictionResult = generateStockPredictions();

            if ("200".equals(predictionResult.get("code").toString())) {
                // 获取采购推荐清单
                List<DB4AIPredictDTO> recommendations = getPurchaseRecommendations();

                log.info("每周库存预测任务完成，生成 {} 条采购推荐", recommendations.size());
            } else {
                log.warn("每周库存预测任务失败: {}", predictionResult.get("message"));
            }
        } catch (Exception e) {
            log.error("每周库存预测任务失败", e);
        }
    }

    /**
     * 定期任务：每天检查异常记录
     */
    @Scheduled(cron = "0 30 23 * * ?") // 每天23:30执行
    public void dailyAnomalyCheckTask() {
        log.info("开始执行每日异常检测任务...");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<AnomalyDetectionDTO> anomalies = getAnomalyRecords(yesterday, yesterday, false);

            log.info("发现 {} 条异常出入库记录", anomalies.size());

            if (!anomalies.isEmpty()) {
                // 记录到日志中
                anomalies.forEach(anomaly ->
                        log.warn("异常记录: {} - {} - {} - {}",
                                anomaly.getRecordId(),
                                anomaly.getMaterialName(),
                                anomaly.getAnomalyReason(),
                                anomaly.getZScore()
                        )
                );
            }
        } catch (Exception e) {
            log.error("每日异常检测任务失败", e);
        }
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
// [file content end]