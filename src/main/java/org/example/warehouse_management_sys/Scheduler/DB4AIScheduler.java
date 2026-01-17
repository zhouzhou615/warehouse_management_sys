//// [file name]: DB4AIScheduler.java
//package org.example.warehouse_management_sys.Scheduler;
//
//import lombok.extern.slf4j.Slf4j;
//import org.example.warehouse_management_sys.Service.DB4AIService;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//
//@Slf4j
//@Component
//public class DB4AIScheduler {
//
//    @Resource
//    private DB4AIService db4aiService;
//
//    /**
//     * 每天凌晨1点记录库存快照
//     */
//    @Scheduled(cron = "0 0 1 * * ?")
//    public void dailyStockSnapshot() {
//        log.info("开始执行每日库存快照任务...");
//        try {
//            db4aiService.recordDailyStock();
//            log.info("每日库存快照任务完成");
//        } catch (Exception e) {
//            log.error("每日库存快照任务失败", e);
//        }
//    }
//
//    /**
//     * 每周一凌晨2点重新训练预测模型
//     */
//    @Scheduled(cron = "0 0 2 ? * MON")
//    public void weeklyModelRetraining() {
//        log.info("开始执行每周模型重训练任务...");
//        try {
//            // 填充最新训练数据
//            db4aiService.populateTrainingData();
//
//            // 创建异常检测模型
//            db4aiService.createAnomalyDetectionModel();
//
//            // 批量创建低库存物料的预测模型
//            int created = db4aiService.batchCreateForecastModels();
//
//            log.info("每周模型重训练完成，创建了{}个预测模型", created);
//        } catch (Exception e) {
//            log.error("每周模型重训练任务失败", e);
//        }
//    }
//
//    /**
//     * 每天凌晨3点进行库存预测和异常检测
//     */
//    @Scheduled(cron = "0 0 3 * * ?")
//    public void dailyPredictionAndDetection() {
//        log.info("开始执行每日预测和检测任务...");
//        try {
//            // 1. 预测采购需求
//            var purchaseNeeds = db4aiService.predictPurchaseNeeds();
//            log.info("采购需求预测完成，发现{}个需要采购的物料", purchaseNeeds.size());
//
//            // 2. 检测异常记录
//            var anomalies = db4aiService.detectAnomalousRecords();
//            log.info("异常检测完成，发现{}条异常记录", anomalies.size());
//
//            log.info("每日预测和检测任务完成");
//        } catch (Exception e) {
//            log.error("每日预测和检测任务失败", e);
//        }
//    }
//}