package org.pms.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.pms.types.GatewayCode;
import org.pms.types.Response;

import java.io.IOException;

/**
 * @author alcsyooterranf
 * @program PressureMonitorSys-gateway
 * @description HttpResponse包装工具类
 * @create 2025/11/29
 */
public class HttpResponseUtil {
	
	public static void assembleResponse(HttpServletResponse response, GatewayCode responseCode) throws IOException {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
		Response bean = Response.builder()
				.code(responseCode.getCode())
				.message(responseCode.getMessage())
				.build();
		response.getWriter().write(new ObjectMapper().writeValueAsString(bean));
		response.getWriter().flush();
		response.getWriter().close();
	}
	
}
