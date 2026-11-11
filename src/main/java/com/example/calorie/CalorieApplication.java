package com.example.calorie;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * 启动类。
 * 继承 SpringBootServletInitializer，打成 war 后可部署到外置 Tomcat 11。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.example.calorie.mapper")
@EnableScheduling
public class CalorieApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        // 统一时区，避免统计日期出现偏差
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(CalorieApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CalorieApplication.class);
    }
}
