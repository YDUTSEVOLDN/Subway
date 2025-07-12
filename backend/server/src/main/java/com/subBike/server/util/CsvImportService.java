package com.subBike.server.util;



import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.subBike.server.entity.SubAmount;
import com.subBike.server.entity.id.SubAmountID;
import com.subBike.server.mapper.SubAmountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream; // 1. 导入
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader; // 2. 导入
import java.nio.charset.StandardCharsets; // 3. 导入
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // 1. 导入 Locale
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class CsvImportService {

    @Autowired
    private SubAmountMapper subAmountMapper;

    // 2. 指定 Locale.ENGLISH 以正确解析英文月份
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MMM-dd", Locale.ENGLISH);
    
    // 注意：MMM 解析英文月份时，默认 Locale 可能有问题，建议指定
    static {
        FILE_DATE_FORMAT.setLenient(false);
    }

    /**
     * 导入指定目录下的所有CSV文件
     * @param directoryPath CSV文件所在目录（如 "classpath:csv/"）
     */
    public void importCsvFiles(String directoryPath) throws IOException, CsvValidationException, ParseException {
        // 获取目录下的所有文件
        File directory = ResourceUtils.getFile(directoryPath);
        File[] files = directory.listFiles((dir, name) -> name.startsWith("station_flow_") && name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("未找到CSV文件");
            return;
        }

        // 批量处理所有文件
        for (File file : files) {
            importSingleCsv(file);
        }
    }

    /**
     * 导入单个CSV文件
     */
    private void importSingleCsv(File file) throws IOException, CsvValidationException, ParseException {
        // 1. 从文件名提取日期
        String fileName = file.getName();
        String dateStr = fileName.replace("station_flow_", "").replace(".csv", "");
        Date date = new Date(FILE_DATE_FORMAT.parse(dateStr).getTime());

        // 2. 逐行读取并保存
        int successCount = 0;
        int duplicateCount = 0;
        // 4. 明确使用 UTF-8 编码读取文件
        try (CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String[] line;
            boolean isFirstLine = true;

            while ((line = reader.readNext()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                try {
                    SubAmount subAmount = new SubAmount();
                    
                    subAmount.setDate(date);
                    subAmount.setStation(line[0]);
                    subAmount.setTime(Integer.parseInt(line[1]));
                    
                    // 更健壮的数字解析
                    int inNum = 0;
                    int outNum = 0;
                    try {
                        inNum = Integer.parseInt(line[2].trim());
                    } catch (NumberFormatException e) {
                        System.err.println("警告: 解析in_num失败 (文件: " + fileName + ", 行: " + String.join(",", line) + "). 使用默认值 0.");
                    }
                    try {
                        outNum = Integer.parseInt(line[3].trim());
                    } catch (NumberFormatException e) {
                        System.err.println("警告: 解析out_num失败 (文件: " + fileName + ", 行: " + String.join(",", line) + "). 使用默认值 0.");
                    }
                    
                    subAmount.setInNum(inNum);
                    subAmount.setOutNum(outNum);

                    subAmountMapper.save(subAmount);
                    successCount++;

                } catch (DataIntegrityViolationException e) {
                    // 捕获因主键重复导致的异常
                    duplicateCount++;
                } catch (Exception e) {
                    // 捕获其他行级错误
                     System.err.println("处理行数据时出错: " + String.join(",", line) + " | 错误: " + e.getMessage());
                }
            }
        }
        System.out.println("导入完成: " + fileName + " | 成功: " + successCount + "条 | 忽略重复: " + duplicateCount + "条");
    }
}