package com.microsoft.copilot.eclipse.ui;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Activator class for the Copilot UI plugin.
 */
public class CopilotUi implements BundleActivator {

  @Override
  public void start(BundleContext context) throws Exception {
    // wake up the Core plugin by calling a method from it.
    CopilotCore.getPlugin();
  }

  @Override
  public void stop(BundleContext context) throws Exception {

  }

}
