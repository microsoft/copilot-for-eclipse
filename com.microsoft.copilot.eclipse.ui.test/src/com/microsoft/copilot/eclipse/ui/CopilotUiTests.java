package com.microsoft.copilot.eclipse.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;

import com.microsoft.copilot.eclipse.core.CopilotCore;

class CopilotUiTests {

  @Test
  void testCopilotCoreWakeUp() throws Exception {
    CopilotUi ui = new CopilotUi();
    ui.start(null);

    Job.getJobManager().join(CopilotCore.INIT_JOB_FAMILY, null);

    Bundle bundle = Platform.getBundle("com.microsoft.copilot.eclipse.core");

    assertNotNull(bundle);
    assertEquals(Bundle.ACTIVE, bundle.getState());
  }
}
