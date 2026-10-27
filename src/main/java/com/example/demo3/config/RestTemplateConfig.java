package com.example.demo3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HTTP 客户端配置：AI 助手调用智谱大模型用
 * 注意：RestTemplate 默认的 String 转换器用 ISO-8859-1，中文会乱码，
 *       这里显式指定 UTF-8 并放到转换器首位，保证请求/响应中文正常
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        StringHttpMessageConverter utf8Converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        utf8Converter.setSupportedMediaTypes(
                List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));
        restTemplate.getMessageConverters().add(0, utf8Converter);
        return restTemplate;
    }
}
