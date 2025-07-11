package com.subBike.server.util;

import org.apache.ibatis.mapping.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {


    @Value("${spring.jwt.secret}")  // 必须与 YAML 中的 `jwt.secret` 匹配
    private String jwtSecret;
//
//    @Autowired
//    private Environment env;  // 直接注入 Environment 作为备选方案

    @GetMapping("/test")
    public String test() {
        // 方式1：通过 @Value 注入
        String valueFromAnnotation = jwtSecret;

        // 方式2：通过 Environment 获取
        //String valueFromEnv = env.getProperty("jwt.secret");

        return "Value from @Value: " + valueFromAnnotation + "\n"
                ;
    }
}