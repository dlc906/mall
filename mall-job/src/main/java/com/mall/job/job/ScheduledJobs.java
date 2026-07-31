package com.mall.job.job;

import com.mall.job.feign.DistributionFeignClient;
import com.mall.job.feign.OrderFeignClient;
import com.mall.job.feign.ProductFeignClient;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ScheduledJobs {

    @Resource
    private OrderFeignClient orderFeignClient;
    @Resource
    private DistributionFeignClient distributionFeignClient;
    @Resource
    private ProductFeignClient productFeignClient;

    /**
     * 取消超时未支付订单
     * Cron: 每5分钟执行一次
     */
    @XxlJob("cancelUnpaidOrders")
    public void cancelUnpaidOrders() {
        log.info(">>>> XXL-Job: cancelUnpaidOrders start");
        try {
            orderFeignClient.cancelUnpaidOrders();
            log.info(">>>> XXL-Job: cancelUnpaidOrders success");
        } catch (Exception e) {
            log.error(">>>> XXL-Job: cancelUnpaidOrders failed", e);
            throw new RuntimeException("取消超时订单失败", e);
        }
    }

    /**
     * 月度佣金结算
     * Cron: 每月1日凌晨2点执行
     */
    @XxlJob("settleCommission")
    public void settleCommission() {
        log.info(">>>> XXL-Job: settleCommission start");
        try {
            distributionFeignClient.settleCommission();
            log.info(">>>> XXL-Job: settleCommission success");
        } catch (Exception e) {
            log.error(">>>> XXL-Job: settleCommission failed", e);
            throw new RuntimeException("佣金结算失败", e);
        }
    }

    /**
     * 同步商品数据到 Elasticsearch
     * Cron: 每小时执行一次
     */
    @XxlJob("syncProductToES")
    public void syncProductToES() {
        log.info(">>>> XXL-Job: syncProductToES start");
        try {
            productFeignClient.syncToEs();
            log.info(">>>> XXL-Job: syncProductToES success");
        } catch (Exception e) {
            log.error(">>>> XXL-Job: syncProductToES failed", e);
            throw new RuntimeException("ES同步失败", e);
        }
    }

    /**
     * 清理过期Token
     * 每天凌晨3点执行
     */
    @XxlJob("cleanExpiredTokens")
    public void cleanExpiredTokens() {
        log.info(">>>> XXL-Job: cleanExpiredTokens start");
        // Redis keys expire via TTL; this is a placeholder for manual cleanup if needed
        log.info(">>>> XXL-Job: cleanExpiredTokens completed (managed by Redis TTL)");
    }

    /**
     * 健康检查任务
     * 每分钟执行 - 用于验证调度中心连通性
     */
    @XxlJob("healthCheck")
    public void healthCheck() {
        log.debug("XXL-Job health check: {}", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
