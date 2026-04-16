// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Utility class for Copilot preference pages.
 */
public final class PreferencePageUtils {

  // Private constructor to prevent instantiation
  private PreferencePageUtils() {
  }

  /**
   * Creates an external link that opens the given URL in the system browser.
   *
   * @param composite the parent composite
   * @param label the link label (can contain <a/> tags)
   * @param tooltip the tooltip text
   */
  public static void createExternalLink(Composite composite, String label, String tooltip) {
    createLink(composite, label, tooltip, PreferencePageUtils::openUrlInBrowser);
  }

  /**
   * Creates a link that opens the given preference page.
   *
   * @param shell the parent shell
   * @param composite the parent composite
   * @param label the label
   * @param tooltip the tooltip
   * @param preferenceId the preference page ID
   */
  public static void createPreferenceLink(Shell shell, Composite composite, String label, String tooltip,
      String preferenceId) {
    createLink(composite, label, tooltip, event -> openPreferencePage(shell, preferenceId, event));
  }

  /**
   * Creates a link with common setup and custom selection behavior.
   *
   * @param composite the parent composite
   * @param label the link label
   * @param tooltip the tooltip text
   * @param selectionHandler the selection event handler
   */
  private static void createLink(Composite composite, String label, String tooltip,
      Consumer<SelectionEvent> selectionHandler) {
    final Link link = new Link(composite, SWT.NONE);
    link.setText(label);
    link.setToolTipText(tooltip);
    link.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectionHandler.accept(e);
      }
    });
  }

  /**
   * Opens a URL in the system browser.
   *
   * @param event the selection event containing the URL
   */
  private static void openUrlInBrowser(SelectionEvent event) {
    try {
      PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
    } catch (PartInitException | MalformedURLException e) {
      CopilotCore.LOGGER.error("Failed to open URL: " + event.text, e);
    }
  }

  /**
   * Opens a preference page.
   *
   * @param shell the parent shell
   * @param preferenceId the preference page ID
   * @param event the selection event
   */
  private static void openPreferencePage(Shell shell, String preferenceId, SelectionEvent event) {
    PreferencesUtil.createPreferenceDialogOn(shell, preferenceId, null, event);
  }
}