package org.pms.trigger.aep.commad;

import lombok.extern.slf4j.Slf4j;
import org.pms.domain.command.dto.BaseCommandRespDataDTO;
import org.pms.trigger.buffer.DataBuffer;
import org.pms.types.GatewayCode;
import org.pms.types.Response;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * AEP平台设备指令响应接口
 * 重构说明：将RocketMQ替换为本地队列+异步批量RPC
 *
 * @author: alcsyooterranf
 * @date 2025-01-23
 */
@Slf4j
@RestController
public class CommandRespDataController {
	
	private final DataBuffer dataBuffer;
	
	public CommandRespDataController(DataBuffer dataBuffer) {
		this.dataBuffer = dataBuffer;
	}
	
	@RequestMapping(value = "aep/command", method = RequestMethod.POST)
	public Response<String> deviceCommandResponse(@RequestBody @Valid BaseCommandRespDataDTO request) {
		log.info("aep/command收到消息: deviceId={}, taskId={}, status={}",
				request.getDeviceId(),
				request.getTaskId(),
				request.getResult());
		
		// 接口调用计时
		long start = System.currentTimeMillis();
		
		// 放入本地队列（快速返回，不等待后端处理）
		boolean success = dataBuffer.offerCommand(request);
		
		long end = System.currentTimeMillis();
		log.info("指令响应入队耗时: {}ms, 队列大小: {}",
				end - start, dataBuffer.getCommandQueueSize());
		
		if (!success) {
			log.error("指令响应队列已满，数据被拒绝: deviceId={}, taskId={}",
					request.getDeviceId(), request.getTaskId());
			return Response.<String>builder()
					.code(GatewayCode.LOCAL_QUEUE_IS_FULL.getCode())
					.message(GatewayCode.LOCAL_QUEUE_IS_FULL.getMessage())
					.build();
		}
		
		return Response.<String>builder()
				.code(GatewayCode.SUCCESS.getCode())
				.message(GatewayCode.SUCCESS.getMessage())
				.data("接受设备指令响应通知")
				.build();
	}
	
}
