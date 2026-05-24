// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationActionScope;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.chat.TerminalAutoApproveRule;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ToolMetadata;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ToolMetadata.TerminalCommandData;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TerminalConfirmationHandlerTests {

  private static final String CONV_ID = "conv-1";
  private static final Gson GSON = new Gson();

  @Mock
  private IPreferenceStore preferenceStore;

  private TerminalConfirmationHandler handler;

  @BeforeEach
  void setUp() {
    handler = new TerminalConfirmationHandler(preferenceStore);
  }

  // --- rule matching (tested through evaluate) ---

  @Test
  void ruleMatching_simpleRuleMatchesCommandAtStart() {
    stubRules(List.of(new TerminalAutoApproveRule("rm", true)));
    stubUnmatched(false);
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"rm -rf /tmp"}, new String[]{"rm"},
            "rm -rf /tmp");
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void ruleMatching_simpleRuleDoesNotMatchMiddleOfWord() {
    stubRules(List.of(new TerminalAutoApproveRule("rm", true)));
    stubUnmatched(false);
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"remove something"},
            new String[]{"remove"}, "remove something");
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void ruleMatching_regexCaseInsensitive() {
    stubRules(List.of(new TerminalAutoApproveRule("/^git\\b/i", true)));
    stubUnmatched(false);
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"Git status"}, new String[]{"Git"},
            "Git status");
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void ruleMatching_regexDotallMatchesSubshell() {
    stubRules(List.of(
        new TerminalAutoApproveRule("/(\\(.+\\))/s", true)));
    stubUnmatched(false);
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"(echo hello)"},
            new String[]{"(echo"}, "(echo hello)");
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void ruleMatching_noMatchWhenSubCommandsNull() {
    stubRules(List.of(new TerminalAutoApproveRule("rm", true)));
    stubUnmatched(false);
    InvokeClientToolConfirmationParams params =
        buildParams(null, null, "rm -rf");
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void ruleMatching_noMatchWhenSubCommandsEmpty() {
    stubRules(List.of(new TerminalAutoApproveRule("rm", true)));
    stubUnmatched(false);
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{}, new String[]{}, "rm -rf");
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void ruleMatching_noMatchWhenSubCommandBlank() {
    stubRules(List.of(new TerminalAutoApproveRule("rm", true)));
    stubUnmatched(false);
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"  "}, new String[]{"  "}, "  ");
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  // --- evaluate ---

  @Test
  void evaluate_autoApprovedWhenAllSubCommandsMatchAllowRules() {
    stubRules(List.of(new TerminalAutoApproveRule("echo", true)));
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo hello"}, new String[]{"echo"},
            "echo hello");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  @Test
  void evaluate_needsConfirmationWhenDenyRuleMatches() {
    stubRules(List.of(new TerminalAutoApproveRule("rm", false)));
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"rm -rf /"}, new String[]{"rm"},
            "rm -rf /");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
    assertNotNull(result.getContent());
  }

  @Test
  void evaluate_needsConfirmationWhenNoRulesMatchAndUnmatchedFalse() {
    stubRules(List.of(new TerminalAutoApproveRule("echo", true)));
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"ls -la"}, new String[]{"ls"},
            "ls -la");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
  }

  @Test
  void evaluate_autoApprovedWhenNoRulesMatchAndUnmatchedTrue() {
    stubRules(List.of(new TerminalAutoApproveRule("echo", true)));
    stubUnmatched(true);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"ls -la"}, new String[]{"ls"},
            "ls -la");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  @Test
  void evaluate_needsConfirmationWhenSubCommandsNull() {
    stubRules(List.of());

    InvokeClientToolConfirmationParams params =
        buildParams(null, null, "echo hello");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
  }

  @Test
  void evaluate_needsConfirmationWhenSubCommandsEmpty() {
    stubRules(List.of());

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{}, null, "echo hello");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
  }

  @Test
  void evaluate_emptyRulesUsesUnmatchedSetting() {
    stubRules(List.of());
    stubUnmatched(true);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"ls"}, new String[]{"ls"}, "ls");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  // --- Session memory via cacheDecision ---

  @Test
  void cacheDecision_acceptAllSession_autoApprovesSubsequent() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");

    ConfirmationAction allSession = buildSessionAction(
        TerminalConfirmationHandler.Action.ACCEPT_ALL_SESSION);
    handler.cacheDecision(allSession, params, CONV_ID);

    ConfirmationResult result = evaluate(params, CONV_ID);
    assertTrue(result.isAutoApproved());
  }

  @Test
  void cacheDecision_acceptNamesSession_autoApprovesMatchingNames() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");

    ConfirmationAction namesSession = buildSessionAction(
        TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION);
    handler.cacheDecision(namesSession, params, CONV_ID);

    ConfirmationResult result = evaluate(params, CONV_ID);
    assertTrue(result.isAutoApproved());
  }

  @Test
  void cacheDecision_acceptExactSession_autoApprovesMatchingCommand() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");

    ConfirmationAction exactSession = buildSessionAction(
        TerminalConfirmationHandler.Action.ACCEPT_EXACT_SESSION);
    handler.cacheDecision(exactSession, params, CONV_ID);

    ConfirmationResult result = evaluate(params, CONV_ID);
    assertTrue(result.isAutoApproved());
  }

  @Test
  void clearSession_removesApprovalsForConversation() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");

    ConfirmationAction allSession = buildSessionAction(
        TerminalConfirmationHandler.Action.ACCEPT_ALL_SESSION);
    handler.cacheDecision(allSession, params, CONV_ID);

    handler.clearSession(CONV_ID);

    ConfirmationResult result = evaluate(params, CONV_ID);
    assertFalse(result.isAutoApproved());
  }

  @Test
  void clearSession_doesNotAffectOtherConversation() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");

    ConfirmationAction allSession = buildSessionAction(
        TerminalConfirmationHandler.Action.ACCEPT_ALL_SESSION);
    handler.cacheDecision(allSession, params, CONV_ID);

    handler.clearSession("other-conv");

    ConfirmationResult result = evaluate(params, CONV_ID);
    assertTrue(result.isAutoApproved());
  }

  // --- buildContent actions ---

  @Test
  void buildContent_alwaysHasAllowOnceAsPrimary() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");
    ConfirmationResult result = evaluate(params, CONV_ID);

    ConfirmationContent content = result.getContent();
    assertNotNull(content);
    List<ConfirmationAction> actions = content.getActions();
    ConfirmationAction first = actions.get(0);
    assertTrue(first.isPrimary());
    assertTrue(first.isAccept());
  }

  @Test
  void buildContent_alwaysHasSkipAsDismiss() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    ConfirmationAction last = actions.get(actions.size() - 1);
    assertFalse(last.isAccept());
  }

  @Test
  void buildContent_hasAllowAllSessionAction() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    boolean hasAllSession = actions.stream().anyMatch(a ->
        a.getMetadata().containsKey(ConfirmationAction.META_ACTION)
            && a.getMetadata().get(ConfirmationAction.META_ACTION)
            .equals(
                TerminalConfirmationHandler.Action.ACCEPT_ALL_SESSION
                    .name()));
    assertTrue(hasAllSession);
  }

  @Test
  void buildContent_hasCommandNameActionsWhenNamesPresent() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    boolean hasNamesSession = actions.stream().anyMatch(a ->
        hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION));
    boolean hasNamesGlobal = actions.stream().anyMatch(a ->
        hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_GLOBAL));
    assertTrue(hasNamesSession);
    assertTrue(hasNamesGlobal);
  }

  @Test
  void buildContent_hasExactCommandActionsWhenDifferentFromName() {
    stubRules(List.of());
    stubUnmatched(false);

    // commandLine "echo hello" differs from single commandName "echo"
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    boolean hasExactSession = actions.stream().anyMatch(a ->
        hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_EXACT_SESSION));
    boolean hasExactGlobal = actions.stream().anyMatch(a ->
        hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_EXACT_GLOBAL));
    assertTrue(hasExactSession);
    assertTrue(hasExactGlobal);
  }

  @Test
  void buildContent_noExactActionsWhenSingleSubCommandEqualsName() {
    stubRules(List.of());
    stubUnmatched(false);

    // commandLine equals the single commandName
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"}, "echo");
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    boolean hasExact = actions.stream().anyMatch(a ->
        hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_EXACT_SESSION)
            || hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_EXACT_GLOBAL));
    assertFalse(hasExact);
  }

  @Test
  void buildContent_actionScopesAreCorrect() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo"}, new String[]{"echo"},
            "echo hello");
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();

    // Allow Once → ONCE scope
    assertEquals(ConfirmationActionScope.ONCE, actions.get(0).getScope());

    // Session actions have SESSION scope
    actions.stream()
        .filter(a -> hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION)
            || hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_EXACT_SESSION)
            || hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_ALL_SESSION))
        .forEach(a -> assertEquals(ConfirmationActionScope.SESSION,
            a.getScope()));

    // Global actions have GLOBAL scope
    actions.stream()
        .filter(a -> hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_GLOBAL)
            || hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_EXACT_GLOBAL))
        .forEach(a -> assertEquals(ConfirmationActionScope.GLOBAL,
            a.getScope()));
  }

  // --- Helpers ---

  private void stubRules(List<TerminalAutoApproveRule> rules) {
    when(preferenceStore.getString(Constants.AUTO_APPROVE_TERMINAL_RULES))
        .thenReturn(GSON.toJson(rules));
  }

  private void stubUnmatched(boolean value) {
    when(preferenceStore.getBoolean(
        Constants.AUTO_APPROVE_UNMATCHED_TERMINAL)).thenReturn(value);
  }

  private ConfirmationResult evaluate(
      InvokeClientToolConfirmationParams params, String conversationId) {
    return handler.evaluate(params, conversationId, true);
  }

  private static InvokeClientToolConfirmationParams buildParams(
      String[] subCommands, String[] commandNames, String commandLine) {
    TerminalCommandData tcd = new TerminalCommandData();
    tcd.setSubCommands(subCommands);
    tcd.setCommandNames(commandNames);

    ToolMetadata meta = new ToolMetadata();
    meta.setTerminalCommandData(tcd);

    InvokeClientToolConfirmationParams params =
        new InvokeClientToolConfirmationParams();
    params.setConversationId(CONV_ID);
    params.setToolMetadata(meta);
    params.setInput(Map.of("toolType", "terminal", "command",
        commandLine != null ? commandLine : ""));
    return params;
  }

  private static ConfirmationAction buildSessionAction(
      TerminalConfirmationHandler.Action actionType) {
    Map<String, String> meta = Map.of(
        ConfirmationAction.META_ACTION, actionType.name());
    return new ConfirmationAction(
        "test", true, ConfirmationActionScope.SESSION, meta, false);
  }

  private static boolean hasActionType(ConfirmationAction action,
      TerminalConfirmationHandler.Action type) {
    return action.getMetadata().containsKey(ConfirmationAction.META_ACTION)
        && action.getMetadata().get(ConfirmationAction.META_ACTION)
        .equals(type.name());
  }

  // --- Unapproved filtering tests ---

  @Test
  void buildContent_filtersSessionApprovedNamesFromActions() {
    stubRules(List.of());
    stubUnmatched(false);

    // "echo && curl" — approve echo in session first
    InvokeClientToolConfirmationParams approveParams =
        buildParams(new String[]{"echo hello"}, new String[]{"echo"},
            "echo hello");
    handler.cacheDecision(
        buildSessionAction(
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION),
        approveParams, CONV_ID);

    // Now evaluate "echo hello && curl example.com"
    InvokeClientToolConfirmationParams params =
        buildParams(
            new String[]{"echo hello", "curl example.com"},
            new String[]{"echo", "curl"},
            "echo hello && curl example.com");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
    List<ConfirmationAction> actions = result.getContent().getActions();

    // Command-name actions should only mention "curl", not "echo"
    ConfirmationAction namesAction = actions.stream()
        .filter(a -> hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION))
        .findFirst().orElse(null);
    assertNotNull(namesAction);
    assertTrue(namesAction.getLabel().contains("curl"));
    assertFalse(namesAction.getLabel().contains("echo"));
  }

  @Test
  void buildContent_filtersGlobalApprovedNamesFromActions() {
    // Global allow rule for "echo"
    stubRules(List.of(new TerminalAutoApproveRule("echo", true)));
    stubUnmatched(false);

    // Evaluate "echo hello && hostname"
    InvokeClientToolConfirmationParams params =
        buildParams(
            new String[]{"echo hello", "hostname"},
            new String[]{"echo", "hostname"},
            "echo hello && hostname");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
    List<ConfirmationAction> actions = result.getContent().getActions();

    // "echo" is globally allowed — only "hostname" in actions
    ConfirmationAction namesAction = actions.stream()
        .filter(a -> hasActionType(a,
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION))
        .findFirst().orElse(null);
    assertNotNull(namesAction);
    assertTrue(namesAction.getLabel().contains("hostname"));
    assertFalse(namesAction.getLabel().contains("echo"));
  }

  @Test
  void buildContent_allNamesApproved_autoApproves() {
    stubRules(List.of(new TerminalAutoApproveRule("echo", true)));
    stubUnmatched(false);

    // Session-approve "curl"
    InvokeClientToolConfirmationParams approveParams =
        buildParams(new String[]{"curl x"}, new String[]{"curl"},
            "curl x");
    handler.cacheDecision(
        buildSessionAction(
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION),
        approveParams, CONV_ID);

    // Evaluate "echo hello && curl example.com"
    // echo = global allow, curl = session allow → all approved
    InvokeClientToolConfirmationParams params =
        buildParams(
            new String[]{"echo hello", "curl example.com"},
            new String[]{"echo", "curl"},
            "echo hello && curl example.com");
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  @Test
  void sessionRules_surviveConversationSwitch() {
    // Approve "echo" in conversation A
    InvokeClientToolConfirmationParams approveParams =
        buildParams(new String[]{"echo hello"}, new String[]{"echo"},
            "echo hello");
    handler.cacheDecision(
        buildSessionAction(
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION),
        approveParams, "conv-A");

    // Switch to conversation B, then back to A — rule should survive
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo world"}, new String[]{"echo"},
            "echo world");
    ConfirmationResult result = evaluate(params, "conv-A");
    assertTrue(result.isAutoApproved());
  }

  @Test
  void sessionRules_evictOldestWhenCapExceeded() {
    // Fill up to MAX_SESSION_CONVERSATIONS with unique conversation IDs
    for (int i = 0; i < TerminalConfirmationHandler.MAX_SESSION_CONVERSATIONS;
         i++) {
      InvokeClientToolConfirmationParams p =
          buildParams(new String[]{"echo"}, new String[]{"echo"}, "echo");
      handler.cacheDecision(
          buildSessionAction(
              TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION),
          p, "conv-" + i);
    }

    // Add one more — should evict the oldest (conv-0)
    InvokeClientToolConfirmationParams p =
        buildParams(new String[]{"echo"}, new String[]{"echo"}, "echo");
    handler.cacheDecision(
        buildSessionAction(
            TerminalConfirmationHandler.Action.ACCEPT_NAMES_SESSION),
        p, "conv-new");

    // conv-0 should have been evicted
    InvokeClientToolConfirmationParams params =
        buildParams(new String[]{"echo test"}, new String[]{"echo"},
            "echo test");
    ConfirmationResult evicted = evaluate(params, "conv-0");
    assertFalse(evicted.isAutoApproved());

    // conv-new should still work
    ConfirmationResult kept = evaluate(params, "conv-new");
    assertTrue(kept.isAutoApproved());

    // conv-1 (second oldest, not evicted) should still work
    ConfirmationResult second = evaluate(params, "conv-1");
    assertTrue(second.isAutoApproved());
  }
}
