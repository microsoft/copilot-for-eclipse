// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.TerminalAutoApproveRule;
import com.microsoft.copilot.eclipse.ui.chat.confirmation.TerminalConfirmationHandler;

/**
 * Terminal auto-approve section with a rule table, action buttons, and
 * unmatched-command checkbox.
 */
public class TerminalAutoApproveSection extends Composite {

  private static final int TABLE_HEIGHT_HINT = 200;
  private static final Type TERMINAL_RULES_TYPE =
      new TypeToken<List<TerminalAutoApproveRule>>() {}.getType();

  private TableViewer tableViewer;
  private List<TerminalAutoApproveRule> rules = new ArrayList<>();
  private Button removeButton;
  private Button toggleButton;
  private Button resetButton;
  private Button unmatchedCheckbox;

  /** Creates the terminal auto-approve section inside the given parent. */
  public TerminalAutoApproveSection(Composite parent, int style) {
    super(parent, style);
    setLayout(new GridLayout(1, false));
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    createContents();
  }

  private void createContents() {
    Group group = new Group(this, SWT.NONE);
    group.setText(Messages.preferences_page_terminal_auto_approve_title);
    group.setLayout(new GridLayout(1, false));
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    group.setBackgroundMode(SWT.INHERIT_FORCE);

    Label description = new Label(group, SWT.WRAP);
    description.setText(
        Messages.preferences_page_terminal_auto_approve_description);
    GridData descData = new GridData(SWT.FILL, SWT.TOP, true, false);
    descData.widthHint = 400;
    description.setLayoutData(descData);

    Composite container = new Composite(group, SWT.NONE);
    container.setLayout(new GridLayout(2, false));
    container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    createTable(container);
    createButtons(container);

    unmatchedCheckbox = new Button(group, SWT.CHECK);
    unmatchedCheckbox.setText(
        Messages.preferences_page_terminal_auto_approve_unmatched);
    unmatchedCheckbox.setLayoutData(
        new GridData(SWT.FILL, SWT.TOP, true, false));

    new WrappableNoteLabel(group,
        Messages.preferences_page_note_prefix + " ",
        Messages.preferences_page_terminal_auto_approve_unmatched_note);
  }

  private void createTable(Composite parent) {
    tableViewer = new TableViewer(parent,
        SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE);
    Table table = tableViewer.getTable();
    GridData tableData = new GridData(SWT.FILL, SWT.FILL, true, false);
    tableData.heightHint = TABLE_HEIGHT_HINT;
    table.setLayoutData(tableData);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    TableViewerColumn commandCol =
        new TableViewerColumn(tableViewer, SWT.NONE);
    commandCol.getColumn().setText(
        Messages.preferences_page_terminal_auto_approve_column_command);
    commandCol.getColumn().setWidth(300);
    commandCol.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return ((TerminalAutoApproveRule) element).getCommand();
      }
    });

    TableViewerColumn statusCol =
        new TableViewerColumn(tableViewer, SWT.NONE);
    statusCol.getColumn().setText(
        Messages.preferences_page_auto_approve_column_status);
    statusCol.getColumn().setWidth(100);
    statusCol.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return ((TerminalAutoApproveRule) element).isAutoApprove()
            ? Messages.preferences_page_auto_approve_allow
            : Messages.preferences_page_auto_approve_deny;
      }
    });

    tableViewer.setContentProvider(ArrayContentProvider.getInstance());
    tableViewer.addSelectionChangedListener(e -> updateButtonState());
  }

  private void createButtons(Composite parent) {
    Composite btnGroup = new Composite(parent, SWT.NONE);
    btnGroup.setLayout(new GridLayout(1, false));
    btnGroup.setLayoutData(
        new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

    Button addButton = new Button(btnGroup, SWT.PUSH);
    addButton.setText(Messages.preferences_page_auto_approve_add);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    addButton.addListener(SWT.Selection, e -> onAdd());

    removeButton = new Button(btnGroup, SWT.PUSH);
    removeButton.setText(
        Messages.preferences_page_auto_approve_remove);
    removeButton.setLayoutData(
        new GridData(SWT.FILL, SWT.TOP, true, false));
    removeButton.setEnabled(false);
    removeButton.addListener(SWT.Selection, e -> onRemove());

    toggleButton = new Button(btnGroup, SWT.PUSH);
    toggleButton.setText(
        Messages.preferences_page_auto_approve_allow);
    toggleButton.setLayoutData(
        new GridData(SWT.FILL, SWT.TOP, true, false));
    toggleButton.setEnabled(false);
    toggleButton.addListener(SWT.Selection, e -> onToggle());

    resetButton = new Button(btnGroup, SWT.PUSH);
    resetButton.setText(
        Messages.preferences_page_auto_approve_reset);
    resetButton.setLayoutData(
        new GridData(SWT.FILL, SWT.TOP, true, false));
    resetButton.addListener(SWT.Selection, e -> onResetToDefaults());
  }

  private void onAdd() {
    AddTerminalRuleDialog dialog = new AddTerminalRuleDialog(getShell());
    if (dialog.open() == AddTerminalRuleDialog.OK) {
      String command = dialog.getCommand();
      // Remove existing rule with same command, then add at end
      rules.removeIf(r -> r.getCommand().equals(command));
      rules.add(new TerminalAutoApproveRule(command, dialog.isAutoApprove()));
      tableViewer.refresh();
      updateButtonState();
    }
  }

  private void onRemove() {
    IStructuredSelection sel = tableViewer.getStructuredSelection();
    if (!sel.isEmpty()) {
      rules.remove(sel.getFirstElement());
      tableViewer.refresh();
      updateButtonState();
    }
  }

  private void onToggle() {
    IStructuredSelection sel = tableViewer.getStructuredSelection();
    if (!sel.isEmpty()) {
      TerminalAutoApproveRule rule =
          (TerminalAutoApproveRule) sel.getFirstElement();
      rule.setAutoApprove(!rule.isAutoApprove());
      tableViewer.refresh();
      updateButtonState();
    }
  }

  private void onResetToDefaults() {
    boolean confirmed = MessageDialog.openQuestion(getShell(),
        Messages.preferences_page_terminal_auto_approve_reset_title,
        Messages.preferences_page_auto_approve_reset_message);
    if (confirmed) {
      rules.clear();
      rules.addAll(TerminalConfirmationHandler.DEFAULT_RULES.stream()
          .map(r -> new TerminalAutoApproveRule(
              r.getCommand(), r.isAutoApprove()))
          .toList());
      tableViewer.refresh();
      updateButtonState();
    }
  }

  private void updateButtonState() {
    boolean hasSelection =
        !tableViewer.getStructuredSelection().isEmpty();
    removeButton.setEnabled(hasSelection);
    toggleButton.setEnabled(hasSelection);
    if (hasSelection) {
      TerminalAutoApproveRule rule = (TerminalAutoApproveRule)
          tableViewer.getStructuredSelection().getFirstElement();
      toggleButton.setText(rule.isAutoApprove()
          ? Messages.preferences_page_auto_approve_deny
          : Messages.preferences_page_auto_approve_allow);
    } else {
      toggleButton.setText(
          Messages.preferences_page_auto_approve_allow);
    }
    resetButton.setEnabled(!isMatchingDefaults());
  }

  private boolean isMatchingDefaults() {
    List<TerminalAutoApproveRule> defaults = TerminalConfirmationHandler.DEFAULT_RULES;
    if (rules.size() != defaults.size()) {
      return false;
    }
    for (int i = 0; i < rules.size(); i++) {
      if (!rules.get(i).getCommand().equals(defaults.get(i).getCommand())
          || rules.get(i).isAutoApprove() != defaults.get(i).isAutoApprove()) {
        return false;
      }
    }
    return true;
  }

  /** Loads terminal rules and unmatched-command preference from the store. */
  public void loadFromPreferences(IPreferenceStore store) {
    String json = store.getString(Constants.AUTO_APPROVE_TERMINAL_RULES);
    rules.clear();
    if (StringUtils.isNotBlank(json) && !"[]".equals(json.trim())) {
      try {
        List<TerminalAutoApproveRule> loaded =
            new Gson().fromJson(json, TERMINAL_RULES_TYPE);
        if (loaded != null) {
          rules.addAll(loaded);
        }
      } catch (Exception e) {
        CopilotCore.LOGGER.error(
            "Failed to parse terminal auto-approve rules from preferences", e);
      }
    }
    tableViewer.setInput(rules);

    unmatchedCheckbox.setSelection(
        store.getBoolean(Constants.AUTO_APPROVE_UNMATCHED_TERMINAL));
    updateButtonState();
  }

  /** Saves terminal rules and unmatched-command preference to the store. */
  public void saveToPreferences(IPreferenceStore store) {
    store.setValue(Constants.AUTO_APPROVE_TERMINAL_RULES,
        new Gson().toJson(rules));
    store.setValue(Constants.AUTO_APPROVE_UNMATCHED_TERMINAL,
        unmatchedCheckbox.getSelection());
  }
}
