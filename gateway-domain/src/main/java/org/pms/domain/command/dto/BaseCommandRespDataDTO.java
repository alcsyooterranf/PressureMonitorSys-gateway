package org.pms.domain.command.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author alcsyooterranf
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseCommandRespDataDTO {
	
	/**
	 * 时间戳
	 */
	@JsonProperty(value = "timestamp")
	private Long timestamp;
	/**
	 * 租户ID
	 */
	@JsonProperty(value = "tenantId")
	private String tenantId;
	/**
	 * 指令任务ID
	 */
	@JsonProperty(value = "taskId")
	private Long taskId;
	/**
	 * 指令执行结果
	 */
	@JsonProperty(value = "result")
	private CommandResultDTO result;
	/**
	 * 协议类型
	 */
	@JsonProperty(value = "protocol")
	private String protocol;
	/**
	 * 产品ID
	 */
	@JsonProperty(value = "productId")
	private String productId;
	/**
	 * 消息类型=commandResponse
	 */
	@JsonProperty(value = "messageType")
	private String messageType;
	/**
	 * 设备ID
	 */
	@JsonProperty(value = "deviceId")
	private String deviceId;
	
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class CommandResultDTO {
		
		/**
		 * 指令执行结果, 数据类型为json, 字段与下发的指令保持一致
		 */
		@JsonProperty(value = "resultDetail")
		private JsonNode resultDetail;
		/**
		 * 指令执行结果状态
		 */
		@JsonProperty(value = "resultCode")
		private String resultCode;
		
	}
	
}
