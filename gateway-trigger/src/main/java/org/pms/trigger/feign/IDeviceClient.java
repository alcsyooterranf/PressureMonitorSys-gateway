package org.pms.trigger.feign;

import org.pms.api.dto.devicedata.DeviceDataDTO;
import org.pms.api.facade.IDeviceDataFacade;
import org.pms.types.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 后端服务Feign客户端
 * <p>
 * 用途：网关调用后端服务的RPC接口
 * <p>
 * 设计思路：
 * 1. 继承 IDeviceDataFacade 接口（共享API接口）
 * 2. 使用 @PostMapping 指定具体的URL路径
 * 3. 网关通过此客户端将API层DTO发送给后端
 *
 * @author alcsyooterranf
 * @date 2025-01-23
 */
@FeignClient(
		name = "business-device-data-service",
		url = "${rpc.business.url}",
		configuration = FeignConfig.class
)
public interface IDeviceClient extends IDeviceDataFacade {
	
	/**
	 * 批量保存设备数据
	 *
	 * @param dataList 设备数据列表（API层DTO）
	 * @return 响应结果
	 */
	@Override
	@PostMapping("/api/device/data/batch-save")
	Response<Boolean> batchHandleDeviceData(@RequestBody List<DeviceDataDTO> dataList);
	
	/**
	 * 单条保存设备数据（用于重试）
	 *
	 * @param data 设备数据（API层DTO）
	 * @return 响应结果
	 */
	@Override
	@PostMapping("/api/device/data/save")
	Response<Boolean> handleDeviceData(@RequestBody DeviceDataDTO data);
	
}

