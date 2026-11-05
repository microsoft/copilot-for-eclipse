// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.terminal.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

/**
 * Verifies that the terminal implementation bundle is started by the framework and registers its
 * {@link IRunInTerminalTool} service without anyone calling {@link Bundle#start()} explicitly, the way it is
 * installed by p2 as an ordinary plug-in of the feature.
 */
class TerminalServiceManagerTest {

  private static final String TERMINAL_BUNDLE_ID = "com.microsoft.copilot.eclipse.ui.terminal";
  private static final long SERVICE_TIMEOUT_SECONDS = 30;

  @Test
  void testTerminalBundle_declaresLazyActivationPolicy_soEquinoxAutoStartsIt() {
    Bundle terminalBundle = getInstalledTerminalBundle();

    // Equinox marks lazy bundles as auto-started on install (osgi.compatibility.eagerStart.LazyActivation)
    assertEquals(Constants.ACTIVATION_LAZY, terminalBundle.getHeaders("").get(Constants.BUNDLE_ACTIVATIONPOLICY),
        "Without a lazy activation policy the terminal bundle stays RESOLVED and its DS component is never activated");
  }

  @Test
  void testTerminalBundle_installedWithoutExplicitStart_isStarted() {
    Bundle terminalBundle = getInstalledTerminalBundle();

    int state = terminalBundle.getState();
    assertTrue(state == Bundle.STARTING || state == Bundle.ACTIVE,
        "Terminal bundle should have been started by the framework, but its state is " + state);
  }

  @Test
  void testGetCurrentService_terminalBundleInstalled_providesRunInTerminalTool() throws Exception {
    Bundle terminalBundle = getInstalledTerminalBundle();
    TerminalServiceManager manager = TerminalServiceManager.getInstance();
    assertNotNull(manager);

    CompletableFuture<IRunInTerminalTool> serviceFuture = new CompletableFuture<>();
    TerminalServiceManager.TerminalServiceListener listener = serviceFuture::complete;
    manager.addListener(listener);
    try {
      IRunInTerminalTool service = manager.getCurrentService() != null ? manager.getCurrentService()
          : serviceFuture.get(SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

      assertNotNull(service);
      assertEquals(terminalBundle, FrameworkUtil.getBundle(service.getClass()));
    } finally {
      manager.removeListener(listener);
    }
  }

  private static Bundle getInstalledTerminalBundle() {
    Bundle terminalBundle = Platform.getBundle(TERMINAL_BUNDLE_ID);
    assertNotNull(terminalBundle, TERMINAL_BUNDLE_ID + " is not installed in the test runtime");
    return terminalBundle;
  }
}
