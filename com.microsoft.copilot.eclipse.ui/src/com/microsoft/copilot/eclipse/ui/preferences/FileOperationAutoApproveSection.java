// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.preferences;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.FileOperationAutoApproveRule;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.FileSafetyRuleInfo;
import com.microsoft.copilot.eclipse.ui.chat.confirmation.FileOperationConfirmationHandler;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

/**
 * File-operation auto-approve section with a rule table, action buttons, and
 * unmatched-file-operation checkbox.
 *
 * <p>Default rules are fetched from CLS asynchronously and merged with
 * local fallback defaults
 * ({@link FileOperationConfirmationHandler#FALLBACK_DEFAULT_RULES}).
 * Default rules cannot be removed; only user-added rules can be removed.</p>
 */
public class FileOperationAutoApproveSection extends Composite {

  private static final int TABLE_HEIGHT_HINT = 200;
  private static final Type FILE_OP_RULES_TYPE =
      new TypeToken<List<FileOperationAutoApproveRule>>() {}.getType();

  private TableViewer tableViewer;
  private final List<FileOperationAutoApproveRule> defaultRules =
      new ArrayList<>();
  private final List<FileOperationAutoApproveRule> userRules =
      new ArrayList<>();
  /** Combined view (defaults + user) shown in the table. */
  private final List<FileOperationAutoApproveRule> allRules =
      new ArrayList<>();
  private boolean defaultRulesLoaded;
  private Button removeButton;
  private Button toggleButton;
  private Button resetButton;
  private Button unmatchedCheckbox;

  /** Creates the file-operation auto-approve section inside the given parent. */
  public FileOperationAutoApproveSection(Composite parent, int style) {
    super(parent, style);
    setLayout(new GridLayout(1, false));
    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    createContents();
  }

  private void createContents() {
    Group group = new Group(this, SWT.NONE);
    group.setText(Messages.preferences_page_file_op_auto_approve_title);
    group.setLayout(new GridLayout(1, false));
    group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    group.setBackgroundMode(SWT.INHERIT_FORCE);

    Label description = new Label(group, SWT.WRAP);
    description.setText(
        Messages.preferences_page_file_op_auto_approve_description);
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
        Messages.preferences_page_file_op_auto_approve_unmatched);
    unmatchedCheckbox.setLayoutData(
        new GridData(SWT.FILL, SWT.TOP, true, false));

    new WrappableNoteLabel(group,
        Messages.preferences_page_note_prefix + " ",
        Messages.preferences_page_file_op_auto_approve_unmatched_note);
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

    TableViewerColumn patternCol =
        new TableViewerColumn(tableViewer, SWT.NONE);
    patternCol.getColumn().setText(
        Messages.preferences_page_file_op_auto_approve_column_pattern);
    patternCol.getColumn().setWidth(200);
    patternCol.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return ((FileOperationAutoApproveRule) element).getPattern();
      }

      @Override
      public Color getForeground(Object element) {
        return ((FileOperationAutoApproveRule) element).isDefault()
            ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY) : null;
      }
    });

    TableViewerColumn descCol =
        new TableViewerColumn(tableViewer, SWT.NONE);
    descCol.getColumn().setText(
        Messages.preferences_page_file_op_auto_approve_column_description);
    descCol.getColumn().setWidth(150);
    descCol.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        String desc =
            ((FileOperationAutoApproveRule) element).getDescription();
        return desc != null ? desc : "";
      }

      @Override
      public Color getForeground(Object element) {
        return ((FileOperationAutoApproveRule) element).isDefault()
            ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY) : null;
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
        return ((FileOperationAutoApproveRule) element).isAutoApprove()
            ? Messages.preferences_page_auto_approve_allow
            : Messages.preferences_page_auto_approve_deny;
      }

      @Override
      public Color getForeground(Object element) {
        return ((FileOperationAutoApproveRule) element).isDefault()
            ? Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY) : null;
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
    AddFileOperationRuleDialog dialog =
        new AddFileOperationRuleDialog(getShell());
    if (dialog.open() == AddFileOperationRuleDialog.OK) {
      String pattern = dialog.getPattern();
      if (isPatternExists(pattern)) {
        MessageDialog.openWarning(getShell(),
            Messages.preferences_page_file_op_auto_approve_duplicate_title,
            Messages.preferences_page_file_op_auto_approve_duplicate_message);
        return;
      }
      userRules.add(new FileOperationAutoApproveRule(
          pattern, dialog.getDescription(), dialog.isAutoApprove()));
      rebuildAllRules();
      tableViewer.refresh();
      updateButtonState();
    }
  }

  private boolean isPatternExists(String pattern) {
    return defaultRules.stream()
            .anyMatch(r -> r.getPattern().equals(pattern))
        || userRules.stream()
            .anyMatch(r -> r.getPattern().equals(pattern));
  }

  private void onRemove() {
    IStructuredSelection sel = tableViewer.getStructuredSelection();
    if (!sel.isEmpty()) {
      FileOperationAutoApproveRule rule =
          (FileOperationAutoApproveRule) sel.getFirstElement();
      if (!rule.isDefault()) {
        userRules.remove(rule);
        rebuildAllRules();
        tableViewer.refresh();
        updateButtonState();
      }
    }
  }

  private void onToggle() {
    IStructuredSelection sel = tableViewer.getStructuredSelection();
    if (!sel.isEmpty()) {
      FileOperationAutoApproveRule rule =
          (FileOperationAutoApproveRule) sel.getFirstElement();
      if (!rule.isDefault()) {
        rule.setAutoApprove(!rule.isAutoApprove());
        tableViewer.refresh();
        updateButtonState();
      }
    }
  }

  private void onResetToDefaults() {
    boolean confirmed = MessageDialog.openQuestion(getShell(),
        Messages.preferences_page_file_op_auto_approve_reset_title,
        Messages.preferences_page_auto_approve_reset_message);
    if (confirmed) {
      userRules.clear();
      resetDefaultRulesToOriginal();
      rebuildAllRules();
      tableViewer.refresh();
      updateButtonState();
    }
  }

  /**
   * Resets default rules' autoApprove to their original values.
   * CLS defaults use {@code requiresConfirmation=true} → {@code autoApprove=false}.
   * Local fallback defaults are already {@code autoApprove=false}.
   */
  private void resetDefaultRulesToOriginal() {
    for (FileOperationAutoApproveRule rule : defaultRules) {
      rule.setAutoApprove(false);
    }
  }

  private void updateButtonState() {
    boolean hasSelection =
        !tableViewer.getStructuredSelection().isEmpty();
    FileOperationAutoApproveRule selected = hasSelection
        ? (FileOperationAutoApproveRule) tableViewer
            .getStructuredSelection().getFirstElement()
        : null;
    boolean isDefault = selected != null && selected.isDefault();

    removeButton.setEnabled(hasSelection && !isDefault);
    toggleButton.setEnabled(hasSelection && !isDefault);
    if (hasSelection) {
      toggleButton.setText(selected.isAutoApprove()
          ? Messages.preferences_page_auto_approve_deny
          : Messages.preferences_page_auto_approve_allow);
    } else {
      toggleButton.setText(
          Messages.preferences_page_auto_approve_allow);
    }
    resetButton.setEnabled(!isMatchingDefaults());
  }

  /**
   * Checks whether the current rule set matches defaults exactly
   * (no user rules, all defaults at original autoApprove values).
   */
  private boolean isMatchingDefaults() {
    if (!userRules.isEmpty()) {
      return false;
    }
    for (FileOperationAutoApproveRule rule : defaultRules) {
      if (rule.isAutoApprove()) {
        return false;
      }
    }
    return true;
  }

  /** Loads file-operation rules and unmatched-file-operation preference from the store. */
  public void loadFromPreferences(IPreferenceStore store) {
    List<FileOperationAutoApproveRule> savedRules = parseSavedRules(store);

    // Initialize with local fallback defaults
    applyFallbackDefaults();

    // Separate saved rules into defaults (override autoApprove) and user
    Set<String> defaultPatterns = defaultRules.stream()
        .map(FileOperationAutoApproveRule::getPattern)
        .collect(Collectors.toSet());
    userRules.clear();
    for (FileOperationAutoApproveRule saved : savedRules) {
      if (defaultPatterns.contains(saved.getPattern())) {
        // Restore toggled autoApprove for default rules
        defaultRules.stream()
            .filter(d -> d.getPattern().equals(saved.getPattern()))
            .findFirst()
            .ifPresent(d -> d.setAutoApprove(saved.isAutoApprove()));
      } else {
        userRules.add(saved);
      }
    }

    rebuildAllRules();
    tableViewer.setInput(allRules);

    unmatchedCheckbox.setSelection(
        store.getBoolean(Constants.AUTO_APPROVE_UNMATCHED_FILE_OP));
    updateButtonState();

    fetchDefaultRulesFromCls();
  }

  private List<FileOperationAutoApproveRule> parseSavedRules(
      IPreferenceStore store) {
    String json =
        store.getString(Constants.AUTO_APPROVE_FILE_OP_RULES);
    if (StringUtils.isNotBlank(json) && !"[]".equals(json.trim())) {
      try {
        List<FileOperationAutoApproveRule> loaded =
            new Gson().fromJson(json, FILE_OP_RULES_TYPE);
        if (loaded != null) {
          return loaded;
        }
      } catch (Exception e) {
        CopilotCore.LOGGER.error(
            "Failed to parse file operation auto-approve rules", e);
      }
    }
    return List.of();
  }

  private void applyFallbackDefaults() {
    defaultRules.clear();
    for (FileOperationAutoApproveRule fallback
        : FileOperationConfirmationHandler.FALLBACK_DEFAULT_RULES) {
      defaultRules.add(new FileOperationAutoApproveRule(
          fallback.getPattern(), fallback.getDescription(),
          fallback.isAutoApprove(), true));
    }
  }

  /**
   * Fetches default file safety rules from CLS asynchronously.
   * On success, merges CLS rules with local fallback and updates
   * the table. On failure, keeps local fallback defaults.
   */
  private void fetchDefaultRulesFromCls() {
    CopilotLanguageServerConnection conn =
        CopilotCore.getPlugin().getCopilotLanguageServer();
    if (conn == null) {
      return;
    }
    conn.getDefaultFileSafetyRules().thenAccept(result -> {
      if (result == null || result.getDefaultRules() == null) {
        return;
      }
      SwtUtils.invokeOnDisplayThreadAsync(() -> {
        if (isDisposed() || defaultRulesLoaded) {
          return;
        }
        applyClsDefaults(result.getDefaultRules());
      }, FileOperationAutoApproveSection.this);
    }).exceptionally(ex -> {
      // CLS not available — keep local fallback defaults
      CopilotCore.LOGGER.error(
          "Failed to fetch default file safety rules from CLS", ex);
      return null;
    });
  }

  /**
   * Applies CLS-provided default rules, merging with local fallback.
   * CLS rules take priority; local fallbacks fill gaps.
   */
  private void applyClsDefaults(List<FileSafetyRuleInfo> clsRules) {
    // Preserve any user-toggled autoApprove on existing defaults
    Map<String, Boolean> toggledDefaults = new HashMap<>();
    for (FileOperationAutoApproveRule existing : defaultRules) {
      toggledDefaults.put(existing.getPattern(), existing.isAutoApprove());
    }

    defaultRules.clear();
    Set<String> clsPatterns = new HashSet<>();
    for (FileSafetyRuleInfo clsRule : clsRules) {
      String pattern = clsRule.getPattern();
      clsPatterns.add(pattern);
      boolean autoApprove = !clsRule.isRequiresConfirmation();
      // Restore user toggle if they had changed this default
      if (toggledDefaults.containsKey(pattern)) {
        autoApprove = toggledDefaults.get(pattern);
      }
      String desc = clsRule.getDescription() != null
          ? clsRule.getDescription() : "";
      defaultRules.add(new FileOperationAutoApproveRule(
          pattern, desc, autoApprove, true));
    }

    // Add local fallbacks for patterns not in CLS
    for (FileOperationAutoApproveRule fallback
        : FileOperationConfirmationHandler.FALLBACK_DEFAULT_RULES) {
      if (!clsPatterns.contains(fallback.getPattern())) {
        boolean autoApprove = fallback.isAutoApprove();
        if (toggledDefaults.containsKey(fallback.getPattern())) {
          autoApprove = toggledDefaults.get(fallback.getPattern());
        }
        defaultRules.add(new FileOperationAutoApproveRule(
            fallback.getPattern(), fallback.getDescription(),
            autoApprove, true));
      }
    }

    // Remove user rules that overlap with new defaults
    Set<String> allDefaultPatterns = defaultRules.stream()
        .map(FileOperationAutoApproveRule::getPattern)
        .collect(Collectors.toSet());
    userRules.removeIf(r -> allDefaultPatterns.contains(r.getPattern()));

    defaultRulesLoaded = true;
    rebuildAllRules();
    tableViewer.refresh();
    updateButtonState();
  }

  /** Rebuilds the combined list shown in the table. */
  private void rebuildAllRules() {
    allRules.clear();
    allRules.addAll(defaultRules);
    allRules.addAll(userRules);
  }

  /** Saves file-operation rules and unmatched-file-operation preference to the store. */
  public void saveToPreferences(IPreferenceStore store) {
    // Save all rules (defaults + user) to preferences.
    // On next load, defaults are re-identified by pattern matching.
    store.setValue(Constants.AUTO_APPROVE_FILE_OP_RULES,
        new Gson().toJson(allRules));
    store.setValue(Constants.AUTO_APPROVE_UNMATCHED_FILE_OP,
        unmatchedCheckbox.getSelection());
  }
}
