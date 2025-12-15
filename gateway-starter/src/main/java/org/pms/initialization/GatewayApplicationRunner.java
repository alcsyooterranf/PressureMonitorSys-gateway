package org.pms.initialization;

import com.pms.auth.utils.JwtUtil;
import com.pms.auth.utils.RSAUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.pms.api.common.ResponseCode;
import org.pms.api.common.RpcResponse;
import org.pms.trigger.feign.IAuthRpcClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.PublicKey;
import java.util.Objects;

/**
 * @author alcsyooterranf
 * @program PressureMonitorSys-ws
 * @description 项目启动后依赖编排
 * @create 2025/12/14
 */
@Slf4j
@Component
public class GatewayApplicationRunner implements InitializingBean {
	
	@Resource
	private IAuthRpcClient authRpcClient;
	
	@Value("${pms.auth.public-key-path}")
	private String publicKeyPath;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("开始初始化公钥配置...");
		
		// 1. 检查本地是否有公钥
		if (!hasLocalPublicKey()) {
			log.info("本地未找到公钥，从认证服务获取公钥");
			// 如果没有，则获取公钥并保存
			String publicKey = fetchPublicKeyFromAuthService();
			savePublicKey(publicKey);
			log.info("公钥获取并保存成功");
		} else {
			log.info("本地已存在公钥，检查公钥是否一致");
			// 如果有，则检查公钥是否一致
			String localPublicKey = readLocalPublicKey();
			boolean isConsistent = checkPublicKeyConsistency(localPublicKey);
			
			// 2. 如果不一致，则获取最新的公钥并保存
			if (!isConsistent) {
				log.warn("本地公钥与认证服务不一致，更新公钥");
				String newPublicKey = fetchPublicKeyFromAuthService();
				savePublicKey(newPublicKey);
				log.info("公钥更新成功");
			} else {
				log.info("公钥一致性检查通过");
			}
		}
		
		// 3. 初始化公钥
		JwtUtil.initKey();
		
		log.info("公钥配置初始化完成");
	}
	
	/**
	 * 检查本地是否存在公钥文件
	 *
	 * @return true-存在，false-不存在
	 */
	private boolean hasLocalPublicKey() {
		Path path = Paths.get(publicKeyPath);
		boolean exists = Files.exists(path) && Files.isRegularFile(path);
		log.debug("检查本地公钥文件: path={}, exists={}", publicKeyPath, exists);
		return exists;
	}
	
	/**
	 * 从认证服务获取公钥
	 *
	 * @return 公钥字符串
	 * @throws Exception 获取失败时抛出异常
	 */
	private String fetchPublicKeyFromAuthService() throws Exception {
		log.info("调用认证服务获取公钥");
		RpcResponse<String> response = authRpcClient.getPublicKey();
		
		if (response == null || !Objects.equals(response.getCode(), ResponseCode.SUCCESS.getCode())) {
			String errorMsg = response != null ? response.getMessage() : "响应为空";
			log.error("获取公钥失败: {}", errorMsg);
			throw new RuntimeException("获取公钥失败: " + errorMsg);
		}
		
		String publicKey = response.getData();
		if (publicKey == null || publicKey.trim().isEmpty()) {
			log.error("获取的公钥为空");
			throw new RuntimeException("获取的公钥为空");
		}
		
		log.info("成功从认证服务获取公钥");
		return publicKey;
	}
	
	/**
	 * 读取本地公钥
	 *
	 * @return 公钥字符串
	 * @throws IOException 读取失败时抛出异常
	 */
	private String readLocalPublicKey() throws IOException {
		Path path = Paths.get(publicKeyPath);
		String publicKey = Files.readString(path);
		log.debug("读取本地公钥: path={}", publicKeyPath);
		return publicKey.trim();
	}
	
	/**
	 * 保存公钥到本地文件
	 *
	 * @param publicKey 公钥字符串
	 * @throws IOException 保存失败时抛出异常
	 */
	private void savePublicKey(String publicKey) throws IOException {
		Path path = Paths.get(publicKeyPath);
		
		// 确保父目录存在
		Path parentDir = path.getParent();
		if (parentDir != null && !Files.exists(parentDir)) {
			Files.createDirectories(parentDir);
			log.info("创建公钥存储目录: {}", parentDir);
		}
		
		// 写入公钥文件
		Files.writeString(path, publicKey, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		log.info("公钥已保存到本地: {}", publicKeyPath);
	}
	
	/**
	 * 检查本地公钥与认证服务的公钥是否一致
	 *
	 * @param localPublicKey 本地公钥
	 * @return true-一致，false-不一致
	 */
	private boolean checkPublicKeyConsistency(String localPublicKey) {
		try {
			log.info("调用认证服务检查公钥一致性");
			RpcResponse<Boolean> response = authRpcClient.checkPublicKey(localPublicKey);
			
			if (response == null || !Objects.equals(response.getCode(), ResponseCode.SUCCESS.getCode())) {
				String errorMsg = response != null ? response.getMessage() : "响应为空";
				log.error("检查公钥一致性失败: {}", errorMsg);
				return false;
			}
			
			Boolean isConsistent = response.getData();
			log.info("公钥一致性检查结果: {}", isConsistent);
			return Boolean.TRUE.equals(isConsistent);
		} catch (Exception e) {
			log.error("检查公钥一致性时发生异常", e);
			return false;
		}
	}
	
}
