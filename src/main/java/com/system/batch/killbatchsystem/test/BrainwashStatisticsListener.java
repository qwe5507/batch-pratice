package com.system.batch.killbatchsystem.test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class BrainwashStatisticsListener implements StepExecutionListener {

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long writeCount = stepExecution.getWriteCount();
        long filterCount = stepExecution.getFilterCount();

        stepExecution.getExecutionContext().putLong("brainwashedVictimCount", writeCount);
        stepExecution.getExecutionContext().putLong("brainwashResistanceCount", filterCount);

        return stepExecution.getExitStatus();
    }
}