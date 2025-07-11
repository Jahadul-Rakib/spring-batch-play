package com.rakib.springbatchplay;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@SpringBootApplication
public class SpringBatchPlayApplication {

    private static final String filePath = "src/main/resources/generated/generated_data.xlsx";

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchPlayApplication.class, args);
        if (!new File(filePath).exists()) {
            generate_excel_row();
        }
    }

    private static void generate_excel_row() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");


        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("id");
        header.createCell(1).setCellValue("name");
        header.createCell(2).setCellValue("type");

        for (int i = 1; i <= 30000; i++) {
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(i);
            row.createCell(1).setCellValue((i % 2 == 0) ? "Test" : "Nut");
            row.createCell(2).setCellValue((i % 3 == 0) ? "ANY" : "Fruites");
        }

        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (FileOutputStream out = new FileOutputStream(file)) {
            workbook.write(out);
            System.out.println("Excel file written to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
