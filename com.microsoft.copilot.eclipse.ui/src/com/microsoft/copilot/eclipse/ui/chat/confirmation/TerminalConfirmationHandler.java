// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

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
import com.microsoft.copilot.eclipse.core.chat.TerminalAutoApproveRule;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ToolMetadata;
import com.microsoft.copilot.eclipse.ui.chat.Messages;

/**
 * Evaluates terminal command confirmation requests against user-configured allow/deny rules.
 * Rules are matched against command names provided by CLS in toolMetadata.terminalCommandData.
 */
public class TerminalConfirmationHandler implements ConfirmationHandler {

  /** Action types matching IntelliJ's TerminalAutoApproveAction enum. */
  public enum Action {
    /** Allow specific command names for the current session/conversation. */
    ACCEPT_NAMES_SESSION,
    /** Always allow specific command names (persisted as a global rule). */
    ACCEPT_NAMES_GLOBAL,
    /** Allow this exact command line for the current session/conversation. */
    ACCEPT_EXACT_SESSION,
    /** Always allow this exact command line (persisted as a global rule). */
    ACCEPT_EXACT_GLOBAL,
    /** Allow all terminal commands for the current session/conversation. */
    ACCEPT_ALL_SESSION
  }

  /** Result of evaluating sub-commands against rules. */
  enum RuleVerdict { ALL_APPROVED, DENY_BLOCKED, UNMATCHED }

  private static final class RuleResult {
    final RuleVerdict verdict;
    final List<String> unapprovedItems;

    RuleResult(RuleVerdict verdict, List<String> unapprovedItems) {
      this.verdict = verdict;
      this.unapprovedItems = unapprovedItems;
    }
  }

  static final String META_COMMAND_NAMES = "commandNames";
  static final String META_COMMAND_LINE = "commandLine";

  /** Default deny rules for dangerous terminal commands. */
  public static final List<TerminalAutoApproveRule> DEFAULT_RULES = List.of(
      new TerminalAutoApproveRule("rm", false),
      new TerminalAutoApproveRule("rmdir", false),
      new TerminalAutoApproveRule("del", false),
      new TerminalAutoApproveRule("kill", false),
      new TerminalAutoApproveRule("curl", false),
      new TerminalAutoApproveRule("wget", false),
      new TerminalAutoApproveRule("eval", false),
      new TerminalAutoApproveRule("chmod", false),
      new TerminalAutoApproveRule("chown", false),
      new TerminalAutoApproveRule("/^Remove-Item\\b/i", false),
      new TerminalAutoApproveRule("/(\\(.+\\))/s", false),
      new TerminalAutoApproveRule("/`.+`/s", false),
      new TerminalAutoApproveRule("/\\{.+\\}/s", false));

  private static final Type RULES_TYPE = new TypeToken<List<TerminalAutoApproveRule>>() {
  }.getType();

  private final IPreferenceStore preferenceStore;

  // Session-scoped in-memory storage keyed by conversationId.
  // Uses insertion-ordered maps so we can evict the oldest entry when the
  // map grows beyond MAX_SESSION_CONVERSATIONS.
  private final Map<String, Set<String>> allowedCommandNames =
      Collections.synchronizedMap(new LinkedHashMap<>());
  private final Map<String, Set<String>> allowedExactCommands =
      Collections.synchronizedMap(new LinkedHashMap<>());
  private final Set<String> allowAllConversations =
      Collections.newSetFromMap(
          Collections.synchronizedMap(new LinkedHashMap<>()));

  /**
   * Creates a new TerminalConfirmationHandler.
   *
   * @param preferenceStore the preference store for reading terminal auto-approve rules
   */
  public TerminalConfirmationHandler(IPreferenceStore preferenceStore) {
    this.preferenceStore = preferenceStore;
  }

  /**
   * When the auto-approval feature is disabled, terminal commands always prompt
   * with Allow Once / Skip only — no session or global approval buttons.
   * This matches IntelliJ's behavior where terminal ignores all rules when disabled.
   */
  @Override
  public ConfirmationResult evaluate(InvokeClientToolConfirmationParams params,
      String sessionConversationId, boolean isAutoApprovalEnabled) {
    if (!isAutoApprovalEnabled) {
      return evaluateAutoApprovalDisabled(params);
    }
    return evaluateAutoApprovalEnabled(params, sessionConversationId);
  }

  /**
   * Evaluates a terminal confirmation request. Check order follows IntelliJ:
   * 1. Session "allow all" flag
   * 2. Session exact commandLine match
   * 3. Session command name match (all names must be approved)
   * 4. Global exact commandLine match against rules
   * 5. Global per-subCommand regex/prefix match against rules
   * 6. Unmatched fallback (auto-approve if preference enabled)
   */
  private ConfirmationResult evaluateAutoApprovalEnabled(
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    String convId = sessionConversationId;
    String commandLine = extractCommandLine(params);

    // 1. Session: all commands allowed for this conversation
    if (allowAllConversations.contains(convId)) {
      return ConfirmationResult.AUTO_APPROVED;
    }

    // 2. Session: exact commandLine previously approved
    Set<String> exactSet = allowedExactCommands.get(convId);
    if (commandLine != null && exactSet != null
        && exactSet.contains(commandLine.trim())) {
      return ConfirmationResult.AUTO_APPROVED;
    }

    // 3. Session: all command names (e.g. "tree", "echo") approved
    String[] cmdNames = getCommandNames(params);
    Set<String> namesSet = allowedCommandNames.get(convId);
    if (cmdNames != null && namesSet != null && cmdNames.length > 0) {
      boolean allApproved = true;
      for (String name : cmdNames) {
        if (!namesSet.contains(name)) {
          allApproved = false;
          break;
        }
      }
      if (allApproved) {
        return ConfirmationResult.AUTO_APPROVED;
      }
    }

    // 4-6. Global rules
    String[] subCommands = getSubCommands(params);
    if (subCommands == null || subCommands.length == 0) {
      return ConfirmationResult.needsConfirmation(
          buildContent(params, null));
    }

    List<TerminalAutoApproveRule> rules = loadRules();

    // 4. Exact commandLine match
    if (commandLine != null) {
      for (TerminalAutoApproveRule rule : rules) {
        if (commandLine.trim().equals(rule.getCommand().trim())
            && rule.isAutoApprove()) {
          return ConfirmationResult.AUTO_APPROVED;
        }
      }
    }

    // 5. Per-subCommand evaluation
    RuleResult result = evaluateSubCommands(
        subCommands, cmdNames, rules, namesSet);

    switch (result.verdict) {
      case ALL_APPROVED:
        return ConfirmationResult.AUTO_APPROVED;
      case DENY_BLOCKED:
        return ConfirmationResult.needsConfirmation(
            buildContent(params, result.unapprovedItems));
      case UNMATCHED:
      default:
        // 6. Unmatched fallback
        if (preferenceStore.getBoolean(
            Constants.AUTO_APPROVE_UNMATCHED_TERMINAL)) {
          return ConfirmationResult.AUTO_APPROVED;
        }
        List<String> items = result.unapprovedItems.isEmpty()
            ? null : result.unapprovedItems;
        return ConfirmationResult.needsConfirmation(
            buildContent(params, items));
    }
  }

  private ConfirmationResult evaluateAutoApprovalDisabled(
      InvokeClientToolConfirmationParams params) {
    String title = params.getTitle() != null
        ? params.getTitle() : Messages.confirmation_title_terminal;
    return ConfirmationResult.needsConfirmation(
        new ConfirmationContent(title, params.getMessage(),
            List.of(
                ConfirmationAction.allowOnce(Messages.confirmation_action_allowOnce),
                ConfirmationAction.skip(Messages.confirmation_action_skip))));
  }

  /**
   * Evaluates each sub-command against global rules and session state.
   * Returns a verdict and the list of unapproved command names.
   */
  private RuleResult evaluateSubCommands(String[] subCommands,
      String[] cmdNames, List<TerminalAutoApproveRule> rules,
      Set<String> sessionApprovedNames) {
    boolean allApproved = true;
    boolean hasDeny = false;
    List<String> unapproved = new ArrayList<>();

    for (int i = 0; i < subCommands.length; i++) {
      String subCommand = subCommands[i];
      String cmdName = cmdNames != null && i < cmdNames.length
          ? cmdNames[i] : null;
      boolean hasAllow = false;
      boolean denied = false;

      // Session command-name approval
      if (cmdName != null && sessionApprovedNames != null
          && sessionApprovedNames.contains(cmdName)) {
        hasAllow = true;
      }

      // Global rule matching
      for (TerminalAutoApproveRule rule : rules) {
        if (matchesRule(subCommand, rule.getCommand())) {
          if (rule.isAutoApprove()) {
            hasAllow = true;
          } else {
            denied = true;
          }
          break;
        }
      }

      if (denied && !hasAllow) {
        hasDeny = true;
        if (cmdName != null && !unapproved.contains(cmdName)) {
          unapproved.add(cmdName);
        }
      } else if (!hasAllow) {
        allApproved = false;
        if (cmdName != null && !unapproved.contains(cmdName)) {
          unapproved.add(cmdName);
        }
      }
    }

    RuleVerdict verdict;
    if (hasDeny) {
      verdict = RuleVerdict.DENY_BLOCKED;
    } else if (allApproved) {
      verdict = RuleVerdict.ALL_APPROVED;
    } else {
      verdict = RuleVerdict.UNMATCHED;
    }
    return new RuleResult(verdict, unapproved);
  }

  /**
   * Builds the confirmation dialog content. When {@code unapprovedNames}
   * is non-null, only those names appear in the command-name actions
   * (already-approved names are filtered out).
   */
  private ConfirmationContent buildContent(
      InvokeClientToolConfirmationParams params,
      List<String> unapprovedNames) {
    final String[] commandNames = getCommandNames(params);
    String commandLine = extractCommandLine(params);

    // Use unapproved names if available, otherwise all unique names
    List<String> uniqueNames = unapprovedNames != null
        ? unapprovedNames : dedup(commandNames);
    String label = !uniqueNames.isEmpty()
        ? "'" + String.join(", ", uniqueNames) + "'" : "'command'";
    String namesValue = String.join(",", uniqueNames);

    // Show exact command actions when commandLine differs from
    // a single command name (otherwise redundant).
    boolean showExact = commandLine != null
        && !(uniqueNames.size() == 1
            && commandLine.trim().equals(uniqueNames.get(0)));

    List<ConfirmationAction> actions = new ArrayList<>();
    actions.add(ConfirmationAction.allowOnce(Messages.confirmation_action_allowOnce));
    if (!uniqueNames.isEmpty()) {
      actions.add(action(Action.ACCEPT_NAMES_SESSION,
          NLS.bind(Messages.confirmation_action_allowNamesSession, label),
          ConfirmationActionScope.SESSION,
          Map.of(META_COMMAND_NAMES, namesValue)));
      actions.add(action(Action.ACCEPT_NAMES_GLOBAL,
          NLS.bind(Messages.confirmation_action_alwaysAllowNames, label),
          ConfirmationActionScope.GLOBAL,
          Map.of(META_COMMAND_NAMES, namesValue)));
    }
    if (showExact) {
      actions.add(action(Action.ACCEPT_EXACT_SESSION,
          Messages.confirmation_action_allowExactSession,
          ConfirmationActionScope.SESSION,
          Map.of(META_COMMAND_LINE, commandLine)));
      actions.add(action(Action.ACCEPT_EXACT_GLOBAL,
          Messages.confirmation_action_alwaysAllowExact,
          ConfirmationActionScope.GLOBAL,
          Map.of(META_COMMAND_LINE, commandLine)));
    }
    actions.add(action(Action.ACCEPT_ALL_SESSION,
        Messages.confirmation_action_allowAllCommands,
        ConfirmationActionScope.SESSION, Map.of()));
    actions.add(ConfirmationAction.skip(Messages.confirmation_action_skip));

    String title = params.getTitle() != null
        ? params.getTitle() : Messages.confirmation_title_terminal;
    return new ConfirmationContent(title, params.getMessage(), actions);
  }

  private static ConfirmationAction action(Action type, String label,
      ConfirmationActionScope scope, Map<String, String> extra) {
    Map<String, String> meta = new HashMap<>(extra);
    meta.put(ConfirmationAction.META_ACTION, type.name());
    return new ConfirmationAction(label, true, scope, meta, false);
  }

  private String extractCommandLine(
      InvokeClientToolConfirmationParams params) {
    Object input = params.getInput();
    if (input instanceof Map<?, ?> inputMap) {
      Object cmd = inputMap.get("command");
      if (cmd instanceof String) {
        return (String) cmd;
      }
    }
    return null;
  }

  private static List<String> dedup(String[] items) {
    if (items == null || items.length == 0) {
      return Collections.emptyList();
    }
    LinkedHashSet<String> set = new LinkedHashSet<>();
    for (String item : items) {
      if (item != null && !item.isBlank()) {
        set.add(item);
      }
    }
    return new ArrayList<>(set);
  }

  private String[] getSubCommands(InvokeClientToolConfirmationParams params) {
    ToolMetadata metadata = params.getToolMetadata();
    if (metadata != null && metadata.getTerminalCommandData() != null) {
      return metadata.getTerminalCommandData().getSubCommands();
    }
    return null;
  }

  private String[] getCommandNames(InvokeClientToolConfirmationParams params) {
    ToolMetadata metadata = params.getToolMetadata();
    if (metadata != null && metadata.getTerminalCommandData() != null) {
      return metadata.getTerminalCommandData().getCommandNames();
    }
    return null;
  }

  /**
   * Matches a sub-command against a rule. Exact string match is checked
   * first (for exact-command rules). Then regex rules (/pattern/flags) are
   * used directly, and simple rules (e.g., "rm") are converted to ^rm\b.
   */
  static boolean matchesRule(String subCommand, String rulePattern) {
    if (StringUtils.isBlank(subCommand)
        || StringUtils.isBlank(rulePattern)) {
      return false;
    }

    // Exact match first
    if (subCommand.trim().equals(rulePattern.trim())) {
      return true;
    }

    String regex;
    int regexFlags = 0;

    if (rulePattern.startsWith("/")
        && rulePattern.lastIndexOf('/') > 0) {
      // Explicit regex: "/^git\b/i"
      int lastSlash = rulePattern.lastIndexOf('/');
      regex = rulePattern.substring(1, lastSlash);
      String flags = rulePattern.substring(lastSlash + 1);
      if (flags.contains("i")) {
        regexFlags |= Pattern.CASE_INSENSITIVE;
      }
      if (flags.contains("s")) {
        regexFlags |= Pattern.DOTALL;
      }
    } else {
      // Simple rule: "rm" → "^rm\b"
      regex = "^" + Pattern.quote(rulePattern) + "\\b";
    }

    try {
      return Pattern.compile(regex, regexFlags)
          .matcher(subCommand).find();
    } catch (PatternSyntaxException e) {
      CopilotCore.LOGGER.error(
          "Invalid terminal auto-approve regex: " + rulePattern, e);
      return false;
    }
  }

  List<TerminalAutoApproveRule> loadRules() {
    String json = preferenceStore.getString(Constants.AUTO_APPROVE_TERMINAL_RULES);
    if (StringUtils.isBlank(json) || "[]".equals(json.trim())) {
      return Collections.emptyList();
    }
    try {
      List<TerminalAutoApproveRule> rules = new Gson().fromJson(json, RULES_TYPE);
      return rules != null ? rules : Collections.emptyList();
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to parse terminal auto-approve rules", e);
      return Collections.emptyList();
    }
  }

  @Override
  public void cacheDecision(ConfirmationAction confirmAction,
      InvokeClientToolConfirmationParams params,
      String sessionConversationId) {
    String actionName = confirmAction.getMetadata()
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

    // Prefer command data from action metadata (set by buildContent)
    // so we persist exactly what the user chose, not the full params.
    Map<String, String> meta = confirmAction.getMetadata();
    String metaNames = meta.get(META_COMMAND_NAMES);
    String metaLine = meta.get(META_COMMAND_LINE);

    String[] cmdNames = metaNames != null && !metaNames.isBlank()
        ? metaNames.split(",") : getCommandNames(params);
    String commandLine = metaLine != null && !metaLine.isBlank()
        ? metaLine : extractCommandLine(params);

    switch (type) {
      case ACCEPT_NAMES_SESSION:
        if (cmdNames != null) {
          ConfirmationHandler.evictOldestIfNeeded(allowedCommandNames);
          Set<String> nameSet = allowedCommandNames.computeIfAbsent(
              convId, k -> ConcurrentHashMap.newKeySet());
          Collections.addAll(nameSet, cmdNames);
        }
        break;
      case ACCEPT_EXACT_SESSION:
        if (commandLine != null && !commandLine.isBlank()) {
          ConfirmationHandler.evictOldestIfNeeded(allowedExactCommands);
          allowedExactCommands.computeIfAbsent(
              convId, k -> ConcurrentHashMap.newKeySet())
              .add(commandLine.trim());
        }
        break;
      case ACCEPT_ALL_SESSION:
        allowAllConversations.add(convId);
        // Cap allowAllConversations the same way
        while (allowAllConversations.size() > MAX_SESSION_CONVERSATIONS) {
          allowAllConversations.iterator().remove();
        }
        break;
      case ACCEPT_NAMES_GLOBAL:
        if (cmdNames != null) {
          addGlobalRules(List.of(cmdNames));
        }
        break;
      case ACCEPT_EXACT_GLOBAL:
        if (commandLine != null && !commandLine.isBlank()) {
          addGlobalRules(List.of(commandLine.trim()));
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void clearSession(String conversationId) {
    allowedCommandNames.remove(conversationId);
    allowedExactCommands.remove(conversationId);
    allowAllConversations.remove(conversationId);
  }

  private void addGlobalRules(List<String> commands) {
    List<TerminalAutoApproveRule> original = loadRules();
    Set<String> existing = original.stream()
        .map(TerminalAutoApproveRule::getCommand)
        .collect(Collectors.toSet());

    // Override existing deny rules → allow
    List<TerminalAutoApproveRule> updated = original.stream()
        .map(r -> commands.contains(r.getCommand()) && !r.isAutoApprove()
            ? new TerminalAutoApproveRule(r.getCommand(), true) : r)
        .collect(Collectors.toCollection(ArrayList::new));

    // Append new rules for commands not yet present
    commands.stream()
        .filter(cmd -> !existing.contains(cmd))
        .map(cmd -> new TerminalAutoApproveRule(cmd, true))
        .forEach(updated::add);

    if (!updated.equals(original)) {
      preferenceStore.setValue(Constants.AUTO_APPROVE_TERMINAL_RULES,
          new Gson().toJson(updated));
    }
  }
}
