package org.pms.trigger.feign;

import org.pms.api.dto.command.CommandRespDTO;
import org.pms.api.facade.ICommandFacade;
import org.pms.types.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author alcsyooterranf
 * @description 后端指令服务Feign客户端, 用于网关调用后端服务的指令响应处理接口
 */
@FeignClient(
		name = "business-command-service",
		url = "${rpc.business.url}",
		path = "/api/command"
)
public interface ICommandClient extends ICommandFacade {
	
	/**
	 * 单条保存指令响应
	 *
	 * @param request 指令响应DTO
	 * @return 响应结果
	 */
	@PostMapping("/api/device/command/save")
	Response<Boolean> handleCommandResp(@RequestBody CommandRespDTO request);
	
	/**
	 * 批量保存指令响应
	 *
	 * @param responseList 指令响应列表
	 * @return 响应结果
	 */
	@PostMapping("/api/device/command/batch-save")
	Response<Boolean> batchHandleCommandResp(@RequestBody List<CommandRespDTO> responseList);
	
}

