package org.pms.handler;

import com.pms.types.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.pms.utils.HttpResponseUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 处理授权失败异常
 *
 * @author zeal
 * @version 1.0
 * @since 2024/6/19 上午10:03
 */
@Slf4j
@Component
public class MyAccessDeniedHandler implements AccessDeniedHandler {
	
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
					   AccessDeniedException accessDeniedException) throws IOException {
		log.error("权限不足 {}", request.getRequestURI());
		HttpResponseUtil.assembleResponse(response, ResponseCode.AUTHORIZATION_FAILURE);
	}
	
}
