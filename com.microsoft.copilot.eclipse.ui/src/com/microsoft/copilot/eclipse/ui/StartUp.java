// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.dialogs.Messages;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * Early startup the Copilot for Eclipse plugin.
 */
public class StartUp implements IStartup {

  @Override
  public void earlyStartup() {
    Bundle platform = Platform.getBundle("org.eclipse.platform");
    if (platform == null) {
      Platform.getLog(CopilotUi.getPlugin().getBundle())
          .log(Status.warning("Cannot determine the Eclipse platform version for the compatibility notice."));
    }

    Version platformVersion = platform == null ? null : platform.getVersion();
    int javaFeature = Runtime.version().feature();
    CompatibilityNotice notice = new CompatibilityNotice(
        ConfigurationScope.INSTANCE.getNode(CopilotUi.getPlugin().getBundle().getSymbolicName()));
    if (!notice.shouldShow(platformVersion, javaFeature)) {
      return;
    }

    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (PlatformUI.getWorkbench().isClosing() || !notice.shouldShow(platformVersion, javaFeature)) {
        return;
      }
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window == null) {
        IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
        if (windows.length == 0) {
          CopilotCore.LOGGER.error(
              new IllegalStateException("No workbench window is available for the compatibility notice."));
          return;
        }
        window = windows[0];
      }
      if (window.getShell().isDisposed()) {
        return;
      }

      MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(window.getShell(),
          Messages.compatibilityNotice_title, Messages.compatibilityNotice_message,
          Messages.compatibilityNotice_dontRemindAgain, false, null, null);
      if (dialog.getReturnCode() == IDialogConstants.OK_ID && dialog.getToggleState()) {
        Job savePreference = Job.create(Messages.compatibilityNotice_saveJobName, monitor -> {
          try {
            notice.dismiss();
            return Status.OK_STATUS;
          } catch (BackingStoreException e) {
            CopilotCore.LOGGER.error("Failed to persist compatibility notice dismissal", e);
            return Status.error("Failed to persist compatibility notice dismissal", e);
          }
        });
        savePreference.setSystem(true);
        savePreference.schedule();
      }
    });
  }

}
