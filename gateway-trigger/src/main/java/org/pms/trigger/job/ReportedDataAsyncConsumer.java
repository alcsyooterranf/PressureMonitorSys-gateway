package org.pms.trigger.job;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.pms.api.dto.command.CommandRespDTO;
import org.pms.api.dto.devicedata.DeviceDataDTO;
import org.pms.domain.command.dto.BaseCommandRespDataDTO;
import org.pms.domain.devicedata.dto.BaseDeviceDataDTO;
import org.pms.trigger.buffer.DataBuffer;
import org.pms.trigger.buffer.DataBufferConfig;
import org.pms.trigger.converter.DomainToApiConverter;
import org.pms.trigger.feign.ICommandClient;
import org.pms.trigger.feign.IDeviceClient;
import org.pms.types.Response;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备数据上报异步消费者, 包含设备数据和指令响应的消费
 * 定时从本地队列中批量取出数据，通过Feign批量调用后端服务
 * <p>
 * 架构设计：
 * 本地队列 → 定时任务(100ms) → 批量取出(1000条) → Feign批量RPC → 后端服务
 * ↓ 失败
 * 重试队列(3次) → Redis Stream持久化
 *
 * @author alcsyooterranf
 * @date 2025-01-23
 */
@Slf4j
@Component
@EnableScheduling
public class ReportedDataAsyncConsumer {
	
	/**
	 * 重试次数记录
	 * key: deviceId, value: 重试次数
	 */
	private final Map<String, Integer> retryCountMap = new HashMap<>();
	@Resource
	private DataBuffer dataBuffer;
	@Resource
	private IDeviceClient deviceClient;
	@Resource
	private ICommandClient commandClient;
	@Resource
	private DomainToApiConverter domainToApiConverter;
	@Resource
	private DataBufferConfig config;
	
	// ==================== 设备数据消费 ====================
	
	/**
	 * 定时消费设备数据队列
	 * 执行间隔和批量大小可配置
	 */
	@Scheduled(fixedDelayString = "${device.buffer.consume-interval-ms:100}")
	public void consumeDeviceDataBatch() {
		try {
			// 1. 批量取出数据（Domain层DTO）
			List<BaseDeviceDataDTO> domainBatch = dataBuffer.drainDataBatch(config.getBatchSize());
			
			if (domainBatch.isEmpty()) {
				return;
			}
			
			log.info("开始消费设备数据批次，数量: {}", domainBatch.size());
			long start = System.currentTimeMillis();
			
			// 2. 转换为API层DTO
			List<DeviceDataDTO> apiBatch = domainBatch.stream()
					.map(domainToApiConverter::convertDeviceData)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			
			if (apiBatch.isEmpty()) {
				log.warn("转换后的API DTO列表为空，跳过本批次");
				return;
			}
			
			// 3. 批量调用后端RPC接口
			try {
				Response<Boolean> rpcResponse = deviceClient.batchHandleDeviceData(apiBatch);
				
				if (!rpcResponse.getData()) {
					log.error("批量保存设备数据失败: {}", rpcResponse.getMessage());
					// 失败的数据放入重试队列
					dataBuffer.offerBatchToRetryData(domainBatch);
				} else {
					long end = System.currentTimeMillis();
					log.info("批量保存设备数据成功，数量: {}, 耗时: {}ms", apiBatch.size(), end - start);
				}
			} catch (Exception e) {
				log.error("批量调用后端服务异常", e);
				// 异常的数据放入重试队列
				dataBuffer.offerBatchToRetryData(domainBatch);
			}
			
		} catch (Exception e) {
			log.error("消费设备数据批次异常", e);
		}
	}
	
	/**
	 * 定时重试失败的设备数据
	 * 每5秒执行一次
	 */
	@Scheduled(fixedDelay = 5000)
	public void retryFailedDeviceData() {
		try {
			// 1. 批量取出重试数据（Domain层DTO）
			List<BaseDeviceDataDTO> retryBatch =
					dataBuffer.drainRetryDataBatch(config.getBatchSize());
			
			if (retryBatch.isEmpty()) {
				return;
			}
			
			log.info("开始重试设备数据，数量: {}", retryBatch.size());
			
			// 2. 逐条重试（重试时不批量，避免一条失败影响整批）
			for (BaseDeviceDataDTO domainData : retryBatch) {
				String deviceId = domainData.getDeviceId();
				int retryCount = retryCountMap.getOrDefault(deviceId, 0);
				
				if (retryCount >= config.getMaxRetryTimes()) {
					log.error("设备数据重试次数超限，放弃重试: deviceId={}, retryCount={}, maxRetryTimes={}",
							deviceId, retryCount, config.getMaxRetryTimes());
					
					// TODO: 降级策略 - 重试次数超限时，保存到Redis Stream持久化，防止数据丢失
					// saveToRedisStream(domainData);
					
					retryCountMap.remove(deviceId);
					continue;
				}
				
				try {
					// 转换为API层DTO
					DeviceDataDTO apiData = domainToApiConverter.convertDeviceData(domainData);
					
					if (apiData == null) {
						log.warn("转换API DTO失败，跳过重试: deviceId={}", deviceId);
						retryCountMap.remove(deviceId);
						continue;
					}
					
					Response<Boolean> rpcResponse = deviceClient.handleDeviceData(apiData);
					
					if (!rpcResponse.getData()) {
						log.warn("设备数据重试失败: deviceId={}, retryCount={}", deviceId, retryCount + 1);
						retryCountMap.put(deviceId, retryCount + 1);
						dataBuffer.offerToRetryData(domainData);
					} else {
						log.info("设备数据重试成功: deviceId={}, retryCount={}", deviceId, retryCount + 1);
						retryCountMap.remove(deviceId);
					}
				} catch (Exception e) {
					log.error("设备数据重试异常: deviceId={}, retryCount={}", deviceId, retryCount + 1, e);
					retryCountMap.put(deviceId, retryCount + 1);
					dataBuffer.offerToRetryData(domainData);
				}
			}
			
		} catch (Exception e) {
			log.error("重试设备数据异常", e);
		}
	}
	
	// ==================== 指令响应消费 ====================
	
	/**
	 * 定时消费指令响应队列
	 * 执行间隔和批量大小可配置
	 */
	@Scheduled(fixedDelayString = "${device.buffer.consume-interval-ms:100}")
	public void consumeCommandResponseBatch() {
		try {
			// 1. 批量取出数据（Domain层DTO）
			List<BaseCommandRespDataDTO> domainBatch = dataBuffer.drainCommandBatch(config.getBatchSize());
			
			if (domainBatch.isEmpty()) {
				return;
			}
			
			log.info("开始消费指令响应批次，数量: {}", domainBatch.size());
			long start = System.currentTimeMillis();
			
			// 2. 转换为API层DTO
			List<CommandRespDTO> apiBatch = domainBatch.stream()
					.map(domainToApiConverter::convertCommandResponse)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			
			if (apiBatch.isEmpty()) {
				log.warn("转换后的API DTO列表为空，跳过本批次");
				return;
			}
			
			// 3. 批量调用后端RPC接口
			try {
				Response<Boolean> rpcResponse = commandClient.batchHandleCommandResp(apiBatch);
				
				if (!rpcResponse.getData()) {
					log.error("批量保存指令响应失败: {}", rpcResponse.getMessage());
					// 失败的数据放入重试队列
					dataBuffer.offerBatchToRetryCommand(domainBatch);
				} else {
					long end = System.currentTimeMillis();
					log.info("批量保存指令响应成功，数量: {}, 耗时: {}ms", apiBatch.size(), end - start);
				}
			} catch (Exception e) {
				log.error("批量调用后端服务异常", e);
				// 异常的数据放入重试队列
				dataBuffer.offerBatchToRetryCommand(domainBatch);
			}
			
		} catch (Exception e) {
			log.error("消费指令响应批次异常", e);
		}
	}
	
	/**
	 * 定时重试失败的指令响应
	 * 每5秒执行一次
	 */
	@Scheduled(fixedDelay = 5000)
	public void retryFailedCommandResponse() {
		try {
			// 1. 批量取出重试数据（Domain层DTO）
			List<BaseCommandRespDataDTO> retryBatch =
					dataBuffer.drainRetryCommandBatch(500);
			
			if (retryBatch.isEmpty()) {
				return;
			}
			
			log.info("开始重试指令响应，数量: {}", retryBatch.size());
			
			// 2. 逐条重试
			for (BaseCommandRespDataDTO domainCommand : retryBatch) {
				String key = domainCommand.getDeviceId() + "_" + domainCommand.getTaskId();
				int retryCount = retryCountMap.getOrDefault(key, 0);
				
				if (retryCount >= config.getMaxRetryTimes()) {
					log.error("指令响应重试次数超限，放弃重试: deviceId={}, taskId={}, retryCount={}, maxRetryTimes={}",
							domainCommand.getDeviceId(), domainCommand.getTaskId(), retryCount, config.getMaxRetryTimes());
					
					// TODO: 降级策略 - 重试次数超限时，保存到Redis Stream持久化，防止数据丢失
					// saveCommandToRedisStream(domainCommand);
					
					retryCountMap.remove(key);
					continue;
				}
				
				try {
					// 转换为API层DTO
					CommandRespDTO apiCommand = domainToApiConverter.convertCommandResponse(domainCommand);
					
					if (apiCommand == null) {
						log.warn("转换API DTO失败，跳过重试: deviceId={}, taskId={}",
								domainCommand.getDeviceId(), domainCommand.getTaskId());
						retryCountMap.remove(key);
						continue;
					}
					
					Response<Boolean> rpcResponse = commandClient.handleCommandResp(apiCommand);
					
					if (!rpcResponse.getData()) {
						log.warn("指令响应重试失败: deviceId={}, taskId={}, retryCount={}", domainCommand.getDeviceId(), domainCommand.getTaskId(), retryCount + 1);
						retryCountMap.put(key, retryCount + 1);
						dataBuffer.offerToRetryCommand(domainCommand);
					} else {
						log.info("指令响应重试成功: deviceId={}, taskId={}, retryCount={}", domainCommand.getDeviceId(), domainCommand.getTaskId(), retryCount + 1);
						retryCountMap.remove(key);
					}
				} catch (Exception e) {
					log.error("指令响应重试异常: deviceId={}, taskId={}, retryCount={}", domainCommand.getDeviceId(), domainCommand.getTaskId(), retryCount + 1, e);
					retryCountMap.put(key, retryCount + 1);
					dataBuffer.offerToRetryCommand(domainCommand);
				}
			}
			
		} catch (Exception e) {
			log.error("重试指令响应异常", e);
		}
	}
	
	// ==================== 监控统计 ====================
	
	/**
	 * 队列监控任务
	 * 定时监控队列状态，打印日志并检查告警
	 * 执行间隔可配置，默认10秒
	 */
	@Scheduled(fixedDelayString = "${device.buffer.monitor-interval-seconds:10}000")
	public void monitorQueueStatus() {
		try {
			// 1. 打印队列状态
			dataBuffer.logQueueStatus();
			
			// 2. 打印重试计数器状态
			log.info("重试计数器状态 - 大小: {}", retryCountMap.size());
			
			// 3. 检查告警
			List<String> alerts = dataBuffer.checkAlerts();
			if (!alerts.isEmpty()) {
				for (String alert : alerts) {
					log.warn(alert);
				}
				
				// TODO: 告警通知 - 可以接入钉钉、企业微信、邮件等告警渠道
				// sendAlertNotification(alerts);
			}
			
			// 4. 获取监控指标（可用于Prometheus等监控系统）
			DataBuffer.QueueMetrics metrics = dataBuffer.getMetrics();
			
			// TODO: 监控指标上报 - 可以上报到Prometheus、InfluxDB等监控系统
			// reportMetrics(metrics);
			
		} catch (Exception e) {
			log.error("队列监控任务执行异常", e);
		}
	}
	
}

