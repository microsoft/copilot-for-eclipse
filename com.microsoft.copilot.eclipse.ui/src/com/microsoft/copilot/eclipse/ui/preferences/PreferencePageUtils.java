// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.osgi.util.NLS;
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
import com.microsoft.copilot.eclipse.core.chat.CustomChatMode;
import com.microsoft.copilot.eclipse.core.utils.WorkspaceUtils;

/**
 * Utility class for Copilot preference pages.
 */
public final class PreferencePageUtils {

  /**
   * Target content height, in pixels, for Copilot preference pages. JFace grows the shared Preferences dialog to
   * the tallest page's preferred height and never shrinks it, so each page keeps its scrollable content within
   * this height to hold the dialog at a stable size while the user navigates. Pages enforce it differently:
   * {@code McpPreferencePage} divides it across two stacked groups; {@code AutoApprovePreferencePage} caps its
   * {@code ScrolledComposite} at this height.
   */
  public static final int STANDARD_CONTENT_HEIGHT = 520;

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

  /**
   * Returns the name of the workspace folder containing the given custom agent: the name of the project, or for a
   * folder of a parent git repository its name marked as parent repository. If several parent repository folders
   * have the same name, the folder path is returned instead to keep them distinguishable.
   *
   * @param mode the custom agent
   * @return the folder name, or an empty string if the agent is not located in a known folder
   */
  public static String getCustomAgentFolderName(CustomChatMode mode) {
    try {
      Path modePath = Paths.get(URI.create(mode.getId()));
      List<WorkspaceFolder> projectFolders = WorkspaceUtils.listWorkspaceFolders();
      for (WorkspaceFolder folder : projectFolders) {
        if (modePath.startsWith(Paths.get(URI.create(folder.getUri())))) {
          return folder.getName();
        }
      }
      if (!WorkspaceUtils.isParentRepositoryEnabled()) {
        return "";
      }

      List<WorkspaceFolder> parentFolders = WorkspaceUtils.listParentRepositoryFolders(projectFolders);
      for (WorkspaceFolder folder : parentFolders) {
        Path folderPath = Paths.get(URI.create(folder.getUri()));
        if (modePath.startsWith(folderPath)) {
          long sameNameCount = parentFolders.stream().filter(f -> f.getName().equals(folder.getName())).count();
          return sameNameCount > 1 ? folderPath.toString()
              : NLS.bind(Messages.preferences_page_parent_repository_folder, folder.getName());
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to get the folder name for custom agent id=" + mode.getId(), e);
    }
    return "";
  }
}