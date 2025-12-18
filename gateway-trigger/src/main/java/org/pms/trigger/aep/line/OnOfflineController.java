package org.pms.trigger.aep.line;

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
public class OnOfflineController {
	
	@RequestMapping(value = "aep/onoffline", method = RequestMethod.POST)
	public Response<String> deviceOnlineOffline(@RequestBody String json) {
		log.info("DeviceOnlineOffline: {}", json);
		return Response.<String>builder()
				.code(GatewayCode.SUCCESS.getCode())
				.message(GatewayCode.SUCCESS.getMessage())
				.data("接受设备上下线通知")
				.build();
	}
	
}
