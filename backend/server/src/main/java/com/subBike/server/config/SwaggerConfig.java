package com.subBike.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SubBike API文档")
                        .version("1.0")
                        .description("共享单车系统API接口文档")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@subbike.com")));
    }
}