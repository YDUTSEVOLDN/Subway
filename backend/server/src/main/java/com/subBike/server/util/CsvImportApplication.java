package com.subBike.server.util;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CsvImportApplication {
    // 数据库连接信息

    private static final String DB_URL =
            "jdbc:mysql://39.96.195.232:3306/subbike?useUnicode=true&characterEncoding=utf8";
    private static final String DB_USER = "admin";
    private static final String DB_PASSWORD = "password";

    // 日期格式：匹配文件名中的 "2019-May-01"
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MMM-dd", Locale.US);

    // SQL插入语句（批量执行）
    private static final String INSERT_SQL =
            "INSERT INTO sub_amount (date, station, time, in_num, out_num) " +
                    "VALUES (?, ?, ?, ?, ?)";

    public static void main(String[] args) {
        System.out.println("===== 开始CSV导入程序 =====");
        String directoryPath;
        if (args.length < 1) {
//            directoryPath="D:\\作业\\小学期\\subBike\\Subway\\backend\\server\\src\\main\\resources\\csv\\subway";
            System.err.println("请指定CSV文件目录作为参数（例如：java ... 目录路径）");

            return;
        }

       directoryPath = args[0];
        System.out.println("待导入的CSV目录：" + directoryPath);
        CsvImportApplication importer = new CsvImportApplication();

        try {
            importer.importCsvFiles(directoryPath);
            System.out.println("===== CSV导入完成 =====");
        } catch (Exception e) {
            System.err.println("CSV导入失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 导入指定目录下的所有CSV文件
     */
    public void importCsvFiles(String directoryPath) throws IOException, ParseException, SQLException {
        File directory = new File(directoryPath);
        System.out.println("检查目录是否存在：" + directory.getAbsolutePath());

        if (!directory.exists()) {
            throw new FileNotFoundException("目录不存在：" + directoryPath);
        }
        if (!directory.isDirectory()) {
            throw new IOException("路径不是目录：" + directoryPath);
        }

        File[] files = directory.listFiles((dir, name) -> {
            boolean match = name.startsWith("station_flow_") && name.endsWith(".csv");
            if (match) {
                System.out.println("匹配到CSV文件：" + name);
            }
            return match;
        });

        if (files == null || files.length == 0) {
            System.out.println("警告：未找到符合条件的CSV文件（前缀：station_flow_，后缀：.csv）");
            return;
        }
        System.out.println("共找到 " + files.length + " 个CSV文件，准备开始导入");

        // 批量处理所有文件
        try (Connection conn = getConnection()) {
            System.out.println("数据库连接成功，自动提交模式：" + conn.getAutoCommit());
            conn.setAutoCommit(false);
            System.out.println("已关闭自动提交，启用事务批量处理");

            for (File file : files) {
                System.out.println("\n===== 开始处理文件：" + file.getName() + " =====");
                importSingleCsv(file, conn);
                conn.commit();
                System.out.println("文件 " + file.getName() + " 提交成功");
            }
        } catch (SQLException e) {
            System.err.println("数据库操作失败，准备回滚事务：" + e.getMessage());
            throw e; // 抛出异常让上层处理
        }
    }

    /**
     * 导入单个CSV文件
     */
    private void importSingleCsv(File file, Connection conn) throws IOException, ParseException, SQLException {
        // 1. 从文件名提取日期
        String fileName = file.getName();
        System.out.println("处理文件：" + fileName + "（路径：" + file.getAbsolutePath() + "）");

        String dateStr = fileName.replace("station_flow_", "").replace(".csv", "");
        System.out.println("从文件名提取日期字符串：" + dateStr);

        java.util.Date utilDate = FILE_DATE_FORMAT.parse(dateStr);
        Date sqlDate = new Date(utilDate.getTime());
        System.out.println("解析日期结果：文件名日期字符串 -> " + dateStr + "，转换为SQL日期 -> " + sqlDate);

        // 读取CSV内容并批量插入
        try (
                // 先创建 PushbackReader 处理 BOM 头
                PushbackReader pushbackReader = new PushbackReader(
                        new InputStreamReader(new FileInputStream(file), "UTF-8"), 1);
                // 再用 BufferedReader 包装 PushbackReader（保持 BufferedReader 类型）
                BufferedReader reader = new BufferedReader(pushbackReader);
                PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)
        ) {
            System.out.println("文件读取编码：UTF-8，准备检测BOM头");

            // 检测并跳过UTF-8 BOM头
            if (pushbackReader.ready()) {
                int firstChar = pushbackReader.read();
                if (firstChar == 0xFEFF) {
                    System.out.println("检测到UTF-8 BOM头，已自动跳过");
                } else {
                    pushbackReader.unread(firstChar);
                    System.out.println("未检测到BOM头，第一个字符ASCII值：" + firstChar + "（字符：" + (char) firstChar + "）");
                }
            }

            String line;
            boolean isFirstLine = true;
            int count = 0;
            int batchSize = 1000;
            System.out.println("批处理大小：" + batchSize + "条/次");

            while ((line = reader.readLine()) != null) {
                // 跳过表头
                if (isFirstLine) {
                    System.out.println("跳过表头行：" + line);
                    isFirstLine = false;
                    continue;
                }

                // 解析CSV行
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (values.length < 4) {
                    System.err.printf("无效的CSV行（文件: %s, 行内容: %s，字段数：%d）%n",
                            fileName, line, values.length);
                    continue;
                }

                try {
                    // 调试：打印前3行数据（含字段值和长度）
                    if (count < 3) {
                        System.out.printf("第%d行数据解析结果：字段数=%d，值分别为：%n",
                                count + 1, values.length, values);
                        for (int i = 0; i < values.length; i++) {
                            System.out.printf("  字段%d：内容='%s'，长度=%d，ASCII码（前3位）：%s%n",
                                    i, values[i].trim(), values[i].length(),
                                    getFirst3Ascii(values[i].trim()));
                        }
                    }

                    // 设置SQL参数
                    pstmt.setDate(1, sqlDate);
                    pstmt.setString(2, values[0].trim());
                    pstmt.setInt(3, Integer.parseInt(values[1].trim()));
                    pstmt.setInt(4, Integer.parseInt(values[2].trim()));
                    pstmt.setInt(5, Integer.parseInt(values[3].trim()));

                    // 调试：打印第一条数据的SQL参数
                    if (count == 0) {
                        System.out.println("第一条数据的SQL参数：");
                        System.out.println("  date: " + sqlDate);
                        System.out.println("  station: '" + values[0].trim() + "'（原始值：'" + values[0] + "'）");
                        System.out.println("  time: " + values[1].trim());
                        System.out.println("  in_num: " + values[2].trim());
                        System.out.println("  out_num: " + values[3].trim());
                    }

                    pstmt.addBatch();
                    count++;

                    if (count % batchSize == 0) {
                        int[] results = pstmt.executeBatch();
                        System.out.printf("已处理 %d 条记录（文件: %s），批处理影响行数：%d%n",
                                count, fileName, results.length);
                    }
                } catch (NumberFormatException e) {
                    System.err.printf("数字转换错误（文件: %s, 行号: %d, 行内容: %s, 错误: %s）%n",
                            fileName, count + 1, line, e.getMessage());
                }
            }

            // 执行剩余批处理
            if (count % batchSize != 0) {
                int[] results = pstmt.executeBatch();
                System.out.printf("处理剩余记录：%d 条，批处理影响行数：%d%n",
                        count % batchSize, results.length);
            }

            System.out.printf("文件导入完成：%s，共处理 %d 条数据%n", fileName, count);

        } catch (IOException e) {
            System.err.printf("文件读取错误（文件: %s, 错误: %s）%n", fileName, e.getMessage());
            throw e; // 抛出异常触发事务回滚
        } catch (SQLException e) {
            System.err.printf("SQL执行错误（文件: %s, 错误: %s）%n", fileName, e.getMessage());
            throw e; // 抛出异常触发事务回滚
        }
    }

    /**
     * 获取数据库连接
     */
    private Connection getConnection() throws SQLException {
        try {
            // 新版本的驱动类名
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL驱动加载成功");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL驱动加载失败: " + e.getMessage());
            throw new SQLException("MySQL驱动未找到", e);
        }

        System.out.println("尝试连接数据库：" + DB_URL.split("\\?")[0] + "，用户：" + DB_USER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("数据库连接成功：" + conn);
        return conn;
    }

    /**
     * 辅助方法：获取字符串前3个字符的ASCII码（用于调试乱码）
     */
    private String getFirst3Ascii(String str) {
        if (str == null || str.isEmpty()) {
            return "空字符串";
        }
        StringBuilder sb = new StringBuilder();
        int len = Math.min(str.length(), 3);
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            sb.append((int) c);
            if (i < len - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}