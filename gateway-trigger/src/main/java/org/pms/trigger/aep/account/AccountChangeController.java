package org.pms.trigger.aep.account;

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
public class AccountChangeController {
	
	@RequestMapping(value = "aep/account", method = RequestMethod.POST)
	public HttpResponse<String> deviceAccountChangeResponse(@RequestBody String json) {
		log.info("DeviceAccountChangeResponse: {}", json);
		return HttpResponse.<String>builder()
				.code(ResponseCode.SUCCESS.getCode())
				.message(ResponseCode.SUCCESS.getMessage())
				.data("接受设备数量变化通知")
				.build();
	}
	
}
