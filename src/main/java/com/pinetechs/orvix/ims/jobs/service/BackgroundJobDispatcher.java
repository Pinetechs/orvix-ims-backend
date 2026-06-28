package com.pinetechs.orvix.ims.jobs.service;

import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.enums.JobType;
import com.pinetechs.orvix.ims.jobs.processor.BackgroundJobProcessor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class BackgroundJobDispatcher {

    private final Map<JobType, BackgroundJobProcessor> processors = new EnumMap<>(JobType.class);

    public BackgroundJobDispatcher(List<BackgroundJobProcessor> processorList) {


        for (BackgroundJobProcessor processor : processorList) {
            processors.put(processor.getJobType(), processor);
        }
    }

    public void dispatch(BackgroundJob job) {
        BackgroundJobProcessor processor = processors.get(job.getJobType());

        if (processor == null) {
            throw new RuntimeException("No processor registered for job type: " + job.getJobType());
        }

         processor.process(job);
    }
}