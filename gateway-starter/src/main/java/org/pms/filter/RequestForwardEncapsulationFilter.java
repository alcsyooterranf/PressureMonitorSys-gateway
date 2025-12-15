package org.pms.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.types.Constants;
import com.pms.types.ResponseCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.pms.domain.auth.dto.LoginUser;
import org.pms.filter.req.CustomHttpServletRequest;
import org.pms.filter.req.SecurityContextHeader;
import org.pms.utils.HttpResponseUtil;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

/**
 * 请求转发封装Filter
 * 在JWT验签通过后，将用户信息写入Header，透传给后端服务
 *
 * @author alcsyooterranf
 * @version 1.0
 * @since 2024/5/15
 */
@Slf4j
@Component
public class RequestForwardEncapsulationFilter extends OncePerRequestFilter {
	
	private static final String SECURITY_CONTEXT_HEADER = Constants.SECURITY_CONTEXT_HEADER;
	// 用户上下文透传Header（新增）
	private static final String USER_ID_HEADER = "X-User-Id";
	private static final String USER_NAME_HEADER = "X-User-Name";
	private static final String USER_ROLES_HEADER = "X-User-Roles";
	
	@Override
	protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
									@NotNull FilterChain filterChain) throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		// 判断是否是匿名用户(register)
		if (authentication instanceof AnonymousAuthenticationToken) {
			try {
				filterChain.doFilter(request, response);
			} catch (ServletException e) {
				HttpResponseUtil.assembleResponse(response, ResponseCode.REQUEST_FORWARD_IO_EXCEPTION);
			}
		} else {
			LoginUser loginUser = (LoginUser) authentication.getPrincipal();
			
			SecurityContextHeader securityContextHeader = SecurityContextHeader.builder()
					.id(loginUser.getUserAggregate().getId())
					.username(loginUser.getUserAggregate().getUsername())
					.authorities(loginUser.getAuthorities().stream().map(Object::toString).toList())
					.build();
			
			// 包装请求
			CustomHttpServletRequest customRequest = new CustomHttpServletRequest(request);
			String encodedSecurityContext = null;
			try {
				encodedSecurityContext = toJsonBase64(securityContextHeader);
			} catch (IOException e) {
				HttpResponseUtil.assembleResponse(response, ResponseCode.REQUEST_BASE64_DECODE_ERROR);
			}
			
			// 添加到请求头（保留原有的X-Security-Context，用于兼容）
			customRequest.addHeader(SECURITY_CONTEXT_HEADER, encodedSecurityContext);
			
			// 添加用户上下文透传Header（新增）
			customRequest.addHeader(USER_ID_HEADER, String.valueOf(loginUser.getUserAggregate().getId()));
			customRequest.addHeader(USER_NAME_HEADER, loginUser.getUserAggregate().getUsername());
			// 将角色列表转换为逗号分隔的字符串
			String roles = loginUser.getAuthorities().stream()
					.map(Object::toString)
					.reduce((a, b) -> a + "," + b)
					.orElse("");
			customRequest.addHeader(USER_ROLES_HEADER, roles);
			
			log.debug("User context headers added: userId={}, username={}, roles={}",
					loginUser.getUserAggregate().getId(),
					loginUser.getUserAggregate().getUsername(),
					roles);
			
			try {
				filterChain.doFilter(customRequest, response);
			} catch (ServletException e) {
				HttpResponseUtil.assembleResponse(response, ResponseCode.REQUEST_FORWARD_IO_EXCEPTION);
			}
		}
	}
	
	private String toJsonBase64(Object object) throws IOException {
		// 1. JSON 序列化
		ObjectMapper objectMapper = new ObjectMapper();
		String jsonString = objectMapper.writeValueAsString(object);
		
		// 2. Base64 编码
		return Base64.getEncoder().encodeToString(jsonString.getBytes());
	}
	
}
