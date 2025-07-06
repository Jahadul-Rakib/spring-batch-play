package com.rakib.springbatchplay.steps;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Iterator;

@StepScope
@Component
public class ExcelItemReader implements ItemReader<Row> {

    private String filePath;
    private Workbook workbook;
    private Iterator<Row> rowIterator;

    @Value("#{jobParameters['input.file.path']}")
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Row read() throws Exception {
        if (rowIterator == null) {
            workbook = WorkbookFactory.create(new File(filePath));
            Sheet sheet = workbook.getSheetAt(0);
            rowIterator = sheet.iterator();
        }

        if (rowIterator.hasNext()) {
            Row currentRow = rowIterator.next();
            /// Skip header row
            if (currentRow.getRowNum() == 0) return read();
            return currentRow;
        } else {
            if (workbook != null) workbook.close();
            return null;
        }
    }
}
