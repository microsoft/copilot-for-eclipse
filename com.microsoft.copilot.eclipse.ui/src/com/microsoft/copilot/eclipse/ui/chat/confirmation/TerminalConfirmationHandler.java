// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    ACCEPT_NAMES_SESSION,
    ACCEPT_NAMES_GLOBAL,
    ACCEPT_EXACT_SESSION,
    ACCEPT_EXACT_GLOBAL,
    ACCEPT_ALL_SESSION
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

  // Session-scoped in-memory storage keyed by conversationId
  private final ConcurrentHashMap<String, Set<String>> allowedCommandNames =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Set<String>> allowedExactCommands =
      new ConcurrentHashMap<>();
  private final Set<String> allowAllConversations = ConcurrentHashMap.newKeySet();

  /**
   * Creates a new TerminalConfirmationHandler.
   *
   * @param preferenceStore the preference store for reading terminal auto-approve rules
   */
  public TerminalConfirmationHandler(IPreferenceStore preferenceStore) {
    this.preferenceStore = preferenceStore;
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
  @Override
  public ConfirmationResult evaluate(
      InvokeClientToolConfirmationParams params) {
    String convId = params.getConversationId();
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

    // 4-6. Global rules from preference store
    String[] subCommands = getSubCommands(params);
    if (subCommands == null || subCommands.length == 0) {
      return ConfirmationResult.needsConfirmation(buildContent(params));
    }

    List<TerminalAutoApproveRule> rules = loadRules();
    if (rules.isEmpty()) {
      return evaluateUnmatched(params);
    }

    // 4. Global: exact commandLine match (for "Always Allow this
    //    exact command" — the full commandLine is stored as a rule)
    if (commandLine != null) {
      for (TerminalAutoApproveRule rule : rules) {
        if (commandLine.trim().equals(rule.getCommand().trim())
            && rule.isAutoApprove()) {
          return ConfirmationResult.AUTO_APPROVED;
        }
      }
    }

    // 5. Global: per-subCommand match against regex/prefix rules.
    //    Any deny match → needs confirmation immediately.
    //    All subCommands must match an allow rule for auto-approve.
    boolean allMatched = true;
    for (String subCommand : subCommands) {
      boolean matched = false;
      for (TerminalAutoApproveRule rule : rules) {
        if (matchesRule(subCommand, rule.getCommand())) {
          if (!rule.isAutoApprove()) {
            return ConfirmationResult.needsConfirmation(buildContent(params));
          }
          matched = true;
          break;
        }
      }
      if (!matched) {
        allMatched = false;
      }
    }

    if (allMatched) {
      return ConfirmationResult.AUTO_APPROVED;
    }
    // 6. No rule matched — defer to unmatched preference
    return evaluateUnmatched(params);
  }

  private ConfirmationResult evaluateUnmatched(
      InvokeClientToolConfirmationParams params) {
    if (preferenceStore.getBoolean(Constants.AUTO_APPROVE_UNMATCHED_TERMINAL)) {
      return ConfirmationResult.AUTO_APPROVED;
    }
    return ConfirmationResult.needsConfirmation(buildContent(params));
  }

  private ConfirmationContent buildContent(
      InvokeClientToolConfirmationParams params) {
    final String[] commandNames = getCommandNames(params);
    String commandLine = extractCommandLine(params);

    List<String> uniqueNames = dedup(commandNames);
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
    Map<String, String> meta = new java.util.HashMap<>(extra);
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
  public void persistDecision(ConfirmationAction confirmAction,
      InvokeClientToolConfirmationParams params) {
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

    String convId = params.getConversationId();
    String[] cmdNames = getCommandNames(params);
    String commandLine = extractCommandLine(params);

    switch (type) {
      case ACCEPT_NAMES_SESSION:
        if (cmdNames != null) {
          Set<String> nameSet = allowedCommandNames.computeIfAbsent(
              convId, k -> ConcurrentHashMap.newKeySet());
          Collections.addAll(nameSet, cmdNames);
        }
        break;
      case ACCEPT_EXACT_SESSION:
        if (commandLine != null && !commandLine.isBlank()) {
          allowedExactCommands.computeIfAbsent(
              convId, k -> ConcurrentHashMap.newKeySet())
              .add(commandLine.trim());
        }
        break;
      case ACCEPT_ALL_SESSION:
        allowAllConversations.add(convId);
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
    List<TerminalAutoApproveRule> rules = new ArrayList<>(loadRules());
    boolean changed = false;
    for (String cmd : commands) {
      if (rules.stream().noneMatch(r -> r.getCommand().equals(cmd))) {
        rules.add(new TerminalAutoApproveRule(cmd, true));
        changed = true;
      }
    }
    if (changed) {
      preferenceStore.setValue(Constants.AUTO_APPROVE_TERMINAL_RULES,
          new Gson().toJson(rules));
    }
  }
}
