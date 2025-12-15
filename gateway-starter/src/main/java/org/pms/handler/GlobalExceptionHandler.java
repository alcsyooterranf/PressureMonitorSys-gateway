package org.pms.handler;

import com.pms.types.AppException;
import com.pms.types.ResponseCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.pms.api.common.HttpResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * WHAT THE ZZZZEAL
 *
 * @author zeal
 * @version 1.0
 * @since 2024/5/28 下午4:04
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	
	/**
	 * 处理业务异常
	 *
	 * @param request 请求
	 * @param e       异常
	 * @return 异常信息
	 */
	@ExceptionHandler(value = AppException.class)
	public HttpResponse<String> bizExceptionHandler(HttpServletRequest request, AppException e) {
		log.error("异常代码: {}, 异常信息: {}", e.getCode(), e.getMessage());
		return HttpResponse.<String>builder()
				.code(e.getCode())
				.message(e.getMessage())
				.build();
	}
	
	/**
	 * 处理SQL异常
	 *
	 * @param request 请求
	 * @param e       异常
	 * @return 异常信息
	 */
	@ExceptionHandler(value = DataAccessException.class)
	public HttpResponse<String> sqlExceptionHandler(HttpServletRequest request, DataAccessException e) {
		AppException appException = new AppException(ResponseCode.SQL_INDEX_DUPLICATE, e);
		log.error("异常代码: {}, 异常信息: {}", appException.getCode(), appException.getMessage());
		return HttpResponse.<String>builder()
				.code(appException.getCode())
				.message(appException.getMessage())
				.build();
	}
	
}
