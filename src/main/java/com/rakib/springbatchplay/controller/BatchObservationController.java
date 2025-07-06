package com.rakib.springbatchplay.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/batch")
public class BatchObservationController {
    private final JobExplorer jobExplorer;
    private final JobOperator jobOperator;
    private final MeterRegistry meterRegistry;

    public BatchObservationController(JobExplorer jobExplorer,
                                      JobOperator jobOperator,
                                      MeterRegistry meterRegistry) {
        this.jobExplorer = jobExplorer;
        this.jobOperator = jobOperator;
        this.meterRegistry = meterRegistry;
    }

    // Get all job instances
    @GetMapping("/jobs")
    public ResponseEntity<List<JobInstance>> getAllJobInstances(
            @RequestParam(required = false) String jobName,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int count) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, start, count);
        return ResponseEntity.ok(jobInstances);
    }

    // Get job executions for a specific instance
    @GetMapping("/jobs/{jobInstanceId}/executions")
    public ResponseEntity<List<JobExecution>> getJobExecutions(@PathVariable long jobInstanceId) {
        List<JobExecution> executions = jobExplorer
                .getJobExecutions(Objects.requireNonNull(jobExplorer.getJobInstance(jobInstanceId)));
        return ResponseEntity.ok(executions);
    }

    // Get execution details
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<JobExecution> getJobExecution(@PathVariable long executionId) {
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        return ResponseEntity.ok(execution);
    }

    // Get step execution details
    @GetMapping("/executions/{executionId}/steps")
    public ResponseEntity<Collection<StepExecution>> getStepExecutions(@PathVariable long executionId) {
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        assert execution != null;
        return ResponseEntity.ok(execution.getStepExecutions());
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getBatchMetrics(@RequestParam String jobName) {
        Map<String, Object> metrics = new LinkedHashMap<>();

        metrics.put("jobsCompleted", meterRegistry.counter(jobName, "status", "COMPLETED").count());
        metrics.put("jobsFailed", meterRegistry.counter(jobName, "status", "FAILED").count());

        Timer timer = meterRegistry.timer("spring.batch.job.execution.time");
        metrics.put("avgExecutionTime", timer.mean(TimeUnit.SECONDS));
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/jobs/search")
    public ResponseEntity<List<JobExecution>> searchExecutions(
            @RequestParam(required = false) String jobName) throws NoSuchJobException {
        List<JobExecution> jobExecutionList = new ArrayList<>();
        Set<Long> runningExecutions = jobOperator.getRunningExecutions(jobName);
        runningExecutions.forEach(executionId -> {
            JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
            jobExecutionList.add(jobExecution);
        });
        return ResponseEntity.ok(jobExecutionList);
    }
}
