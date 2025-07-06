package com.rakib.springbatchplay.steps;

import com.rakib.springbatchplay.service.dto.FinalProduct;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class ExcelItemProcessor implements ItemProcessor<Row, FinalProduct> {

    @Override
    public FinalProduct process(Row item) {
        return mapRowToDataRow(item);
    }

    private FinalProduct mapRowToDataRow(Row row) {
        Cell id = row.getCell(0);   // ProductId
        Cell name = row.getCell(1); // Name
        Cell type = row.getCell(2); // Type

        FinalProduct dataRow = new FinalProduct();
        dataRow.setId((long) id.getNumericCellValue());
        dataRow.setName(name.getStringCellValue());
        dataRow.setType(type.getStringCellValue());
        return dataRow;
    }
}
