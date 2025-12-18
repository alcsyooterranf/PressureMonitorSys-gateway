package org.pms.trigger.feign;

import org.pms.api.IAuthRpcService;
import org.pms.types.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author alcsyooterranf
 * @program PressureMonitorSys-ws
 * @description 鉴权服务Feign客户端
 * @create 2025/12/14
 */
@FeignClient(
		name = "auth-service",
		url = "${rpc.auth.url}",
		configuration = FeignConfig.class
)
public interface IAuthRpcClient extends IAuthRpcService {
	
	@Override
	@GetMapping("/rpc/auth/publicKey")
	Response<String> getPublicKey();
	
	@Override
	@PostMapping("/rpc/auth/checkPublicKey")
	Response<Boolean> checkPublicKey(@RequestBody String publicKey);
	
}
