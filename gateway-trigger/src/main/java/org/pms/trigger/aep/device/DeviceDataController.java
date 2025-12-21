package org.pms.trigger.aep.device;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.pms.domain.devicedata.dto.BaseDeviceDataDTO;
import org.pms.trigger.buffer.DataBuffer;
import org.pms.types.GatewayCode;
import org.pms.types.Response;
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
public class DeviceDataController {
	
	private final static List<String> LEGAL_PROTOCOL = List.of("mqtt");
	private final DataBuffer dataBuffer;
	
	public DeviceDataController(DataBuffer dataBuffer) {
		this.dataBuffer = dataBuffer;
	}
	
	@RequestMapping(value = "aep/data_change", method = RequestMethod.POST)
	public Response<String> deviceDataChange(@RequestBody @Valid BaseDeviceDataDTO request) {
		String protocol = request.getProtocol();
		// 1. 检查协议类型
		if (!LEGAL_PROTOCOL.contains(protocol)) {
			log.error("不支持的协议类型: {}, 目前支持的协议有: {}", protocol, LEGAL_PROTOCOL);
			return Response.<String>builder()
					.code(GatewayCode.PROTOCOL_NOT_SUPPORTED.getCode())
					.message(GatewayCode.PROTOCOL_NOT_SUPPORTED.getMessage())
					.build();
		}
		
		// 2. 检查透传模式
		JsonNode payload = request.getPayload();
		if (Objects.isNull(payload)) {
			log.error("上报数据内容为空: {}", request);
			return Response.<String>builder()
					.code(GatewayCode.DATA_REPORT_PAYLOAD_EMPTY.getCode())
					.message(GatewayCode.DATA_REPORT_PAYLOAD_EMPTY.getMessage())
					.build();
		}
		// 透传模式(payload为Base64编码的二进制数据)
		// TODO: 暂不支持, 以后可拓展
		if (payload.isTextual()) {
			log.error("暂不支持透传模式");
			return Response.<String>builder()
					.code(GatewayCode.DATA_REPORT_PARSE_ERROR.getCode())
					.message(GatewayCode.DATA_REPORT_PARSE_ERROR.getMessage())
					.build();
		}
		log.info("aep/data_change收到消息: deviceId={}, serviceId={}",
				request.getDeviceId(), request.getServiceId());
		
		// 接口调用计时
		long start = System.currentTimeMillis();
		
		// 放入本地队列(快速返回，不等待后端处理)
		boolean success = dataBuffer.offerData(request);
		
		long end = System.currentTimeMillis();
		log.info("数据入队耗时: {}ms, 队列大小: {}", end - start, dataBuffer.getDataQueueSize());
		
		if (!success) {
			log.error("设备数据队列已满，数据被拒绝: deviceId={}", request.getDeviceId());
			return Response.<String>builder()
					.code(GatewayCode.LOCAL_QUEUE_IS_FULL.getCode())
					.message(GatewayCode.LOCAL_QUEUE_IS_FULL.getMessage())
					.build();
		}
		
		return Response.<String>builder()
				.code(GatewayCode.SUCCESS.getCode())
				.message(GatewayCode.SUCCESS.getMessage())
				.data("接受设备数据(" + request.getServiceId() + ")上报")
				.build();
	}
	
}
