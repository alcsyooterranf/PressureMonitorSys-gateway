package org.pms.handler;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * @author alcsyooterranf
 */
@Component
public class MyAccessAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
	
	@Override
	public void verify(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
		AuthorizationManager.super.verify(authentication, object);
	}
	
	@Override
	public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
		String uri = object.getRequest().getRequestURI();
		// 放行注册接口, 允许所有角色访问
		if (uri.startsWith("/rbac/user/register")) {
			return new AuthorizationDecision(true);
		}
		// 放行电信AEP平台的相关接口请求, 将鉴权逻任务交给CA证书
		else if (uri.startsWith("/aep")) {
			return new AuthorizationDecision(true);
		}
		// 配置WebSocket告警推送权限, 允许admin和operator访问（已通过JWT鉴权）
		else if (uri.startsWith("/ws/") && authentication.get().getAuthorities().stream().anyMatch(
				authority -> "ROLE_admin".equals(authority.getAuthority()) ||
						"ROLE_operator".equals(authority.getAuthority()))) {
			return new AuthorizationDecision(true);
		}
		// 配置设备管理模块权限, 允许admin和operator访问
		else if (uri.startsWith("/device_manage") && authentication.get().getAuthorities().stream().anyMatch(
				authority -> "ROLE_admin".equals(authority.getAuthority()) ||
						"ROLE_operator".equals(authority.getAuthority()))) {
			return new AuthorizationDecision(true);
		}
		// 配置管道管理模块权限, 允许admin和operator访问
		else if (uri.startsWith("/pipeline_manage") && authentication.get().getAuthorities().stream().anyMatch(
				authority -> "ROLE_admin".equals(authority.getAuthority()) ||
						"ROLE_operator".equals(authority.getAuthority()))) {
			return new AuthorizationDecision(true);
		}
		// 配置设备数据上报模块权限, 允许admin和operator访问
		else if (uri.startsWith("/device_data") && authentication.get().getAuthorities().stream().anyMatch(
				authority -> "ROLE_admin".equals(authority.getAuthority()) ||
						"ROLE_operator".equals(authority.getAuthority()))) {
			return new AuthorizationDecision(true);
		}
		// 配置指令模块权限, 只允许admin访问
		else if (uri.startsWith("/command") && authentication.get().getAuthorities().stream().anyMatch(
				authority -> "ROLE_admin".equals(authority.getAuthority()))) {
			return new AuthorizationDecision(true);
		}
		// 配置用户管理模块权限
		else if (uri.startsWith("/rbac") && authentication.get().getAuthorities().stream().anyMatch(
				authority -> "ROLE_admin".equals(authority.getAuthority()))) {
			return new AuthorizationDecision(true);
		}
		// 拦截其他所有请求
		else {
			return new AuthorizationDecision(false);
		}
	}
	
}
