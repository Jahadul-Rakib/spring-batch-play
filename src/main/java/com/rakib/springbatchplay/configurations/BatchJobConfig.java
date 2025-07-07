package com.rakib.springbatchplay.configurations;

import com.rakib.springbatchplay.service.dto.FinalProduct;
import com.rakib.springbatchplay.steps.ExcelItemProcessor;
import com.rakib.springbatchplay.steps.ExcelItemReader;
import com.rakib.springbatchplay.steps.ExcelItemWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class BatchJobConfig {
    @Bean
    @Qualifier("excelProcessingJob")
    public Job excelProcessingJob(JobRepository jobRepository, JobExecutionListener jobExecutionListener,
                                  @Qualifier("processExcelStep") Step processExcelStep,
                                  @Qualifier("cleanupStepForProduct") Step cleanUpStep) {
        return new JobBuilder("excelProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobExecutionListener)
                .start(processExcelStep)
                .next(cleanUpStep)
                .build();
    }

    @Bean
    @Qualifier("processExcelStep")
    public Step processExcelStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                 ExcelItemReader excelItemReader, ExcelItemProcessor excelItemProcessor,
                                 ExcelItemWriter excelItemWriter) {
        return new StepBuilder("processExcelStep", jobRepository)
                .<Row, FinalProduct>chunk(100, transactionManager)
                .reader(excelItemReader)
                .processor(excelItemProcessor)
                .writer(excelItemWriter)
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .retry(Exception.class)
                .listener(new StepExecutionListener() {
                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        log.info("all 3 step completed. status: {}", stepExecution.getStatus());
                        return stepExecution.getExitStatus();
                    }
                })
                .build();
    }


    @Bean
    @Qualifier("cleanupStepForProduct")
    public Step cleanupStepForProduct(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String filePath = chunkContext.getStepContext()
                            .getJobParameters()
                            .get(ApplicationConstant.BATCH_FILE_PATH_PARAMETER)
                            .toString();
                    try {
                        Path path = Paths.get(filePath);
                        boolean deleted = Files.deleteIfExists(path);
                        log.info("File {} deletion {}", filePath,
                                deleted ? "successful" : "failed (not found)");
                    } catch (IOException e) {
                        log.error("Failed to delete file: {}", filePath, e);
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();
    }
}
