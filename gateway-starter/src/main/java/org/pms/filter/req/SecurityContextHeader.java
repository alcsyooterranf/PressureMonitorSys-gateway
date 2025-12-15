package org.pms.filter.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description 已认证用户信息, 使用请求头转化为JsonBase64编码格式传递, 发送给后端服务
 * @author alcsyooterranf
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityContextHeader {

    private Long id;
    private String username;
    private List<String> authorities;

}

