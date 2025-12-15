package org.pms.domain.dataReport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author alcsyooterranf
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InfoDTO {
	
	private final String serviceId = "info_report";
	@JsonProperty(value = "terminal_type")
	private String terminalType;
	@JsonProperty(value = "software_version")
	private String softwareVersion;
	@JsonProperty(value = "module_type")
	private String moduleType;
	@JsonProperty(value = "manufacturer_name")
	private String manufacturerName;
	@JsonProperty(value = "hardware_version")
	private String hardwareVersion;
	@JsonProperty(value = "IMEI")
	private String IMEI;
	@JsonProperty(value = "ICCID")
	private String ICCID;
	
}
