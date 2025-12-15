package org.pms.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.types.ResponseCode;
import jakarta.servlet.http.HttpServletResponse;
import org.pms.api.common.HttpResponse;

import java.io.IOException;

/**
 * @program PressureMonitorSys-gateway
 * @description HttpResponse包装工具类
 * @author alcsyooterranf
 * @create 2025/11/29
 */
public class HttpResponseUtil {
	
	public static void assembleResponse(HttpServletResponse response, ResponseCode responseCode) throws IOException {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
		HttpResponse bean = HttpResponse.builder()
				.code(responseCode.getCode())
				.message(responseCode.getMessage())
				.build();
		response.getWriter().write(new ObjectMapper().writeValueAsString(bean));
		response.getWriter().flush();
		response.getWriter().close();
	}
	
}
