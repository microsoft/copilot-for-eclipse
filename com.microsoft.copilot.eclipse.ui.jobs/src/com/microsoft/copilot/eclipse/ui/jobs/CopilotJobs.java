package com.microsoft.copilot.eclipse.ui.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * The activator class for the Copilot Jobs plugin.
 */
public class CopilotJobs extends AbstractUIPlugin {

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    CopilotCore.getPlugin();
    Job initWaitJob = new Job("Waiting for Copilot initialization") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          // Wait until Core is initialized
          Job.getJobManager().join(CopilotCore.INIT_JOB_FAMILY, null);
          return Status.OK_STATUS;
        } catch (Exception e) {
          CopilotCore.LOGGER.error("Failed to wait for Copilot initialization", e);
          return Status.error("Failed to initialize GitHub Copilot for Jobs view.", e);
        }
      }
    };
    initWaitJob.setSystem(true);
    initWaitJob.schedule();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
  }
}
