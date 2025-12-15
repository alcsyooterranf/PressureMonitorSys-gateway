package org.pms;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author alcsyooterranf
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@Configurable
@EnableFeignClients(basePackages = "org.pms.trigger.feign")
public class Application {
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class);
	}
	
}
