package com.subBike.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@EntityScan(basePackages = {"com.subBike.server.entity", "com.subBike.server.entity.id"})
@EnableJpaRepositories(basePackages = {"com.subBike.server.mapper", "com.subBike.server.repository"})
@SpringBootApplication

public class SuBikeApplication {
    //添加静态初始块
    static {System.setProperty("org.apache.commons.logging.Log","org.apache.commons.logging.impl.NoOpLog");}
    public static void main(String[] args) {
        SpringApplication.run(SuBikeApplication.class, args);
    }
}