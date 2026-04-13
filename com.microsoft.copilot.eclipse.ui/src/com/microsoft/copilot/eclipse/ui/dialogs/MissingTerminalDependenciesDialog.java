// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.dialogs;

import java.util.List;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.UiConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Dialog to inform users about missing terminal dependencies. Provides information about what needs to be installed and
 * a link to documentation.
 */
public class MissingTerminalDependenciesDialog extends TitleAreaDialog {

  private final String terminalType;
  private final List<String> missingDependencies;
  private Button dontShowAgainCheckbox;

  /**
   * Creates the dialog.
   *
   * @param parentShell the parent shell
   * @param terminalType the type of terminal (e.g., "Eclipse Terminal" or "TM Terminal")
   * @param missingDependencies list of missing bundle symbolic names
   */
  public MissingTerminalDependenciesDialog(Shell parentShell, String terminalType, List<String> missingDependencies) {
    super(parentShell);
    this.terminalType = terminalType;
    this.missingDependencies = missingDependencies;
    setShellStyle(getShellStyle() | SWT.RESIZE);
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(Messages.terminalDependencyDialog_shellTitle);
    loadIcon(shell);
  }

  private void loadIcon(Shell shell) {
    Image icon = UiUtils.buildImageFromPngPath("/icons/github_copilot.png");
    if (icon != null) {
      shell.setImage(icon);
      shell.addDisposeListener(e -> {
        if (!icon.isDisposed()) {
          icon.dispose();
        }
      });
    }
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    setTitle(Messages.terminalDependencyDialog_title);
    setMessage(NLS.bind(Messages.terminalDependencyDialog_message, terminalType));

    Composite area = (Composite) super.createDialogArea(parent);

    Composite container = new Composite(area, SWT.NONE);
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 15;
    layout.marginHeight = 15;
    layout.verticalSpacing = 10;
    container.setLayout(layout);

    // Missing dependencies section
    Label depsLabel = new Label(container, SWT.NONE);
    depsLabel.setText(Messages.terminalDependencyDialog_missingDependencies);
    depsLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
    for (String dep : missingDependencies) {
      Label depItem = new Label(container, SWT.NONE);
      depItem.setText("  - " + dep);
      depItem.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    Label instructionLabel = new Label(container, SWT.WRAP);
    instructionLabel.setText(NLS.bind(Messages.terminalDependencyDialog_instruction, terminalType));
    GridData instructionData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    instructionData.widthHint = 400;
    instructionData.verticalIndent = 10;
    instructionLabel.setLayoutData(instructionData);

    // Help link
    Link link = new Link(container, SWT.NONE);
    link.setText(NLS.bind(Messages.terminalDependencyDialog_helpLink, UiConstants.TERMINAL_DEPENDENCY_GUIDE_URL));
    link.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Program.launch(e.text);
      }
    });
    GridData linkData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    linkData.verticalIndent = 5;
    link.setLayoutData(linkData);

    // Don't show again checkbox
    dontShowAgainCheckbox = new Button(container, SWT.CHECK);
    dontShowAgainCheckbox.setText(Messages.terminalDependencyDialog_dontShowAgain);
    GridData checkboxData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    checkboxData.verticalIndent = 15;
    dontShowAgainCheckbox.setLayoutData(checkboxData);

    return area;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
  }

  @Override
  protected void okPressed() {
    if (dontShowAgainCheckbox != null && dontShowAgainCheckbox.getSelection()) {
      setSuppressDialog(true);
    }
    super.okPressed();
  }

  /**
   * Check if the dialog has been suppressed by user preference.
   *
   * @return true if the dialog should not be shown
   */
  public static boolean isDialogSuppressed() {
    IEclipsePreferences prefs = ConfigurationScope.INSTANCE
        .getNode(CopilotUi.getPlugin().getBundle().getSymbolicName());
    return prefs.getBoolean(Constants.SUPPRESS_TERMINAL_DEPENDENCY_DIALOG, false);
  }

  /**
   * Set whether to suppress the dialog.
   *
   * @param suppress true to suppress future dialogs
   */
  private static void setSuppressDialog(boolean suppress) {
    try {
      IEclipsePreferences prefs = ConfigurationScope.INSTANCE
          .getNode(CopilotUi.getPlugin().getBundle().getSymbolicName());
      prefs.putBoolean(Constants.SUPPRESS_TERMINAL_DEPENDENCY_DIALOG, suppress);
      prefs.flush();
    } catch (BackingStoreException e) {
      CopilotCore.LOGGER.error("Failed to persist terminal dialog preference", e);
    }
  }

  /**
   * Shows the dialog if not suppressed by user preference.
   *
   * @param terminalType the type of terminal required
   * @param missingDependencies list of missing dependencies
   */
  public static void showIfNotSuppressed(String terminalType, List<String> missingDependencies) {
    if (isDialogSuppressed()) {
      return;
    }

    SwtUtils.invokeOnDisplayThreadAsync(() -> {
      if (!PlatformUI.isWorkbenchRunning()) {
        return;
      }
      var window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window == null) {
        return;
      }
      Shell shell = window.getShell();
      MissingTerminalDependenciesDialog dialog = new MissingTerminalDependenciesDialog(shell, terminalType,
          missingDependencies);
      dialog.open();
    });
  }
}
