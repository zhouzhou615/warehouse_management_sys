// [file name]: DB4AIService.java (修复版)
package org.example.warehouse_management_sys.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

@Slf4j
@Service
public class DB4AIService {

    @Resource
    private DataSource dataSource;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 记录每日库存快照
     */
    @Transactional
    public void recordDailyStockSnapshot() {
        String checkTableSql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                "WHERE table_name = 'daily_stock_snapshot')";

        String createTableSql = "CREATE TABLE IF NOT EXISTS daily_stock_snapshot (" +
                "snapshot_id SERIAL PRIMARY KEY, " +
                "material_id VARCHAR(50) NOT NULL, " +
                "snapshot_date DATE NOT NULL, " +
                "stock_quantity DECIMAL(20,2) NOT NULL, " +
                "safe_stock_min DECIMAL(20,2), " +
                "safe_stock_max DECIMAL(20,2), " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE (material_id, snapshot_date)" +
                ")";

        String insertSql = "INSERT INTO daily_stock_snapshot " +
                "(material_id, snapshot_date, stock_quantity, safe_stock_min, safe_stock_max) " +
                "SELECT m.material_id, CURRENT_DATE, m.current_stock, " +
                "m.safe_stock_min, m.safe_stock_max " +
                "FROM material m " +
                "WHERE m.status = '正常' " +
                "ON CONFLICT (material_id, snapshot_date) DO UPDATE SET " +
                "stock_quantity = EXCLUDED.stock_quantity, " +
                "safe_stock_min = EXCLUDED.safe_stock_min, " +
                "safe_stock_max = EXCLUDED.safe_stock_max, " +
                "create_time = CURRENT_TIMESTAMP";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 检查表是否存在
            ResultSet rs = stmt.executeQuery(checkTableSql);
            if (rs.next() && !rs.getBoolean(1)) {
                // 创建表
                stmt.execute(createTableSql);
                log.info("创建 daily_stock_snapshot 表成功");
            }

            // 插入或更新今日快照
            int affectedRows = stmt.executeUpdate(insertSql);
            log.info("记录每日库存快照完成，影响 {} 行", affectedRows);

        } catch (SQLException e) {
            log.error("记录每日库存快照失败", e);
        }
    }

    /**
     * 预测未来2周库存并生成采购预警
     * 使用简单的趋势分析和统计方法，不依赖DB4AI模型
     */
    public List<Map<String, Object>> predictStockAndGenerateAlerts() {
        List<Map<String, Object>> purchaseList = new ArrayList<>();

        try {
            // 1. 确保有足够的历史数据
            ensureHistoricalData();

            // 2. 获取所有正常状态的物料
            String getMaterialsSql = "SELECT m.material_id, m.material_name, m.specification, " +
                    "m.unit, m.current_stock, m.safe_stock_min, m.safe_stock_max, " +
                    "s.supplier_name, s.contact_person, s.phone " +
                    "FROM material m " +
                    "LEFT JOIN supplier s ON m.supplier_id = s.supplier_id " +
                    "WHERE m.status = '正常'";

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(getMaterialsSql)) {

                while (rs.next()) {
                    String materialId = rs.getString("material_id");
                    String materialName = rs.getString("material_name");
                    BigDecimal currentStock = rs.getBigDecimal("current_stock");
                    BigDecimal safeStockMin = rs.getBigDecimal("safe_stock_min");

                    // 3. 使用简单趋势分析预测未来库存
                    double trend = calculateStockTrend(materialId);
                    // 预测未来14天的库存
                    double predictedStock = currentStock.doubleValue() * (1 + trend * 14);

                    if (predictedStock < safeStockMin.doubleValue()) {
                        // 生成采购预警
                        Map<String, Object> purchaseItem = new HashMap<>();
                        purchaseItem.put("material_id", materialId);
                        purchaseItem.put("material_name", materialName);
                        purchaseItem.put("specification", rs.getString("specification"));
                        purchaseItem.put("unit", rs.getString("unit"));
                        purchaseItem.put("current_stock", currentStock);
                        purchaseItem.put("predicted_stock", new BigDecimal(predictedStock));
                        purchaseItem.put("safe_stock_min", safeStockMin);
                        purchaseItem.put("safe_stock_max", rs.getBigDecimal("safe_stock_max"));
                        purchaseItem.put("required_quantity",
                                safeStockMin.subtract(new BigDecimal(predictedStock)));
                        purchaseItem.put("supplier_name", rs.getString("supplier_name"));
                        purchaseItem.put("contact_person", rs.getString("contact_person"));
                        purchaseItem.put("phone", rs.getString("phone"));
                        purchaseItem.put("alert_type", "预测缺货");
                        purchaseItem.put("alert_time", LocalDateTime.now());
                        purchaseItem.put("prediction_method", "趋势分析（简单线性回归）");

                        purchaseList.add(purchaseItem);

                        // 插入到预警表
                        insertStockAlert(purchaseItem);
                    }
                }

                log.info("库存预测完成，发现 {} 个需要采购的物料", purchaseList.size());

            }
        } catch (SQLException e) {
            log.error("预测库存失败", e);
        }

        return purchaseList;
    }

    /**
     * 检测异常出入库记录
     * 使用统计方法（Z-score）检测异常
     */
    /**
     * 检测异常出入库记录
     * 使用统计方法（Z-score）检测异常
     */
    public List<Map<String, Object>> detectAnomalousInoutRecords() {
        List<Map<String, Object>> anomalies = new ArrayList<>();

        try {
            Connection conn = dataSource.getConnection();

            try {
                // 检测最近7天的出库记录
                String detectionSql =
                        "SELECT ir.record_id, ir.material_id, m.material_name, " +
                                "       ir.quantity, ir.operation_time, ir.inout_type, " +
                                "       ir.operator_id, ir.remark " +
                                "FROM inout_record ir " +
                                "JOIN material m ON ir.material_id = m.material_id " +
                                "WHERE ir.inout_type = '出库' " +
                                "AND ir.operation_time >= CURRENT_DATE - INTERVAL '7 days' " +
                                "ORDER BY ir.operation_time DESC";

                Statement stmt = conn.createStatement();

                try {
                    ResultSet rs = stmt.executeQuery(detectionSql);

                    Map<String, List<BigDecimal>> materialOutRecords = new HashMap<>();

                    // 首先收集所有物料的出库记录
                    while (rs.next()) {
                        String materialId = rs.getString("material_id");
                        BigDecimal quantity = rs.getBigDecimal("quantity");

                        materialOutRecords.computeIfAbsent(materialId, k -> new ArrayList<>())
                                .add(quantity);
                    }

                    // 关闭第一个结果集
                    rs.close();

                    // 重新执行查询进行异常检测
                    rs = stmt.executeQuery(detectionSql);

                    try {
                        while (rs.next()) {
                            String materialId = rs.getString("material_id");
                            BigDecimal quantity = rs.getBigDecimal("quantity");

                            // 计算Z-score
                            List<BigDecimal> records = materialOutRecords.get(materialId);
                            if (records != null && records.size() >= 5) { // 至少有5条记录才进行检测
                                double zScore = calculateZScore(quantity, records);

                                // Z-score大于2.5视为异常
                                if (zScore > 2.5) {
                                    Map<String, Object> anomaly = new HashMap<>();
                                    anomaly.put("record_id", rs.getString("record_id"));
                                    anomaly.put("material_id", materialId);
                                    anomaly.put("material_name", rs.getString("material_name"));
                                    anomaly.put("quantity", quantity);
                                    anomaly.put("operation_time", rs.getTimestamp("operation_time").toLocalDateTime());
                                    anomaly.put("inout_type", rs.getString("inout_type"));
                                    anomaly.put("anomaly_score", zScore);
                                    anomaly.put("anomaly_reason", "出库量异常");
                                    anomaly.put("additional_info", String.format(
                                            "出库量 %.2f，Z值 %.2f，超过阈值 2.5",
                                            quantity, zScore));

                                    anomalies.add(anomaly);

                                    // 标记异常记录
                                    markRecordAsAnomalous(rs.getString("record_id"),
                                            String.format("Z值异常: %.2f", zScore));
                                }
                            }
                        }
                    } finally {
                        rs.close();
                    }

                    log.info("异常检测完成，发现 {} 条异常记录", anomalies.size());

                } finally {
                    stmt.close();
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            log.error("异常检测失败", e);
        }

        return anomalies;
    }


    /**
     * 确保有足够的历史数据用于预测
     */
    private void ensureHistoricalData() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 检查是否有历史数据
            String checkDataSql = "SELECT COUNT(*) FROM daily_stock_snapshot " +
                    "WHERE snapshot_date >= CURRENT_DATE - INTERVAL '30 days'";

            ResultSet rs = stmt.executeQuery(checkDataSql);
            if (rs.next()) {
                long count = rs.getLong(1);
                if (count == 0) {
                    // 没有历史数据，生成模拟数据用于演示
                    generateDemoHistoricalData();
                }
            }

        } catch (SQLException e) {
            log.error("检查历史数据失败", e);
        }
    }

    /**
     * 生成演示用的历史数据
     */
    private void generateDemoHistoricalData() {
        log.info("开始生成演示用的历史数据...");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 获取所有正常状态的物料
            String getMaterialsSql = "SELECT material_id FROM material WHERE status = '正常'";
            ResultSet rs = stmt.executeQuery(getMaterialsSql);

            List<String> materialIds = new ArrayList<>();
            while (rs.next()) {
                materialIds.add(rs.getString("material_id"));
            }

            if (materialIds.isEmpty()) {
                log.warn("没有找到正常状态的物料，无法生成历史数据");
                return;
            }

            // 为过去30天生成历史数据
            LocalDate today = LocalDate.now();
            Random random = new Random();

            for (int i = 30; i >= 0; i--) {
                LocalDate date = today.minusDays(i);

                for (String materialId : materialIds) {
                    // 获取物料的当前库存和安全库存
                    String getMaterialSql = String.format(
                            "SELECT current_stock, safe_stock_min, safe_stock_max " +
                                    "FROM material WHERE material_id = '%s'",
                            materialId);

                    try (Statement stmt2 = conn.createStatement();
                         ResultSet rs2 = stmt2.executeQuery(getMaterialSql)) {

                        if (rs2.next()) {
                            BigDecimal currentStock = rs2.getBigDecimal("current_stock");
                            BigDecimal safeMin = rs2.getBigDecimal("safe_stock_min");
                            BigDecimal safeMax = rs2.getBigDecimal("safe_stock_max");

                            // 生成随机波动（±20%）
                            double fluctuation = 0.8 + random.nextDouble() * 0.4;
                            BigDecimal simulatedStock = currentStock.multiply(
                                    new BigDecimal(fluctuation));

                            // 确保不超出安全库存范围太多
                            if (simulatedStock.compareTo(safeMin.multiply(new BigDecimal("0.5"))) < 0) {
                                simulatedStock = safeMin.multiply(new BigDecimal("0.8"));
                            }
                            if (simulatedStock.compareTo(safeMax.multiply(new BigDecimal("1.5"))) > 0) {
                                simulatedStock = safeMax.multiply(new BigDecimal("1.2"));
                            }

                            // 插入历史数据
                            String insertSql = String.format(
                                    "INSERT INTO daily_stock_snapshot " +
                                            "(material_id, snapshot_date, stock_quantity, safe_stock_min, safe_stock_max) " +
                                            "VALUES ('%s', '%s', %.2f, %.2f, %.2f) " +
                                            "ON CONFLICT (material_id, snapshot_date) DO NOTHING",
                                    materialId, date.toString(),
                                    simulatedStock.doubleValue(),
                                    safeMin.doubleValue(),
                                    safeMax.doubleValue());

                            stmt.executeUpdate(insertSql);
                        }
                    }
                }
            }

            log.info("演示历史数据生成完成，为 {} 个物料生成了过去30天的数据", materialIds.size());

        } catch (SQLException e) {
            log.error("生成演示历史数据失败", e);
        }
    }

    /**
     * 计算库存趋势（简单线性回归）
     */
    private double calculateStockTrend(String materialId) {
        String sql = "SELECT snapshot_date, stock_quantity " +
                "FROM daily_stock_snapshot " +
                "WHERE material_id = ? " +
                "AND snapshot_date >= CURRENT_DATE - INTERVAL '30 days' " +
                "ORDER BY snapshot_date";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, materialId);
            ResultSet rs = pstmt.executeQuery();

            List<Double> dates = new ArrayList<>();
            List<Double> stocks = new ArrayList<>();
            int dayCount = 0;

            while (rs.next()) {
                Date snapshotDate = rs.getDate("snapshot_date");
                BigDecimal stock = rs.getBigDecimal("stock_quantity");

                // 使用天数作为x值
                dates.add((double) dayCount);
                stocks.add(stock.doubleValue());
                dayCount++;
            }

            if (dayCount < 5) {
                return 0.0; // 数据不足，返回0趋势
            }

            // 简单线性回归计算趋势
            return calculateLinearRegression(dates, stocks);

        } catch (SQLException e) {
            log.error("计算库存趋势失败: {}", materialId, e);
            return 0.0;
        }
    }

    /**
     * 计算线性回归斜率
     */
    private double calculateLinearRegression(List<Double> x, List<Double> y) {
        int n = x.size();

        // 计算均值
        double sumX = 0.0, sumY = 0.0;
        for (int i = 0; i < n; i++) {
            sumX += x.get(i);
            sumY += y.get(i);
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        // 计算斜率和截距
        double numerator = 0.0;
        double denominator = 0.0;

        for (int i = 0; i < n; i++) {
            numerator += (x.get(i) - meanX) * (y.get(i) - meanY);
            denominator += Math.pow(x.get(i) - meanX, 2);
        }

        if (denominator == 0) {
            return 0.0;
        }

        double slope = numerator / denominator;

        // 返回每日变化率（百分比）
        return slope / meanY;
    }

    /**
     * 计算Z-score
     */
    private double calculateZScore(BigDecimal value, List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        // 计算均值
        double sum = 0.0;
        for (BigDecimal v : values) {
            sum += v.doubleValue();
        }
        double mean = sum / values.size();

        // 计算标准差
        double variance = 0.0;
        for (BigDecimal v : values) {
            variance += Math.pow(v.doubleValue() - mean, 2);
        }
        double stdDev = Math.sqrt(variance / values.size());

        if (stdDev == 0) {
            return 0.0;
        }

        // 计算Z-score
        return Math.abs((value.doubleValue() - mean) / stdDev);
    }

    /**
     * 获取物料名称
     */
    private String getMaterialName(String materialId) throws SQLException {
        String sql = "SELECT material_name FROM material WHERE material_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, materialId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("material_name");
                }
            }
        }
        return materialId;
    }

    /**
     * 插入库存预警
     */
    private void insertStockAlert(Map<String, Object> alertData) {
        // 先检查是否已存在相同预警
        String checkSql = "SELECT COUNT(*) FROM stock_alert " +
                "WHERE material_id = ? AND alert_type = '预测缺货' " +
                "AND status = '未处理' " +
                "AND alert_time >= CURRENT_DATE - INTERVAL '1 day'";

        String insertSql = "INSERT INTO stock_alert " +
                "(material_id, alert_type, current_stock, safe_threshold, alert_time, status) " +
                "VALUES (?, ?, ?, ?, ?, '未处理')";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            String materialId = (String) alertData.get("material_id");
            checkStmt.setString(1, materialId);

            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // 已存在相同预警，不重复插入
                return;
            }

            insertStmt.setString(1, materialId);
            insertStmt.setString(2, (String) alertData.get("alert_type"));
            insertStmt.setBigDecimal(3, (BigDecimal) alertData.get("predicted_stock"));
            insertStmt.setBigDecimal(4, (BigDecimal) alertData.get("safe_stock_min"));
            insertStmt.setTimestamp(5, Timestamp.valueOf((LocalDateTime) alertData.get("alert_time")));

            insertStmt.executeUpdate();
            log.info("插入库存预警: {}", materialId);

        } catch (SQLException e) {
            log.error("插入库存预警失败", e);
        }
    }

    /**
     * 标记异常记录
     */
    private void markRecordAsAnomalous(String recordId, String reason) {
        String sql = "UPDATE inout_record SET remark = COALESCE(remark, '') || ? " +
                "WHERE record_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String remark = " [异常：" + reason + " 检测时间：" + LocalDateTime.now() + "]";
            pstmt.setString(1, remark);
            pstmt.setString(2, recordId);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("标记异常记录失败", e);
        }
    }

    /**
     * 获取未来2周需要采购的物料清单（用于前端展示）
     */
    public List<Map<String, Object>> getPurchaseRecommendations() {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        String sql =
                "SELECT sa.alert_id, sa.material_id, m.material_name, m.specification, m.unit, " +
                        "       sa.current_stock as predicted_stock, sa.safe_threshold, " +
                        "       (sa.safe_threshold - sa.current_stock) as required_quantity, " +
                        "       s.supplier_name, s.contact_person, s.phone, " +
                        "       sa.alert_time, sa.alert_type " +
                        "FROM stock_alert sa " +
                        "JOIN material m ON sa.material_id = m.material_id " +
                        "LEFT JOIN supplier s ON m.supplier_id = s.supplier_id " +
                        "WHERE sa.alert_type = '预测缺货' " +
                        "AND sa.status = '未处理' " +
                        "AND sa.alert_time >= CURRENT_DATE - INTERVAL '7 days' " +
                        "ORDER BY required_quantity DESC";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("alert_id", rs.getInt("alert_id"));
                item.put("material_id", rs.getString("material_id"));
                item.put("material_name", rs.getString("material_name"));
                item.put("specification", rs.getString("specification"));
                item.put("unit", rs.getString("unit"));
                item.put("predicted_stock", rs.getBigDecimal("predicted_stock"));
                item.put("safe_threshold", rs.getBigDecimal("safe_threshold"));
                item.put("required_quantity", rs.getBigDecimal("required_quantity"));
                item.put("supplier_name", rs.getString("supplier_name"));
                item.put("contact_person", rs.getString("contact_person"));
                item.put("phone", rs.getString("phone"));
                item.put("alert_time", rs.getTimestamp("alert_time").toLocalDateTime());
                item.put("alert_type", rs.getString("alert_type"));

                recommendations.add(item);
            }

        } catch (SQLException e) {
            log.error("获取采购推荐失败", e);
        }

        return recommendations;
    }

    /**
     * 清除旧的预测预警
     */
    public void cleanupOldPredictions() {
        String sql = "DELETE FROM stock_alert " +
                "WHERE alert_type = '预测缺货' " +
                "AND alert_time < CURRENT_DATE - INTERVAL '30 days'";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            log.info("清除 {} 条旧的预测预警", deleted);
        } catch (SQLException e) {
            log.error("清除旧预测预警失败", e);
        }
    }

    /**
     * 获取预测准确率统计（演示用）
     */
    public Map<String, Object> getPredictionAccuracy() {
        Map<String, Object> stats = new HashMap<>();

        // 这里可以添加预测准确率统计逻辑
        // 由于是演示系统，我们返回一些模拟数据

        stats.put("total_predictions", 50);
        stats.put("accurate_predictions", 42);
        stats.put("inaccurate_predictions", 8);
        stats.put("accuracy_rate", 84.0);
        stats.put("avg_prediction_error", 15.2);
        stats.put("last_updated", LocalDateTime.now());

        return stats;
    }
}