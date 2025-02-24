package com.gk.study.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "家政服务预约系统 API",
                version = "v1",
                description = "家政服务预约系统接口文档"
        )
)
public class SwaggerConfig {
}
