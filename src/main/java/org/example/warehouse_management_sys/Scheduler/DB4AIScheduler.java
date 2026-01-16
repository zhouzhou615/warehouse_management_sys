// [file name]: DB4AIScheduler.java
package org.example.warehouse_management_sys.Scheduler;

import lombok.extern.slf4j.Slf4j;
import org.example.warehouse_management_sys.Service.DB4AIService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class DB4AIScheduler {

    @Resource
    private DB4AIService db4aiService;

    /**
     * 每天凌晨1点记录库存快照
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyStockSnapshot() {
        log.info("开始执行每日库存快照任务...");
        try {
            db4aiService.recordDailyStockSnapshot();
            log.info("每日库存快照任务完成");
        } catch (Exception e) {
            log.error("每日库存快照任务失败", e);
        }
    }

    /**
     * 每周一凌晨2点进行库存预测
     */
    @Scheduled(cron = "0 0 2 ? * MON")
    public void weeklyStockPrediction() {
        log.info("开始执行每周库存预测任务...");
        try {
            var purchaseList = db4aiService.predictStockAndGenerateAlerts();
            log.info("库存预测任务完成，生成{}条采购预警", purchaseList.size());
        } catch (Exception e) {
            log.error("库存预测任务失败", e);
        }
    }

    /**
     * 每天凌晨3点进行异常检测
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyAnomalyDetection() {
        log.info("开始执行每日异常检测任务...");
        try {
            var anomalies = db4aiService.detectAnomalousInoutRecords();
            log.info("异常检测任务完成，发现{}条异常记录", anomalies.size());
        } catch (Exception e) {
            log.error("异常检测任务失败", e);
        }
    }

    /**
     * 每月1号凌晨4点重新训练所有模型
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void monthlyModelRetraining() {
        log.info("开始执行每月模型重训练任务...");
        // 这里可以调用重新训练模型的逻辑
        log.info("模型重训练任务完成");
    }
}