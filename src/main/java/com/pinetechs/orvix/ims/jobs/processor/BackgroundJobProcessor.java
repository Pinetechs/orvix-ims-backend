package com.pinetechs.orvix.ims.jobs.processor;

import com.pinetechs.orvix.ims.jobs.entity.BackgroundJob;
import com.pinetechs.orvix.ims.jobs.enums.JobType;

public interface BackgroundJobProcessor {
    JobType getJobType();

    void process(BackgroundJob job);
}
