package org.pms.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.pms.types.GatewayCode;
import org.pms.utils.HttpResponseUtil;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 捕获Spring Security Filter整个过滤器链中所有和认证和授权相关的异常, 由ExceptionTranslationFilter调用
 * 其他类型异常如: ServletException, IOException默认不处理
 * 但有时触发异常后, 导致uri重定向为/error, 进而被AuthenticationEntryPoint或AccessDeniedHandler意外捕获
 *
 * @author zeal
 * @version 1.0
 * @since 2024/6/19 上午10:00
 */
@Slf4j
@Component
public class MyAuthenticationEntryPoint implements AuthenticationEntryPoint {
	
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
						 AuthenticationException authException) throws IOException {
		HttpResponseUtil.assembleResponse(response, GatewayCode.AUTHENTICATION_FAILURE);
	}
	
}
