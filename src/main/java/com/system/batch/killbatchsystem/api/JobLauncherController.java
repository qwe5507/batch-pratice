package com.system.batch.killbatchsystem.api;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobLauncherController {
    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobExplorer jobExplorer;


    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private ApplicationContext applicationContext;

    @PostMapping("/{jobName}/start")
    public ResponseEntity<String> launchJob(
            @PathVariable String jobName) throws Exception {
        Job job;
        try {
            job = jobRegistry.getJob(jobName);
        } catch (NoSuchJobException e) {
            return ResponseEntity.badRequest().body("Unknown job name: " + jobName);
        }

        JobParameters jobParameters = new JobParametersBuilder(jobExplorer)
                .getNextJobParameters(job)
                .toJobParameters();

        JobExecution execution = jobLauncher.run(job, jobParameters);
        return ResponseEntity.ok("Job launched with ID: " + execution.getId());
    }
}