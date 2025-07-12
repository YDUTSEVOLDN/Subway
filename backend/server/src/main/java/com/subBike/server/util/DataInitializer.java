package com.subBike.server.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CsvImportService csvImportService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("===== 开始执行数据初始化程序 =====");
        try {
            // 首先清空数据表，确保幂等性
            // 注意：ddl-auto: create 会自动完成清空，此处逻辑可作为双重保障
            // subAmountMapper.deleteAllInBatch(); // 如果需要手动清空
            
            System.out.println("正在从 classpath:csv/ 目录导入数据...");
            csvImportService.importCsvFiles("classpath:csv/");
            System.out.println("===== 数据初始化完成 =====");
        } catch (Exception e) {
            System.err.println("数据初始化过程中发生错误：" + e.getMessage());
            e.printStackTrace();
        }
    }
} 