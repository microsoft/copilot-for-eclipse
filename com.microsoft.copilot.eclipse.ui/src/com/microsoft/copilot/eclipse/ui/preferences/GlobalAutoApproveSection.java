// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.copilot.eclipse.core.Constants;

/**
 * Global auto-approve preference section with a YOLO mode checkbox
 * that bypasses all confirmation dialogs when enabled.
 */
public class GlobalAutoApproveSection extends Composite {

  private static final int TOOLTIP_LINE_LENGTH = 90;

  private Button yoloCheckbox;

  /** Creates the global auto-approve section inside the given parent. */
  public GlobalAutoApproveSection(Composite parent, int style) {
    super(parent, style);
    setLayout(new GridLayout(1, false));
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    createContents();
  }

  private void createContents() {
    Group group = new Group(this, SWT.NONE);
    group.setText(Messages.preferences_page_global_auto_approve_title);
    group.setLayout(new GridLayout(1, false));
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    group.setBackgroundMode(SWT.INHERIT_FORCE);

    Composite yoloRow = new Composite(group, SWT.NONE);
    GridLayout yoloRowLayout = new GridLayout(2, false);
    yoloRowLayout.marginWidth = 0;
    yoloRowLayout.marginHeight = 0;
    yoloRow.setLayout(yoloRowLayout);
    yoloRow.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    yoloCheckbox = new Button(yoloRow, SWT.CHECK);
    yoloCheckbox.setText(
        Messages.preferences_page_global_auto_approve_label);
    yoloCheckbox.setLayoutData(
        new GridData(SWT.LEFT, SWT.CENTER, false, false));
    yoloCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (yoloCheckbox.getSelection()) {
          MessageDialog dialog = new MessageDialog(
              getShell(),
              Messages.preferences_page_global_auto_approve_confirm_title,
              null,
              Messages.preferences_page_global_auto_approve_confirm_message,
              MessageDialog.WARNING,
              new String[] {
                  Messages.preferences_page_global_auto_approve_confirm_button,
                  Messages.preferences_page_global_auto_approve_cancel_button
              },
              1);
          int result = dialog.open();
          if (result != 0) {
            yoloCheckbox.setSelection(false);
          }
        }
      }
    });

    Label warningIcon = new Label(yoloRow, SWT.NONE);
    warningIcon.setImage(PlatformUI.getWorkbench().getSharedImages()
        .getImage(ISharedImages.IMG_OBJS_WARN_TSK));
    warningIcon.setToolTipText(wrapTooltip(
        Messages.preferences_page_global_auto_approve_confirm_message));
    warningIcon.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

    new WrappableNoteLabel(group,
      Messages.preferences_page_note_prefix + " ",
      Messages.preferences_page_global_auto_approve_confirm_message);
  }

  /** Loads global auto-approve settings from the preference store. */
  public void loadFromPreferences(IPreferenceStore store) {
    yoloCheckbox.setSelection(
        store.getBoolean(Constants.AUTO_APPROVE_YOLO_MODE));
  }

  /** Saves global auto-approve settings to the preference store. */
  public void saveToPreferences(IPreferenceStore store) {
    store.setValue(Constants.AUTO_APPROVE_YOLO_MODE,
        yoloCheckbox.getSelection());
  }

  private static String wrapTooltip(String text) {
    StringBuilder wrapped = new StringBuilder(text.length());
    int lineLength = 0;
    for (String word : text.split(" ")) {
      if (lineLength > 0 && lineLength + word.length() + 1 > TOOLTIP_LINE_LENGTH) {
        wrapped.append('\n');
        lineLength = 0;
      } else if (lineLength > 0) {
        wrapped.append(' ');
        lineLength++;
      }
      wrapped.append(word);
      lineLength += word.length();
    }
    return wrapped.toString();
  }
}
