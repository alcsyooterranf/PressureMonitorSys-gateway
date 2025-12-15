package org.pms.trigger.feign;

import org.pms.api.common.HttpResponse;
import org.pms.domain.command.dto.BaseCommandResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author alcsyooterranf
 * @description 后端指令服务Feign客户端, 用于网关调用后端服务的指令响应处理接口
 */
@FeignClient(
		name = "backend-command-service",
		url = "${backend.service.url}",
		path = "/api/command"
)
public interface ICommandClient {
	
	/**
	 * 单条保存指令响应
	 *
	 * @param request 指令响应DTO
	 * @return 响应结果
	 */
	@PostMapping("/response/save")
	HttpResponse<Void> saveCommandResponse(@RequestBody BaseCommandResponseDTO request);
	
	/**
	 * 批量保存指令响应
	 *
	 * @param responseList 指令响应列表
	 * @return 响应结果
	 */
	@PostMapping("/response/batch-save")
	HttpResponse<Void> batchSaveCommandResponse(@RequestBody List<BaseCommandResponseDTO> responseList);
	
}

