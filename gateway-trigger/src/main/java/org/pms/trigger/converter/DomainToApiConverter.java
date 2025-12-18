package org.pms.trigger.converter;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.pms.api.dto.command.CommandResponseDTO;
import org.pms.api.dto.command.CommandState;
import org.pms.api.dto.devicedata.DeviceDataDTO;
import org.pms.api.dto.devicedata.MonitorParameterDTO;
import org.pms.domain.command.dto.BaseCommandResponseDTO;
import org.pms.domain.dataReport.dto.BaseDataChangeReportDTO;
import org.springframework.stereotype.Component;

/**
 * Domain层DTO转换为API层DTO
 * <p>
 * 职责：
 * 1. 将网关Domain层接收的AEP消息DTO转换为API层DTO
 * 2. API层DTO用于网关和后端之间的RPC通信
 * <p>
 * 设计思路：
 * - Domain层DTO：接收AEP消息，包含AEP特有字段（upPacketSN、upDataSN等）
 * - API层DTO：RPC通信，只包含业务核心字段
 * TODO: 目前仅支持非透传数据类型转化为{@link MonitorParameterDTO}, 若以后扩展出其他数据上报内容, 请更改!
 *
 * @author alcsyooterranf
 * @date 2025-01-23
 */
@Slf4j
@Component
public class DomainToApiConverter {
	
	/**
	 * 转换设备数据上报DTO
	 *
	 * @param domain Domain层DTO（AEP消息）
	 * @return API层DTO（RPC传输）
	 */
	public DeviceDataDTO convertDeviceData(BaseDataChangeReportDTO domain) {
		if (domain == null) {
			return null;
		}
		
		// 转换payload
		if (!domain.getPayload().isObject()) {
			log.error("类型转换错误, payload非对象类型: {}", domain.getPayload());
			return null;
		}
		JsonNode payload = domain.getPayload();
		MonitorParameterDTO payloadDto = MonitorParameterDTO.builder()
				.pressure(payload.get("pressure").asText())
				.temperature(payload.get("temperature").asText())
				.voltage(payload.get("voltage").asInt())
				.build();
		
		return DeviceDataDTO.builder()
				.deviceId(domain.getDeviceId())
				.pipelineId(domain.getProductId())
				.serviceId(domain.getServiceId())
				.timestamp(domain.getTimestamp())
				.payload(payloadDto)
				.tenantId(domain.getTenantId())
				.protocol(domain.getProtocol())
				.deviceType(domain.getDeviceType())
				.assocAssetId(domain.getAssocAssetId())
				.IMSI(domain.getIMSI())
				.IMEI(domain.getIMEI())
				.build();
	}
	
	/**
	 * 转换指令响应DTO
	 *
	 * @param domain Domain层DTO（AEP消息）
	 * @return API层DTO（RPC传输）
	 */
	public CommandResponseDTO convertCommandResponse(BaseCommandResponseDTO domain) {
		if (domain == null) {
			return null;
		}
		
		return CommandResponseDTO.builder()
				.deviceId(domain.getDeviceId())
				.pipelineId(domain.getProductId())
				.taskId(domain.getTaskId())
				.commandResult(
						domain.getResult() != null
								? domain.getResult().getResultDetail()
								: null)
				.commandState(
						domain.getResult() != null
								? CommandState.valueOf(domain.getResult().getResultCode())
								: CommandState.UNKNOW_ERROR)
				.timestamp(domain.getTimestamp())
				.tenantId(domain.getTenantId())
				.build();
	}
	
}

