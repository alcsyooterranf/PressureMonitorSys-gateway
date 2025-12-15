package org.pms.trigger.aep.commad;

import com.pms.types.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.pms.api.common.HttpResponse;
import org.pms.trigger.buffer.DeviceDataBuffer;
import org.pms.domain.command.dto.BaseCommandResponseDTO;
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
public class CommandResponseController {

    private final DeviceDataBuffer deviceDataBuffer;

    public CommandResponseController(DeviceDataBuffer deviceDataBuffer) {
        this.deviceDataBuffer = deviceDataBuffer;
    }

    @RequestMapping(value = "aep/command", method = RequestMethod.POST)
    public HttpResponse<String> deviceCommandResponse(@RequestBody @Valid BaseCommandResponseDTO request) {
        log.info("aep/command收到消息: deviceId={}, taskId={}, status={}",
                request.getDeviceId(),
                request.getTaskId(),
                request.getResult());

        // 接口调用计时
        long start = System.currentTimeMillis();

        // 放入本地队列（快速返回，不等待后端处理）
        boolean success = deviceDataBuffer.offerCommand(request);

        long end = System.currentTimeMillis();
        log.info("指令响应入队耗时: {}ms, 队列大小: {}",
                end - start, deviceDataBuffer.getCommandQueueSize());

        if (!success) {
            log.error("指令响应队列已满，数据被拒绝: deviceId={}, taskId={}",
                    request.getDeviceId(), request.getTaskId());
            return HttpResponse.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .message("系统繁忙，请稍后重试")
                    .data("队列已满")
                    .build();
        }

        return HttpResponse.<String>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .message(ResponseCode.SUCCESS.getMessage())
                .data("接受设备指令响应通知")
                .build();
    }

}
