package org.pms.trigger.aep.line;

import com.pms.types.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.pms.api.common.HttpResponse;
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
	public HttpResponse<String> deviceOnlineOffline(@RequestBody String json) {
		log.info("DeviceOnlineOffline: {}", json);
		return HttpResponse.<String>builder()
				.code(ResponseCode.SUCCESS.getCode())
				.message(ResponseCode.SUCCESS.getMessage())
				.data("接受设备上下线通知")
				.build();
	}
	
}
