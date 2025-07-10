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
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

    @Autowired
    private SubAmountMapper subAmountMapper;

    // 日期格式：匹配文件名中的 "2019-May-01"
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MMM-dd");
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
        // 1. 从文件名提取日期（如 "station_flow_2019-May-01.csv" -> "2019-May-01"）
        String fileName = file.getName();
        String dateStr = fileName.replace("station_flow_", "").replace(".csv", "");
        Date date = new Date(FILE_DATE_FORMAT.parse(dateStr).getTime());

        // 2. 读取CSV内容
        List<SubAmount> dataList = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] line;
            boolean isFirstLine = true;

            while ((line = reader.readNext()) != null) {
                // 跳过表头（station, hour, in_count, out_count）
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 3. 解析每行数据并封装为实体类
                SubAmount subAmount = new SubAmount();
                SubAmountID id = new SubAmountID();

                // 设置联合主键
                id.setDate(date);
                id.setStation(line[0]); // 第一列：station
                id.setTime(Integer.parseInt(line[1])); // 第二列：hour（转为Integer）

                // 设置其他字段
//                subAmount.setId(id); // 如果实体类中用了@EmbeddedId，需要这一步
                subAmount.setInNum(Integer.parseInt(line[2])); // 第三列：in_count
                subAmount.setOutNum(Integer.parseInt(line[3])); // 第四列：out_count

                dataList.add(subAmount);
            }
        }

        // 4. 批量保存到数据库
        if (!dataList.isEmpty()) {
            subAmountMapper.saveAll(dataList);
            System.out.println("导入成功：" + file.getName() + "，共" + dataList.size() + "条数据");
        }
    }
}