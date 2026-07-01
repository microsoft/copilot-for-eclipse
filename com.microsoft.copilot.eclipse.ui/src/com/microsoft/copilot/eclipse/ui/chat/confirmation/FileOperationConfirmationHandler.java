// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import java.lang.reflect.Type;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationActionScope;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.chat.FileOperationAutoApproveRule;
import com.microsoft.copilot.eclipse.core.chat.service.IChatServiceManager;
import com.microsoft.copilot.eclipse.core.chat.service.ICustomizationFileService;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ToolMetadata;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.ui.chat.Messages;

/**
 * Evaluates file-operation confirmation requests against user-configured glob pattern rules.
 * Files outside the workspace always require confirmation.
 */
public class FileOperationConfirmationHandler implements ConfirmationHandler {

  /** Action types for edit confirmation. */
  public enum Action {
    /** Allow this specific file for the rest of the session. */
    ACCEPT_FILE_SESSION,
    /** Always allow this specific file (persisted globally). */
    ACCEPT_FILE_GLOBAL,
    /** Allow all files under this folder for the session. */
    ACCEPT_FOLDER_SESSION
  }

  /** File tool type matching CLS values. */
  enum FileToolType {
    FILE_READ("file_read"),
    FILE_WRITE("file_write"),
    FILE_OPERATION("file_operation");

    private final String value;

    FileToolType(String value) {
      this.value = value;
    }

    String getDefaultTitle() {
      switch (this) {
        case FILE_READ:
          return Messages.confirmation_title_fileRead;
        case FILE_WRITE:
          return Messages.confirmation_title_fileWrite;
        default:
          return Messages.confirmation_title_fileOperation;
      }
    }

    String getMessageTemplate() {
      switch (this) {
        case FILE_READ:
          return Messages.confirmation_message_fileRead;
        case FILE_WRITE:
          return Messages.confirmation_message_fileWrite;
        default:
          return Messages.confirmation_message_fileOperation;
      }
    }

    static FileToolType fromValue(String value) {
      if (value != null) {
        for (FileToolType t : values()) {
          if (t.value.equals(value)) {
            return t;
          }
        }
      }
      return FILE_OPERATION;
    }
  }

  static final String META_FILE_PATH = "filePath";
  static final String META_FOLDER_PATH = "folderPath";

  /**
   * Local fallback defaults matching CLS
   * {@code FileSafetyRulesService.defaultRules}.
   * Used when CLS is not yet available.
   */
  public static final List<FileOperationAutoApproveRule> FALLBACK_DEFAULT_RULES = List.of(
      new FileOperationAutoApproveRule("**/.github/instructions/*",
          "GitHub instructions files", false, true),
      new FileOperationAutoApproveRule("**/github-copilot/**/*",
          "GitHub Copilot settings and token files", false, true));

  private static final Type RULES_TYPE =
      new TypeToken<List<FileOperationAutoApproveRule>>() {
      }.getType();

  private final IPreferenceStore preferenceStore;
  private final AttachedFileRegistry attachedFileRegistry;
  private final Map<String, Set<String>> sessionApprovedFiles =
      Collections.synchronizedMap(new LinkedHashMap<>());
  private final Map<String, Set<String>> sessionApprovedFolders =
      Collections.synchronizedMap(new LinkedHashMap<>());

  /**
   * Creates a new FileOperationConfirmationHandler.
   *
   * @param preferenceStore the preference store for reading file-operation auto-approve rules
   * @param attachedFileRegistry registry of user-attached files for auto-approval
   */
  public FileOperationConfirmationHandler(IPreferenceStore preferenceStore,
      AttachedFileRegistry attachedFileRegistry) {
    this.preferenceStore = preferenceStore;
    this.attachedFileRegistry = attachedFileRegistry;
  }

  @Override
  public ConfirmationResult evaluate(InvokeClientToolConfirmationParams params,
      String sessionConversationId, boolean isAutoApprovalEnabled) {
    if (!isAutoApprovalEnabled) {
      return evaluateAutoApprovalDisabled(params, sessionConversationId);
    }
    return evaluateAutoApprovalEnabled(params, sessionConversationId);
  }

  private ConfirmationResult evaluateAutoApprovalEnabled(
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    String filePath = extractFilePath(params);
    if (StringUtils.isBlank(filePath)) {
      return ConfirmationResult.DISMISSED;
    }

    // Auto-approve files explicitly attached by the user in the context panel
    if (attachedFileRegistry.isAttachedFile(
        sessionConversationId, filePath)) {
      return ConfirmationResult.AUTO_APPROVED;
    }

    // Session override — file level
    String convId = sessionConversationId;
    Set<String> convFiles = sessionApprovedFiles.get(convId);
    if (convFiles != null && convFiles.contains(normalizePath(filePath))) {
      return ConfirmationResult.AUTO_APPROVED;
    }
    // Session override — folder level
    Set<String> convFolders = sessionApprovedFolders.get(convId);
    if (convFolders != null && filePath != null) {
      String normalizedPath = normalizePath(filePath);
      for (String folder : convFolders) {
        if (normalizedPath.startsWith(folder + "/")) {
          return ConfirmationResult.AUTO_APPROVED;
        }
      }
    }

    // Auto-approve reads of Copilot customization files (skills, instructions, prompts, agents)
    // discovered by the language server. Reading them is normal agent operation; edits still prompt.
    if (isFileRead(params)) {
      Path localPath = FileUtils.getLocalFilePath(filePath);
      ICustomizationFileService service = getCustomizationFileService();
      if (localPath != null && service != null && isCustomizationRead(
          localPath, service.getCustomizationFiles(), service.getSkillFolders())) {
        return ConfirmationResult.AUTO_APPROVED;
      }
    }

    // Files outside workspace always require confirmation
    if (isOutsideWorkspace(params)) {
      return ConfirmationResult.needsConfirmation(buildContent(params));
    }

    // Check against rules
    List<FileOperationAutoApproveRule> rules = loadRules();
    for (FileOperationAutoApproveRule rule : rules) {
      if (matchesGlob(filePath, rule.getPattern())) {
        return rule.isAutoApprove()
            ? ConfirmationResult.AUTO_APPROVED
            : ConfirmationResult.needsConfirmation(buildContent(params));
      }
    }

    return evaluateUnmatched(params);
  }

  private ConfirmationResult evaluateAutoApprovalDisabled(
      InvokeClientToolConfirmationParams params, String sessionConversationId) {
    String filePath = extractFilePath(params);
    if (StringUtils.isBlank(filePath)) {
      return ConfirmationResult.DISMISSED;
    }

    // Still honor files explicitly attached by the user (intentional context)
    if (attachedFileRegistry.isAttachedFile(sessionConversationId, filePath)) {
      return ConfirmationResult.AUTO_APPROVED;
    }

    // Outside workspace always requires confirmation (simplified dialog only)
    if (isOutsideWorkspace(params)) {
      return ConfirmationResult.needsConfirmation(buildSimplifiedContent(params));
    }

    // Only check default rules; ignore user-configured rules
    for (FileOperationAutoApproveRule rule : FALLBACK_DEFAULT_RULES) {
      if (matchesGlob(filePath, rule.getPattern())) {
        return rule.isAutoApprove()
            ? ConfirmationResult.AUTO_APPROVED
            : ConfirmationResult.needsConfirmation(buildSimplifiedContent(params));
      }
    }

    // Unmatched workspace file: auto-approve
    return ConfirmationResult.AUTO_APPROVED;
  }

  private ConfirmationResult evaluateUnmatched(
      InvokeClientToolConfirmationParams params) {
    if (preferenceStore.getBoolean(Constants.AUTO_APPROVE_UNMATCHED_FILE_OP)) {
      return ConfirmationResult.AUTO_APPROVED;
    }
    return ConfirmationResult.needsConfirmation(buildContent(params));
  }

  private ConfirmationContent buildContent(
      InvokeClientToolConfirmationParams params) {
    String filePath = extractFilePath(params);
    final FileToolType fileType =
        FileToolType.fromValue(ConfirmationHandler.extractToolType(params));
    String safeFilePath = filePath != null ? filePath : "";
    boolean outsideWorkspace = isOutsideWorkspace(params);

    List<ConfirmationAction> actions = new ArrayList<>();
    actions.add(ConfirmationAction.allowOnce(
        Messages.confirmation_action_allowOnce));

    if (outsideWorkspace && filePath != null) {
      // Outside workspace: offer folder-level approval
      Path parent = Path.of(filePath).getParent();
      String folderName = (parent != null && parent.getFileName() != null)
          ? parent.getFileName().toString()
          : (parent != null ? parent.toString() : filePath);
      String folderPath = parent != null ? parent.toString() : "";
      actions.add(action(Action.ACCEPT_FOLDER_SESSION,
          NLS.bind(Messages.confirmation_action_allowFolderSession,
              folderName),
          ConfirmationActionScope.SESSION,
          Map.of(META_FOLDER_PATH, folderPath)));
    } else {
      // In workspace: offer file-level approval
      actions.add(action(Action.ACCEPT_FILE_SESSION,
          Messages.confirmation_action_allowFileSession,
          ConfirmationActionScope.SESSION,
          Map.of(META_FILE_PATH, safeFilePath)));
      actions.add(action(Action.ACCEPT_FILE_GLOBAL,
          Messages.confirmation_action_alwaysAllow,
          ConfirmationActionScope.GLOBAL,
          Map.of(META_FILE_PATH, safeFilePath)));
    }
    actions.add(ConfirmationAction.skip(
        Messages.confirmation_action_skip));

    String title = params.getTitle() != null
        ? params.getTitle() : fileType.getDefaultTitle();
    String fileName = "";
    try {
      if (filePath != null) {
        fileName = Path.of(filePath).getFileName().toString();
      }
    } catch (Exception ignored) {
      // use empty
    }
    String message = NLS.bind(fileType.getMessageTemplate(), fileName);
    return new ConfirmationContent(title, message, actions);
  }

  /**
   * Builds a simplified confirmation dialog with only Allow Once and Skip actions.
   * Used when the auto-approval feature is disabled by token/policy.
   */
  private ConfirmationContent buildSimplifiedContent(
      InvokeClientToolConfirmationParams params) {
    String filePath = extractFilePath(params);
    final FileToolType fileType =
        FileToolType.fromValue(ConfirmationHandler.extractToolType(params));
    String fileName = "";
    try {
      if (filePath != null) {
        fileName = Path.of(filePath).getFileName().toString();
      }
    } catch (Exception ignored) {
      // use empty
    }
    String title = params.getTitle() != null ? params.getTitle() : fileType.getDefaultTitle();
    String message = NLS.bind(fileType.getMessageTemplate(), fileName);
    return new ConfirmationContent(title, message,
        List.of(
            ConfirmationAction.allowOnce(Messages.confirmation_action_allowOnce),
            ConfirmationAction.skip(Messages.confirmation_action_skip)));
  }

  private static ConfirmationAction action(Action type, String label,
      ConfirmationActionScope scope, Map<String, String> extra) {
    Map<String, String> meta = new java.util.HashMap<>(extra);
    meta.put(ConfirmationAction.META_ACTION, type.name());
    return new ConfirmationAction(label, true, scope, meta, false);
  }

  private String extractFilePath(InvokeClientToolConfirmationParams params) {
    // Try toolMetadata first
    ToolMetadata metadata = params.getToolMetadata();
    if (metadata != null && metadata.getSensitiveFileData() != null) {
      return metadata.getSensitiveFileData().getFilePath();
    }
    // Fallback: extract from input map
    Object input = params.getInput();
    if (input instanceof Map) {
      Object path = ((Map<?, ?>) input).get("filePath");
      if (path == null) {
        path = ((Map<?, ?>) input).get("path");
      }
      return path instanceof String ? (String) path : null;
    }
    return null;
  }

  // Uses CLS-provided isGlobal flag from sensitiveFileData metadata
  private boolean isOutsideWorkspace(InvokeClientToolConfirmationParams params) {
    ToolMetadata metadata = params.getToolMetadata();
    if (metadata != null && metadata.getSensitiveFileData() != null) {
      return metadata.getSensitiveFileData().isGlobal();
    }
    return false;
  }

  private static boolean isFileRead(InvokeClientToolConfirmationParams params) {
    return FileToolType.fromValue(ConfirmationHandler.extractToolType(params))
        == FileToolType.FILE_READ;
  }

  private static ICustomizationFileService getCustomizationFileService() {
    IChatServiceManager chatServiceManager = CopilotCore.getPlugin().getChatServiceManager();
    return chatServiceManager == null ? null : chatServiceManager.getCustomizationFileService();
  }

  /**
   * Returns whether reading {@code file} is a customization-file read that is safe to auto-approve.
   * A read matches when the file exactly equals a language-server-reported customization file, or
   * sits inside a reported skill folder (covering {@code SKILL.md} and the helper files it uses).
   *
   * @param file the absolute path being read
   * @param customizationFiles reported single-file customizations (prompts, instructions, agents)
   * @param skillFolders reported skill folders (each directory containing a {@code SKILL.md})
   * @return {@code true} when the read is a recognized customization read
   */
  public static boolean isCustomizationRead(Path file, Set<Path> customizationFiles, Set<Path> skillFolders) {
    Path normalized = file.toAbsolutePath().normalize();
    if (customizationFiles.contains(normalized)) {
      return true;
    }
    for (Path skillFolder : skillFolders) {
      if (normalized.startsWith(skillFolder)) {
        return true;
      }
    }
    return false;
  }

  /** Normalizes a file path for case-insensitive, separator-agnostic comparison. */
  private static String normalizePath(String path) {
    return ConfirmationHandler.normalizePath(path);
  }

  static boolean matchesGlob(String filePath, String globPattern) {
    if (StringUtils.isBlank(filePath) || StringUtils.isBlank(globPattern)) {
      return false;
    }
    try {
      // Normalize both path and pattern to forward slashes for consistent matching
      String normalizedPath = filePath.replace('\\', '/');
      String normalizedPattern = globPattern.replace('\\', '/');

      // Fast exact-match for absolute file path rules (e.g., from "Always Allow")
      if (normalizedPath.equalsIgnoreCase(normalizedPattern)) {
        return true;
      }

      PathMatcher matcher = FileSystems.getDefault()
          .getPathMatcher("glob:" + normalizedPattern);
      return matcher.matches(Path.of(normalizedPath));
    } catch (Exception e) {
      CopilotCore.LOGGER.error(
          "Invalid file-operation auto-approve glob: " + globPattern, e);
      return false;
    }
  }

  List<FileOperationAutoApproveRule> loadRules() {
    String json =
        preferenceStore.getString(Constants.AUTO_APPROVE_FILE_OP_RULES);
    if (StringUtils.isBlank(json) || "[]".equals(json.trim())) {
      return Collections.emptyList();
    }
    try {
      List<FileOperationAutoApproveRule> rules =
          new Gson().fromJson(json, RULES_TYPE);
      return rules != null ? rules : Collections.emptyList();
    } catch (Exception e) {
      CopilotCore.LOGGER.error(
          "Failed to parse file-operation auto-approve rules", e);
      return Collections.emptyList();
    }
  }

  @Override
  public void cacheDecision(ConfirmationAction action,
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    String actionName = action.getMetadata()
        .get(ConfirmationAction.META_ACTION);
    if (actionName == null) {
      return;
    }
    Action type;
    try {
      type = Action.valueOf(actionName);
    } catch (IllegalArgumentException e) {
      return;
    }

    String convId = sessionConversationId;
    Map<String, String> meta = action.getMetadata();
    switch (type) {
      case ACCEPT_FILE_SESSION:
        String fp = meta.getOrDefault(META_FILE_PATH, "");
        if (!fp.isEmpty()) {
          ConfirmationHandler.evictOldestIfNeeded(sessionApprovedFiles);
          sessionApprovedFiles.computeIfAbsent(
              convId, k -> ConcurrentHashMap.newKeySet())
              .add(normalizePath(fp));
        }
        break;
      case ACCEPT_FOLDER_SESSION:
        String folder = meta.getOrDefault(META_FOLDER_PATH, "");
        if (!folder.isEmpty()) {
          ConfirmationHandler.evictOldestIfNeeded(sessionApprovedFolders);
          sessionApprovedFolders.computeIfAbsent(
              convId, k -> ConcurrentHashMap.newKeySet())
              .add(normalizePath(folder));
        }
        break;
      case ACCEPT_FILE_GLOBAL:
        String globalFp = meta.getOrDefault(META_FILE_PATH, "");
        if (!globalFp.isEmpty()) {
          List<FileOperationAutoApproveRule> rules =
              new ArrayList<>(loadRules());
          // Update existing rule if path matches (case-insensitive for Windows)
          boolean found = false;
          for (FileOperationAutoApproveRule r : rules) {
            if (r.getPattern().equalsIgnoreCase(globalFp)) {
              r.setAutoApprove(true);
              found = true;
              break;
            }
          }
          if (!found) {
            rules.add(new FileOperationAutoApproveRule(globalFp,
                Messages.confirmation_autoApprovedDescription, true));
          }
          preferenceStore.setValue(Constants.AUTO_APPROVE_FILE_OP_RULES,
              new Gson().toJson(rules));
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void clearSession(String conversationId) {
    sessionApprovedFiles.remove(conversationId);
    sessionApprovedFolders.remove(conversationId);
    attachedFileRegistry.clearConversation(conversationId);
  }
}
