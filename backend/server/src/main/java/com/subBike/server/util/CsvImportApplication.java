package com.subBike.server.util;



import com.subBike.server.util.CsvImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CsvImportApplication implements CommandLineRunner {

    @Autowired
    private CsvImportService csvImportService;

    public static void main(String[] args) {
        SpringApplication.run(CsvImportApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            // 指定CSV文件所在目录（相对于resources目录）
            String csvDirectory = "classpath:csv/";

            // 执行导入
            csvImportService.importCsvFiles(csvDirectory);

            System.out.println("===== CSV导入完成 =====");
        } catch (Exception e) {
            System.err.println("CSV导入失败：" + e.getMessage());
            e.printStackTrace();
        } finally {
            // 导入完成后退出程序
            System.exit(0);
        }
    }
}