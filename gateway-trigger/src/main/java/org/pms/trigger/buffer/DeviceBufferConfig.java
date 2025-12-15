package org.pms.trigger.buffer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 设备数据缓冲队列配置*
 * 配置说明：
 * - dataQueueSize: 设备数据队列容量，建议 = 峰值QPS × 缓冲时间(秒)
 * - commandQueueSize: 指令响应队列容量，指令频率较低，可设置较小
 * - retryDataQueueSize: 重试数据队列容量，建议 = dataQueueSize × 0.5
 * - retryCommandQueueSize: 重试指令队列容量
 * - queueFullThreshold: 队列满载阈值（百分比），超过此值触发告警
 * - monitorIntervalSeconds: 监控任务执行间隔（秒）
 * 
 * @author alcsyooterranf
 * @date 2025-01-24
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "device.buffer")
public class DeviceBufferConfig {

    /**
     * 设备数据队列容量
     * 默认10000，可缓冲25秒的峰值流量(400 QPS)
     */
    private int dataQueueSize = 10000;

    /**
     * 指令响应队列容量
     * 默认5000，指令响应频率较低
     */
    private int commandQueueSize = 5000;

    /**
     * 设备数据重试队列容量
     * 默认5000，用于存储RPC调用失败的数据
     */
    private int retryDataQueueSize = 5000;

    /**
     * 指令响应重试队列容量
     * 默认2000
     */
    private int retryCommandQueueSize = 2000;

    /**
     * 队列满载告警阈值（百分比）
     * 默认0.8，即队列使用率超过80%时触发告警
     */
    private double queueFullThreshold = 0.8;

    /**
     * 队列监控任务执行间隔（秒）
     * 默认10秒
     */
    private int monitorIntervalSeconds = 10;

    /**
     * 批量消费大小
     * 默认1000条/批
     */
    private int batchSize = 1000;

    /**
     * 批量消费间隔（毫秒）
     * 默认100ms
     */
    private int consumeIntervalMs = 100;

    /**
     * 最大重试次数
     * 默认3次
     */
    private int maxRetryTimes = 3;

}

