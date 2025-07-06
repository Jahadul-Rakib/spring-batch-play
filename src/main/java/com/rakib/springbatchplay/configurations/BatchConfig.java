package com.rakib.springbatchplay.configurations;

import com.rakib.springbatchplay.service.dto.FinalProduct;
import com.rakib.springbatchplay.steps.ExcelItemProcessor;
import com.rakib.springbatchplay.steps.ExcelItemReader;
import com.rakib.springbatchplay.steps.ExcelItemWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    /*---------Spring Batch DB INIT---------*/
    @Bean
    @ConditionalOnMissingBean(DataSourceInitializer.class)
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(true);
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-postgresql.sql"));

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);

        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM BATCH_JOB_INSTANCE LIMIT 1", Integer.class);
            log.info("Spring Batch tables already exist - skipping initialization");
            initializer.setEnabled(false);
        } catch (Exception e) {
            log.info("Initializing Spring Batch tables");
            initializer.setEnabled(true);
        }
        return initializer;
    }

    @Bean
    public VirtualThreadTaskExecutor taskExecutor() {
        return new VirtualThreadTaskExecutor("spring-batch-");
    }

    /*---------Job Observation---------*/
    @Bean
    public JobExplorer jobExplorer(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setTransactionManager(transactionManager);
        factory.setDataSource(dataSource);
        factory.setTablePrefix("BATCH_");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setIsolationLevelForCreate("ISOLATION_DEFAULT");
        factory.setTablePrefix("BATCH_");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobRegistry jobRegistry() {
        return new MapJobRegistry();
    }

    @Bean
    public JobOperator jobOperator(JobRepository jobRepository, JobExplorer jobExplorer, JobRegistry jobRegistry, JobLauncher jobLauncher) throws Exception {
        SimpleJobOperator operator = new SimpleJobOperator();
        operator.setJobRepository(jobRepository);
        operator.setJobExplorer(jobExplorer);
        operator.setJobRegistry(jobRegistry);
        operator.setJobLauncher(jobLauncher);
        operator.afterPropertiesSet();
        return operator;
    }

    /*---------Job Specific Beans---------*/
    @Bean
    @Qualifier("excelProcessingJob")
    public Job excelProcessingJob(JobRepository jobRepository,
                                  JobExecutionListener jobExecutionListener,
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
    public Step processExcelStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 ExcelItemReader excelItemReader,
                                 ExcelItemProcessor excelItemProcessor,
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
                            .get("input.file.path").toString();
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
