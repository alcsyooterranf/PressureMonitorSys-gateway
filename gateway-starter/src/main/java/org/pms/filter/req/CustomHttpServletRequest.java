package org.pms.filter.req;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alcsyooterranf
 */
public class CustomHttpServletRequest extends HttpServletRequestWrapper {

    // 用于存储自定义头的 Map
    private final Map<String, String> customHeaders = new HashMap<>();

    public CustomHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    /**
     * 添加一个自定义头
     *
     * @param name  头名称
     * @param value 头值
     */
    public void addHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    /**
     * 获取指定名称的头值
     *
     * @param name 头名称
     * @return 如果存在自定义头，则返回自定义头的值；否则返回原始请求的头值
     */
    @Override
    public String getHeader(String name) {
        // 优先从自定义头中获取
        String headerValue = customHeaders.get(name);
        if (headerValue != null) {
            return headerValue;
        }
        // 如果自定义头不存在，则调用原始请求的 getHeader 方法
        return super.getHeader(name);
    }

    /**
     * 获取所有头的名称
     *
     * @return 自定义头与原始请求头的集合
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        // 合并自定义头与原始请求头
        Map<String, String> combinedHeaders = new HashMap<>(customHeaders);
        Enumeration<String> originalHeaderNames = super.getHeaderNames();
        while (originalHeaderNames.hasMoreElements()) {
            String headerName = originalHeaderNames.nextElement();
            combinedHeaders.putIfAbsent(headerName, super.getHeader(headerName));
        }
        return Collections.enumeration(combinedHeaders.keySet());
    }

    /**
     * 获取指定名称的所有头值
     *
     * @param name 头名称
     * @return 自定义头的值（如果存在），或者原始请求头的值
     */
    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(Collections.singletonList(customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }

}

