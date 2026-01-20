// [file name]: DB4AIScheduler.java
package org.example.warehouse_management_sys.Scheduler;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.Service.DB4AIService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@EnableScheduling
public class DB4AIScheduler {

    @Resource
    private DB4AIService db4aiService;

    /**
     * 每周日22:00执行 - 未来2周库存预测 & 生成采购清单
     * 这是核心功能：自动生成"未来2周需采购物料清单"
     */
    @Scheduled(cron = "0 0 22 ? * SUN") // 每周日22:00
    public void weeklyStockPredictionAndPurchaseList() {
        log.info("【自动任务】开始执行每周库存预测和采购清单生成...");
        try {
            // 1. 执行库存预测
            var predictionResult = db4aiService.generateStockPredictions();
            if ("200".equals(predictionResult.get("code").toString())) {
                // 2. 获取采购推荐清单
                var purchaseList = db4aiService.getPurchaseRecommendations();
                // 3. 筛选需要采购的物料
                var needPurchaseItems = purchaseList.stream()
                        .filter(p -> p.getPredictedStock().compareTo(p.getSafeStockMin()) < 0)
                        .toList();
                // 4. 记录日志
                log.info("【自动任务】预测完成，需要采购 {} 个物料", needPurchaseItems.size());

                log.info("【自动任务】每周库存预测和采购清单生成完成");
            } else {
                log.warn("【自动任务】预测失败: {}", predictionResult.get("message"));
            }
        } catch (Exception e) {
            log.error("【自动任务】每周库存预测任务失败", e);
        }
    }

    /**
     * 每天23:30执行 - 异常检测
     */
    @Scheduled(cron = "0 30 23 * * ?") // 每天23:30
    public void dailyAnomalyDetection() {
        log.info("【自动任务】开始执行每日异常检测...");
        try {
            // 检测昨天发生的异常记录
            var anomalies = db4aiService.getAnomalyRecords(
                    java.time.LocalDate.now().minusDays(1),
                    java.time.LocalDate.now().minusDays(1),
                    false
            );
            log.info("【自动任务】发现 {} 条异常出入库记录", anomalies.size());
            // 记录异常详情
            anomalies.forEach(anomaly ->
                    log.warn("【异常记录】单据号: {}, 物料: {}, 原因: {}, Z分数: {}",
                            anomaly.getRecordId(),
                            anomaly.getMaterialName(),
                            anomaly.getAnomalyReason(),
                            anomaly.getZScore()
                    )
            );
        } catch (Exception e) {
            log.error("【自动任务】每日异常检测任务失败", e);
        }
    }

    /**
     * 每月1号2:00执行 - 模型维护和数据清理
     */
    @Scheduled(cron = "0 0 2 1 * ?") // 每月1号2:00
    public void monthlyModelMaintenance() {
        log.info("【自动任务】开始执行月度模型维护...");
        try {
            // 1. 清理旧预测数据（保留最近3个月）
            String cleanupSql = "DELETE FROM stock_alert " +
                    "WHERE create_time < CURRENT_DATE - INTERVAL '90 days' " +
                    "AND alert_type = '低库存'";
            // jdbcTemplate.update(cleanupSql);

            // 2. 重新统计物料数据
            // 3. 可选的模型重新训练

            log.info("【自动任务】月度模型维护完成");
        } catch (Exception e) {
            log.error("【自动任务】月度模型维护失败", e);
        }
    }

    /**
     * 每天1:00执行 - 数据统计快照
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天1:00
    public void dailyStatisticsSnapshot() {
        log.info("【自动任务】开始执行每日数据统计...");
        try {
            // 记录每日库存快照
            String snapshotSql = "INSERT INTO stock_daily_snapshot " +
                    "(snapshot_date, total_materials, total_stock_value, avg_stock_level) " +
                    "SELECT CURRENT_DATE, " +
                    "COUNT(*) as total_materials, " +
                    "SUM(current_stock * unit_price) as total_stock_value, " +
                    "AVG(current_stock) as avg_stock_level " +
                    "FROM material " +
                    "WHERE status = '正常'";
            // jdbcTemplate.update(snapshotSql);

            log.info("【自动任务】每日数据统计完成");
        } catch (Exception e) {
            log.error("【自动任务】每日数据统计失败", e);
        }
    }}

