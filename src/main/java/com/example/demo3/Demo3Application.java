package com.example.demo3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot启动类
 * @SpringBootApplication = @Configuration + @ComponentScan + @EnableAutoConfiguration
 * 对应教材原生Spring的ConfigAnnotation配置类
 */
@SpringBootApplication
public class Demo3Application {
    public static void main(String[] args) {
        // SpringBoot自动创建IOC容器，替代教材手动 new AnnotationConfigApplicationContext
        SpringApplication.run(Demo3Application.class, args);
    }
}
