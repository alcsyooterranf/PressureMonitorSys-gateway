package org.pms.trigger.aep.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.pms.types.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.pms.api.common.HttpResponse;
import org.pms.domain.dataReport.dto.BaseDataChangeReportDTO;
import org.pms.trigger.buffer.DeviceDataBuffer;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

/**
 * AEP平台设备数据上报接口
 * 重构说明：将RocketMQ替换为本地队列+异步批量RPC
 *
 * @author alcsyooterranf
 * @date 2025-01-23
 */
@Slf4j
@RestController
public class DataChangeController {
	
	private final static List<String> LEGAL_PROTOCOL = List.of("mqtt");
	private final DeviceDataBuffer deviceDataBuffer;
	
	public DataChangeController(DeviceDataBuffer deviceDataBuffer) {
		this.deviceDataBuffer = deviceDataBuffer;
	}
	
	@RequestMapping(value = "aep/data_change", method = RequestMethod.POST)
	public HttpResponse<String> deviceDataChange(@RequestBody @Valid BaseDataChangeReportDTO request) {
		String protocol = request.getProtocol();
		// 1. 检查协议类型
		if (!LEGAL_PROTOCOL.contains(protocol)) {
			log.error("不支持的协议类型: {}, 目前支持的协议有: {}", protocol, LEGAL_PROTOCOL);
			return HttpResponse.<String>builder()
					.code(ResponseCode.PROTOCOL_NOT_SUPPORTED.getCode())
					.message(ResponseCode.PROTOCOL_NOT_SUPPORTED.getMessage())
					.build();
		}
		
		// 2. 检查透传模式
		JsonNode payload = request.getPayload();
		if (Objects.isNull(payload)) {
			log.error("上报数据内容为空: {}", request);
			return HttpResponse.<String>builder()
					.code(ResponseCode.DATA_REPORT_PAYLOAD_EMPTY.getCode())
					.message(ResponseCode.DATA_REPORT_PAYLOAD_EMPTY.getMessage())
					.build();
		}
		// 透传模式(payload为Base64编码的二进制数据)
		// TODO: 暂不支持, 以后可拓展
		if (payload.isTextual()) {
			log.error("暂不支持透传模式");
			return HttpResponse.<String>builder()
					.code(ResponseCode.DATA_REPORT_PARSE_ERROR.getCode())
					.message(ResponseCode.DATA_REPORT_PARSE_ERROR.getMessage())
					.build();
		}
		log.info("aep/data_change收到消息: deviceId={}, serviceId={}",
				request.getDeviceId(), request.getServiceId());
		
		// 接口调用计时
		long start = System.currentTimeMillis();
		
		// 放入本地队列(快速返回，不等待后端处理)
		boolean success = deviceDataBuffer.offerData(request);
		
		long end = System.currentTimeMillis();
		log.info("数据入队耗时: {}ms, 队列大小: {}", end - start, deviceDataBuffer.getDataQueueSize());
		
		if (!success) {
			log.error("设备数据队列已满，数据被拒绝: deviceId={}", request.getDeviceId());
			return HttpResponse.<String>builder()
					.code(ResponseCode.LOCAL_QUEUE_IS_FULL.getCode())
					.message(ResponseCode.LOCAL_QUEUE_IS_FULL.getMessage())
					.build();
		}
		
		return HttpResponse.<String>builder()
				.code(ResponseCode.SUCCESS.getCode())
				.message(ResponseCode.SUCCESS.getMessage())
				.data("接受设备数据(" + request.getServiceId() + ")上报")
				.build();
	}
	
}
