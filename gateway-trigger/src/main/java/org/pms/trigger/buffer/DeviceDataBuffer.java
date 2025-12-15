package org.pms.trigger.buffer;

import jakarta.annotation.Resource;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.pms.domain.command.dto.BaseCommandResponseDTO;
import org.pms.domain.dataReport.dto.BaseDataChangeReportDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 设备数据本地缓冲队列
 * 用于异步处理设备数据，防止RPC调用阻塞网关响应
 * <p>
 * 架构设计：
 * AEP平台 → 网关Controller → 本地队列(快速返回) → 异步消费者 → 批量RPC调用后端
 * <p>
 * 性能优化：
 * - 配置化：队列容量可通过配置文件调整
 * - 监控指标：提供队列使用率、告警等监控方法
 * - 降级策略：队列满时可降级到Redis Stream（TODO）
 *
 * @author alcsyooterranf
 * @date 2025-01-23
 */
@Slf4j
@Component
public class DeviceDataBuffer {
	
	@Resource
	private DeviceBufferConfig config;
	
	/**
	 * 设备数据队列
	 * 容量可配置，默认10000
	 */
	private volatile BlockingQueue<BaseDataChangeReportDTO> dataQueue;
	
	/**
	 * 指令响应队列
	 * 容量可配置，默认5000
	 */
	private BlockingQueue<BaseCommandResponseDTO> commandQueue;
	
	/**
	 * 设备数据重试队列
	 * 容量可配置，默认5000
	 */
	private BlockingQueue<BaseDataChangeReportDTO> retryDataQueue;
	
	/**
	 * 指令响应重试队列
	 * 容量可配置，默认2000
	 */
	private BlockingQueue<BaseCommandResponseDTO> retryCommandQueue;
	
	/**
	 * 初始化队列（延迟初始化，等待配置注入）
	 */
	private void initQueuesIfNeeded() {
		if (dataQueue == null) {
			synchronized (this) {
				if (dataQueue == null) {
					dataQueue = new LinkedBlockingQueue<>(config.getDataQueueSize());
					commandQueue = new LinkedBlockingQueue<>(config.getCommandQueueSize());
					retryDataQueue = new LinkedBlockingQueue<>(config.getRetryDataQueueSize());
					retryCommandQueue = new LinkedBlockingQueue<>(config.getRetryCommandQueueSize());
					log.info("设备数据缓冲队列初始化完成 - 数据队列:{}, 指令队列:{}, 重试数据:{}, 重试指令:{}",
							config.getDataQueueSize(),
							config.getCommandQueueSize(),
							config.getRetryDataQueueSize(),
							config.getRetryCommandQueueSize());
				}
			}
		}
	}
	
	// ==================== 设备数据队列操作 ====================
	
	/**
	 * 添加设备数据到队列
	 *
	 * @param data 设备数据
	 * @return true-成功, false-队列已满
	 */
	public boolean offerData(BaseDataChangeReportDTO data) {
		initQueuesIfNeeded();
		boolean success = dataQueue.offer(data);
		if (!success) {
			log.warn("设备数据队列已满，数据被拒绝: deviceId={}, queueSize={}/{}",
					data.getDeviceId(), dataQueue.size(), config.getDataQueueSize());
			
			// TODO: 降级策略 - 将数据保存到Redis Stream，防止数据丢失
			// saveToRedisStream(data);
		}
		return success;
	}
	
	/**
	 * 批量取出设备数据
	 *
	 * @param maxSize 最大取出数量
	 * @return 设备数据列表
	 */
	public List<BaseDataChangeReportDTO> drainDataBatch(int maxSize) {
		initQueuesIfNeeded();
		List<BaseDataChangeReportDTO> batch = new ArrayList<>(maxSize);
		dataQueue.drainTo(batch, maxSize);
		return batch;
	}
	
	/**
	 * 获取设备数据队列当前大小
	 */
	public int getDataQueueSize() {
		initQueuesIfNeeded();
		return dataQueue.size();
	}
	
	/**
	 * 获取设备数据队列容量
	 */
	public int getDataQueueCapacity() {
		return config.getDataQueueSize();
	}
	
	/**
	 * 获取设备数据队列使用率
	 *
	 * @return 使用率（0.0 ~ 1.0）
	 */
	public double getDataQueueUsageRate() {
		initQueuesIfNeeded();
		return (double) dataQueue.size() / config.getDataQueueSize();
	}
	
	/**
	 * 设备数据队列是否接近满载
	 *
	 * @return true-接近满载（超过阈值）
	 */
	public boolean isDataQueueNearFull() {
		return getDataQueueUsageRate() > config.getQueueFullThreshold();
	}
	
	// ==================== 指令响应队列操作 ====================
	
	/**
	 * 添加指令响应到队列
	 *
	 * @param command 指令响应
	 * @return true-成功, false-队列已满
	 */
	public boolean offerCommand(BaseCommandResponseDTO command) {
		initQueuesIfNeeded();
		boolean success = commandQueue.offer(command);
		if (!success) {
			log.warn("指令响应队列已满，数据被拒绝: deviceId={}, taskId={}, queueSize={}/{}",
					command.getDeviceId(), command.getTaskId(), commandQueue.size(), config.getCommandQueueSize());
			
			// TODO: 降级策略 - 将指令响应保存到Redis Stream
			// saveCommandToRedisStream(command);
		}
		return success;
	}
	
	/**
	 * 批量取出指令响应
	 *
	 * @param maxSize 最大取出数量
	 * @return 指令响应列表
	 */
	public List<BaseCommandResponseDTO> drainCommandBatch(int maxSize) {
		initQueuesIfNeeded();
		List<BaseCommandResponseDTO> batch = new ArrayList<>(maxSize);
		commandQueue.drainTo(batch, maxSize);
		return batch;
	}
	
	/**
	 * 获取指令响应队列当前大小
	 */
	public int getCommandQueueSize() {
		initQueuesIfNeeded();
		return commandQueue.size();
	}
	
	/**
	 * 获取指令响应队列容量
	 */
	public int getCommandQueueCapacity() {
		return config.getCommandQueueSize();
	}
	
	/**
	 * 获取指令响应队列使用率
	 *
	 * @return 使用率（0.0 ~ 1.0）
	 */
	public double getCommandQueueUsageRate() {
		initQueuesIfNeeded();
		return (double) commandQueue.size() / config.getCommandQueueSize();
	}
	
	/**
	 * 指令响应队列是否接近满载
	 *
	 * @return true-接近满载（超过阈值）
	 */
	public boolean isCommandQueueNearFull() {
		return getCommandQueueUsageRate() > config.getQueueFullThreshold();
	}
	
	// ==================== 重试队列操作 ====================
	
	/**
	 * 添加设备数据到重试队列
	 *
	 * @param data 设备数据
	 */
	public void offerToRetryData(BaseDataChangeReportDTO data) {
		initQueuesIfNeeded();
		boolean success = retryDataQueue.offer(data);
		if (!success) {
			log.error("重试队列已满，数据将丢失: deviceId={}, queueSize={}/{}",
					data.getDeviceId(), retryDataQueue.size(), config.getRetryDataQueueSize());
			
			// TODO: 降级策略 - 重试队列满时，保存到Redis Stream持久化
			// saveToRedisStream(data);
		}
	}
	
	/**
	 * 批量添加设备数据到重试队列
	 *
	 * @param dataList 设备数据列表
	 */
	public void offerBatchToRetryData(List<BaseDataChangeReportDTO> dataList) {
		dataList.forEach(this::offerToRetryData);
	}
	
	/**
	 * 批量取出重试设备数据
	 *
	 * @param maxSize 最大取出数量
	 * @return 设备数据列表
	 */
	public List<BaseDataChangeReportDTO> drainRetryDataBatch(int maxSize) {
		initQueuesIfNeeded();
		List<BaseDataChangeReportDTO> batch = new ArrayList<>(maxSize);
		retryDataQueue.drainTo(batch, maxSize);
		return batch;
	}
	
	/**
	 * 获取重试数据队列当前大小
	 */
	public int getRetryDataQueueSize() {
		initQueuesIfNeeded();
		return retryDataQueue.size();
	}
	
	/**
	 * 获取重试数据队列容量
	 */
	public int getRetryDataQueueCapacity() {
		return config.getRetryDataQueueSize();
	}
	
	/**
	 * 获取重试数据队列使用率
	 *
	 * @return 使用率（0.0 ~ 1.0）
	 */
	public double getRetryDataQueueUsageRate() {
		initQueuesIfNeeded();
		return (double) retryDataQueue.size() / config.getRetryDataQueueSize();
	}
	
	/**
	 * 重试数据队列是否接近满载
	 *
	 * @return true-接近满载（超过阈值）
	 */
	public boolean isRetryDataQueueNearFull() {
		return getRetryDataQueueUsageRate() > config.getQueueFullThreshold();
	}
	
	/**
	 * 添加指令响应到重试队列
	 *
	 * @param command 指令响应
	 */
	public void offerToRetryCommand(BaseCommandResponseDTO command) {
		initQueuesIfNeeded();
		boolean success = retryCommandQueue.offer(command);
		if (!success) {
			log.error("指令重试队列已满，数据将丢失: deviceId={}, taskId={}, queueSize={}/{}",
					command.getDeviceId(), command.getTaskId(),
					retryCommandQueue.size(), config.getRetryCommandQueueSize());
			
			// TODO: 降级策略 - 重试队列满时，保存到Redis Stream持久化
			// saveCommandToRedisStream(command);
		}
	}
	
	/**
	 * 批量添加指令响应到重试队列
	 *
	 * @param commandList 指令响应列表
	 */
	public void offerBatchToRetryCommand(List<BaseCommandResponseDTO> commandList) {
		commandList.forEach(this::offerToRetryCommand);
	}
	
	/**
	 * 批量取出重试指令响应
	 *
	 * @param maxSize 最大取出数量
	 * @return 指令响应列表
	 */
	public List<BaseCommandResponseDTO> drainRetryCommandBatch(int maxSize) {
		initQueuesIfNeeded();
		List<BaseCommandResponseDTO> batch = new ArrayList<>(maxSize);
		retryCommandQueue.drainTo(batch, maxSize);
		return batch;
	}
	
	/**
	 * 获取重试指令队列当前大小
	 */
	public int getRetryCommandQueueSize() {
		initQueuesIfNeeded();
		return retryCommandQueue.size();
	}
	
	/**
	 * 获取重试指令队列容量
	 */
	public int getRetryCommandQueueCapacity() {
		return config.getRetryCommandQueueSize();
	}
	
	/**
	 * 获取重试指令队列使用率
	 *
	 * @return 使用率（0.0 ~ 1.0）
	 */
	public double getRetryCommandQueueUsageRate() {
		initQueuesIfNeeded();
		return (double) retryCommandQueue.size() / config.getRetryCommandQueueSize();
	}
	
	/**
	 * 重试指令队列是否接近满载
	 *
	 * @return true-接近满载（超过阈值）
	 */
	public boolean isRetryCommandQueueNearFull() {
		return getRetryCommandQueueUsageRate() > config.getQueueFullThreshold();
	}
	
	// ==================== 监控统计 ====================
	
	/**
	 * 打印队列状态（用于监控）
	 */
	public void logQueueStatus() {
		initQueuesIfNeeded();
		log.info("队列状态监控 - 数据队列:{}/{} ({:.1f}%), 指令队列:{}/{} ({:.1f}%), 重试数据:{}/{} ({:.1f}%), 重试指令:{}/{} ({:.1f}%)",
				dataQueue.size(), config.getDataQueueSize(), getDataQueueUsageRate() * 100,
				commandQueue.size(), config.getCommandQueueSize(), getCommandQueueUsageRate() * 100,
				retryDataQueue.size(), config.getRetryDataQueueSize(), getRetryDataQueueUsageRate() * 100,
				retryCommandQueue.size(), config.getRetryCommandQueueSize(), getRetryCommandQueueUsageRate() * 100);
	}
	
	/**
	 * 检查队列告警状态
	 *
	 * @return 告警信息列表，无告警返回空列表
	 */
	public List<String> checkAlerts() {
		initQueuesIfNeeded();
		List<String> alerts = new ArrayList<>();
		
		// 数据队列告警
		if (isDataQueueNearFull()) {
			alerts.add(String.format("⚠️ 数据队列使用率过高: %.1f%% (%d/%d)",
					getDataQueueUsageRate() * 100, dataQueue.size(), config.getDataQueueSize()));
		}
		
		// 指令队列告警
		if (isCommandQueueNearFull()) {
			alerts.add(String.format("⚠️ 指令队列使用率过高: %.1f%% (%d/%d)",
					getCommandQueueUsageRate() * 100, commandQueue.size(), config.getCommandQueueSize()));
		}
		
		// 重试数据队列告警
		if (isRetryDataQueueNearFull()) {
			alerts.add(String.format("🔴 重试数据队列积压严重: %.1f%% (%d/%d)",
					getRetryDataQueueUsageRate() * 100, retryDataQueue.size(), config.getRetryDataQueueSize()));
		}
		
		// 重试指令队列告警
		if (isRetryCommandQueueNearFull()) {
			alerts.add(String.format("🔴 重试指令队列积压严重: %.1f%% (%d/%d)",
					getRetryCommandQueueUsageRate() * 100, retryCommandQueue.size(), config.getRetryCommandQueueSize()));
		}
		
		return alerts;
	}
	
	/**
	 * 获取队列监控指标（用于Prometheus等监控系统）
	 */
	public QueueMetrics getMetrics() {
		initQueuesIfNeeded();
		return QueueMetrics.builder()
				.dataQueueSize(dataQueue.size())
				.dataQueueCapacity(config.getDataQueueSize())
				.dataQueueUsageRate(getDataQueueUsageRate())
				.commandQueueSize(commandQueue.size())
				.commandQueueCapacity(config.getCommandQueueSize())
				.commandQueueUsageRate(getCommandQueueUsageRate())
				.retryDataQueueSize(retryDataQueue.size())
				.retryDataQueueCapacity(config.getRetryDataQueueSize())
				.retryDataQueueUsageRate(getRetryDataQueueUsageRate())
				.retryCommandQueueSize(retryCommandQueue.size())
				.retryCommandQueueCapacity(config.getRetryCommandQueueSize())
				.retryCommandQueueUsageRate(getRetryCommandQueueUsageRate())
				.build();
	}
	
	/**
	 * 队列监控指标
	 */
	@Data
	@Builder
	public static class QueueMetrics {
		
		private int dataQueueSize;
		private int dataQueueCapacity;
		private double dataQueueUsageRate;
		
		private int commandQueueSize;
		private int commandQueueCapacity;
		private double commandQueueUsageRate;
		
		private int retryDataQueueSize;
		private int retryDataQueueCapacity;
		private double retryDataQueueUsageRate;
		
		private int retryCommandQueueSize;
		private int retryCommandQueueCapacity;
		private double retryCommandQueueUsageRate;
		
	}
	
}

