package org.pms.domain.devicedata.dto;

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
public class BaseDeviceDataDTO {
	
	/**
	 * (可选)上行报文序号, Tlink协议特有字段
	 */
	@JsonProperty(value = "upPacketSN")
	private Integer upPacketSN;
	/**
	 * (可选)数据上报报文序号, Tlink协议特有字段
	 */
	@JsonProperty(value = "upDataSN")
	private Integer upDataSN;
	/**
	 * 数据上报主题
	 */
	@JsonProperty(value = "topic")
	private String topic;
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
	 * 服务标识
	 */
	@JsonProperty(value = "serviceId")
	private String serviceId;
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
	 * 消息负载 JsonNode
	 * 非透传消息格式为 payload, 消息内容为 JSON 格式;
	 * 透传消息格式为 binary, 消息内容为 Base64 编码后的二进制数据
	 */
	@JsonProperty(value = "payload")
	private JsonNode payload;
	/**
	 * 消息类型=dataReport
	 */
	@JsonProperty(value = "messageType")
	private String messageType;
	/**
	 * (可选)设备标识
	 */
	@JsonProperty(value = "deviceType")
	private String deviceType;
	/**
	 * 设备ID
	 */
	@JsonProperty(value = "deviceId")
	private String deviceId;
	/**
	 * (可选)合作伙伴ID
	 */
	@JsonProperty(value = "assocAssetId")
	private String assocAssetId;
	/**
	 * (可选)NB终端sim卡标识
	 */
	@JsonProperty(value = "IMSI")
	private String IMSI;
	/**
	 * (可选)NB终端设备识别号
	 */
	@JsonProperty(value = "IMEI")
	private String IMEI;
	
}
