package org.pms.trigger.aep.account;

import lombok.extern.slf4j.Slf4j;
import org.pms.types.GatewayCode;
import org.pms.types.Response;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author alcsyooterranf
 */
@Slf4j
@RestController
public class AccountChangeController {
	
	@RequestMapping(value = "aep/account", method = RequestMethod.POST)
	public Response<String> deviceAccountChangeResponse(@RequestBody String json) {
		log.info("DeviceAccountChangeResponse: {}", json);
		return Response.<String>builder()
				.code(GatewayCode.SUCCESS.getCode())
				.message(GatewayCode.SUCCESS.getMessage())
				.data("接受设备数量变化通知")
				.build();
	}
	
}
