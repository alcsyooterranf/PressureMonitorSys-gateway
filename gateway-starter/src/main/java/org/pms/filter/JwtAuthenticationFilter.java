package org.pms.filter;

import com.pms.auth.utils.JwtUtil;
import com.pms.types.AppException;
import com.pms.types.Constants;
import com.pms.types.ResponseCode;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.pms.domain.auth.dto.LoginUser;
import org.pms.domain.auth.service.JwtService;
import org.pms.utils.HttpResponseUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * WHAT THE ZZZZEAL
 *
 * @author zeal
 * @version 1.0
 * @since 2024/5/15 下午5:48
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
	// /aep/**为AEP平台接口
	private static final Set<String> EXCLUDED_PREFIX_PATHS = Set.of("/aep/", "/user/register");
	private static final String TOKEN_HEADER = Constants.TOKEN_HEADER;
	private static final String TOKEN_PREFIX = Constants.TOKEN_PREFIX;
	// TODO: WebSocket握手时从query参数获取token
	private static final String TOKEN_QUERY_PARAM = "token";
	
	@Resource
	private JwtService jwtService;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response,
									@NotNull FilterChain filterChain) throws ServletException, IOException {
		log.info("JWT url {}", request.getRequestURL());
		
		// 0. 放行接口, 并在request中设置放行标志
		String uri = request.getRequestURI();
		if (EXCLUDED_PREFIX_PATHS.stream().anyMatch(uri::startsWith)) {
			log.debug("JWT Filter: release");
			filterChain.doFilter(request, response);
			return;
		}
		
		// 1. 获取token（支持HTTP Header和WebSocket Query参数两种方式）
		String token = extractToken(request);
		
		// 2. 验证token是否存在
		if (StringUtils.isBlank(token)) {
			HttpResponseUtil.assembleResponse(response, ResponseCode.AUTHORIZATION_HEADER_EMPTY);
			return;
		}
		
		// 3. 验证token基本信息（Gateway只验证签名和过期时间，不验证refreshToken存在性）
		try {
			JwtUtil.validateToken(token);
		} catch (AppException e) {
			HttpResponseUtil.assembleResponse(response, ResponseCode.TOKEN_VALIDATE_ERROR);
			return;
		}
		log.info("JWT Filter: token验证通过");
		
		// 4. 从token中获取用户信息
		LoginUser loginUser = jwtService.getLoginUserFromToken(token);
		
		// 5. 将token存入SecurityContextHolder
		if (null == SecurityContextHolder.getContext().getAuthentication()) {
			// 更新security登录用户对象
			UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
					new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
			// 将用户信息存入security上下文
			usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			log.info("JWT Filter: {}", loginUser);
		}
		filterChain.doFilter(request, response);
	}
	
	/**
	 * 提取token（支持HTTP Header和WebSocket Query参数两种方式）
	 *
	 * @param request HTTP请求
	 * @return token字符串，如果不存在则返回null
	 */
	private String extractToken(HttpServletRequest request) {
		// 1. 优先从HTTP Header中获取token（标准HTTP请求）
		String header = request.getHeader(TOKEN_HEADER);
		if (StringUtils.isNotBlank(header) && header.startsWith(TOKEN_PREFIX)) {
			// 提取Bearer后面的token
			return StringUtils.substring(header, TOKEN_PREFIX.length() + 1);
		}
		
		// 2. 从Query参数中获取token（WebSocket握手请求）
		String queryToken = request.getParameter(TOKEN_QUERY_PARAM);
		if (StringUtils.isNotBlank(queryToken)) {
			log.info("从Query参数中获取token (WebSocket握手)");
			return queryToken;
		}
		
		return null;
	}
	
}
