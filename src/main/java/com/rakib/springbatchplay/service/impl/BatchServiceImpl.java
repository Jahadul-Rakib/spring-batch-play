package com.rakib.springbatchplay.service.impl;

import com.rakib.springbatchplay.configurations.ApplicationConstant;
import com.rakib.springbatchplay.service.BatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BatchServiceImpl implements BatchService {

    private final JobLauncher jobLauncher;
    private final Job excelProcessingJob;

    public BatchServiceImpl(JobLauncher jobLauncher, @Qualifier("excelProcessingJob") Job excelProcessingJob) {
        this.jobLauncher = jobLauncher;
        this.excelProcessingJob = excelProcessingJob;
    }

    @Override
    public void startBatchDataProcessing(String filePath) {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString(ApplicationConstant.BATCH_FILE_PATH_PARAMETER, filePath, true)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        try {
            JobExecution execution = jobLauncher.run(excelProcessingJob, jobParameters);
            log.info("Batch Job Name: {}", execution.getJobInstance().getJobName());
            log.info("Batch execution id {}", execution.getId());
            log.info("Batch execution instance id {}", execution.getJobInstance().getId());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
