package com.microsoft.copilot.eclipse.core.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A base class for jobs that need to be repeated at a specified interval.
 */
public abstract class RepeatingJob extends Job {
  protected long repeatDelay = 0;

  /**
   * Constructor for the RepeatingJob.
   *
   * @param jobName the name of the job
   * @param repeatPeriod the period in milliseconds after which the job should be repeated
   */
  protected RepeatingJob(String jobName, long repeatPeriod) {
    super(jobName);
    this.setSystem(true);
    repeatDelay = repeatPeriod;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    schedule(repeatDelay);
    return runTask(monitor);
  }

  /**
   * The task to be executed by the job. This method should be implemented by subclasses to define the specific behavior
   * of the job.
   */
  protected abstract IStatus runTask(IProgressMonitor monitor);

}
