package com.rakib.springbatchplay.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;


@Component
public class ExcelJobCompletionListener implements JobExecutionListener {
    Logger logger = LoggerFactory.getLogger(ExcelJobCompletionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Executing job: " + jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            logger.info("Job completed.");
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            logger.error("Job failed.");
        } else {
            logger.info("Job {}", jobExecution.getStatus());
        }
    }
}
