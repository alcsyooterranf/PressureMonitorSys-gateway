package org.pms.config;

import org.pms.filter.JwtAuthenticationFilter;
import org.pms.filter.OptionFilter;
import org.pms.filter.RequestForwardEncapsulationFilter;
import org.pms.handler.MyAccessAuthorizationManager;
import org.pms.handler.MyAccessDeniedHandler;
import org.pms.handler.MyAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

/**
 * Gateway Security配置
 * <p>
 * Gateway只负责JWT验签和用户上下文透传，不负责登录/登出
 * 登录/登出功能由auth-service提供
 *
 * @author alcsyooterranf
 * @version 2.0
 * @since 2024/5/15 下午4:29
 */
@Configuration
@EnableWebSecurity  // 启用web安全
@EnableMethodSecurity // 启用方法安全
public class MySecurityConfig {
	
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final OptionFilter optionFilter;
	private final MyAccessDeniedHandler myAccessDeniedHandler;
	private final MyAuthenticationEntryPoint myAuthenticationEntryPoint;
	private final MyAccessAuthorizationManager myAccessAuthorizationManager;
	private final RequestForwardEncapsulationFilter requestForwardEncapsulationFilter;
	
	public MySecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter,
			OptionFilter optionFilter, MyAccessDeniedHandler myAccessDeniedHandler,
			MyAuthenticationEntryPoint myAuthenticationEntryPoint,
			MyAccessAuthorizationManager myAccessAuthorizationManager,
			RequestForwardEncapsulationFilter requestForwardEncapsulationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.optionFilter = optionFilter;
		this.myAccessDeniedHandler = myAccessDeniedHandler;
		this.myAuthenticationEntryPoint = myAuthenticationEntryPoint;
		this.myAccessAuthorizationManager = myAccessAuthorizationManager;
		this.requestForwardEncapsulationFilter = requestForwardEncapsulationFilter;
	}
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(
						auth -> auth
								// 放行auth-service的所有接口（登录、刷新token等）
								.requestMatchers("/auth/**").permitAll()
								// 放行AEP平台有关接口
								.requestMatchers("/aep/**").permitAll()
								// 自定义权限逻辑
								.anyRequest().access(myAccessAuthorizationManager)
				)
				// 跨域配置
				.cors(
						cors -> cors
								.configurationSource(request -> {
									var corsConfig = new CorsConfiguration();
									//corsConfig.setAllowedOrigins(List.of("http://localhost:8090")); // 支持跨域
									corsConfig.setAllowedOriginPatterns(List.of("*"));
									corsConfig.setAllowCredentials(true); // cookie
									corsConfig.setAllowedMethods(List.of("GET", "POST")); // 支持请求方式
									corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type")); // 允许请求头信息
									return corsConfig;
								})
				)
				// 异常处理器
				.exceptionHandling(
						handler -> handler
								//配置认证失败处理器
								.authenticationEntryPoint(myAuthenticationEntryPoint)
								//配置授权失败处理器
								.accessDeniedHandler(myAccessDeniedHandler)
				)
				// 基于token，禁用session
				.sessionManagement(
						AbstractHttpConfigurer::disable
				)
				// 禁用csrf
				.csrf(
						AbstractHttpConfigurer::disable
				)
				// 禁用httpBasic
				.httpBasic(
						AbstractHttpConfigurer::disable
				)
				// 禁用formLogin（登录由auth-service处理）
				.formLogin(
						AbstractHttpConfigurer::disable
				)
				// 禁用logout（登出由auth-service处理）
				.logout(
						AbstractHttpConfigurer::disable
				);
		http.addFilterBefore(optionFilter, UsernamePasswordAuthenticationFilter.class);
		// 将自定义的JwtAuthenticationFilter放到LogoutFilter之前
		http.addFilterBefore(jwtAuthenticationFilter, LogoutFilter.class);
		http.addFilterAfter(requestForwardEncapsulationFilter, AuthorizationFilter.class);
		return http.build();
	}
	
	/**
	 * 配置角色继承, ADMIN 包含 USER 的权限
	 *
	 * @return RoleHierarchy
	 */
	@Bean
	public RoleHierarchy roleHierarchy() {
		//ADMIN 包含 USER 的权限
		return RoleHierarchyImpl.fromHierarchy("ADMIN > USER");
	}
	
}
