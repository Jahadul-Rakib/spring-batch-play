package com.rakib.springbatchplay.steps;

import com.rakib.springbatchplay.service.dto.FinalProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@StepScope
@Component
public class ExcelItemWriter implements ItemWriter<FinalProduct> {

    @Override
    public void write(Chunk<? extends FinalProduct> chunk) throws Exception {
        chunk.forEach(item -> {
            log.info(item.toString());
        });
    }
}
