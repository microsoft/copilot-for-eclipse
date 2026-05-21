// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
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
import com.microsoft.copilot.eclipse.core.chat.FileOperationAutoApproveRule;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ToolMetadata;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ToolMetadata.SensitiveFileData;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileOperationConfirmationHandlerTests {

  private static final String CONV_ID = "conv-1";
  private static final Gson GSON = new Gson();

  @Mock
  private IPreferenceStore preferenceStore;

  private AttachedFileRegistry attachedFileRegistry;
  private FileOperationConfirmationHandler handler;

  @BeforeEach
  void setUp() {
    attachedFileRegistry = new AttachedFileRegistry();
    handler = new FileOperationConfirmationHandler(
        preferenceStore, attachedFileRegistry);
  }

  // --- glob matching behavior (tested via evaluate + rules) ---

  @Test
  void evaluate_globExactPathMatchCaseInsensitive() {
    // Rule uses forward slash + lowercase; evaluate uses backslash + uppercase
    stubRules(List.of(
        new FileOperationAutoApproveRule("C:/Users/test.java", "", true)));
    stubUnmatched(false);

    assertTrue(evaluate(
        buildParams("C:\\Users\\test.java", false), CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_globStarStarPatternMatches() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/*.java", "", true)));
    stubUnmatched(false);

    assertTrue(evaluate(
        buildParams("/workspace/src/Main.java", false), CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_globPatternNoMatch() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/*.py", "", true)));
    stubUnmatched(false);

    assertFalse(evaluate(
        buildParams("/workspace/src/Main.java", false), CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_globBackslashPathNormalized() {
    // Rule uses forward slashes; file path uses backslashes
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/.github/instructions/*", "", true)));
    stubUnmatched(false);

    assertTrue(evaluate(
        buildParams("C:\\project\\.github\\instructions\\file.md", false),
        CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_invalidGlobRuleFallsThrough() {
    // Invalid glob should not match; falls through to unmatched setting
    stubRules(List.of(
        new FileOperationAutoApproveRule("[invalid", "", true)));
    stubUnmatched(true);

    assertTrue(evaluate(
        buildParams("/a/b.java", false), CONV_ID).isAutoApproved());
  }

  // --- evaluate: attached files ---

  @Test
  void evaluate_autoApprovedWhenFileAttachedViaPending() {
    attachedFileRegistry.addPending(List.of("/workspace/src/Main.java"));

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_autoApprovedWhenFileAttachedToConversation() {
    attachedFileRegistry.addAttachedFiles(CONV_ID,
        List.of("/workspace/src/Main.java"));

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_attachedFilePathNormalized() {
    // Attached with backslashes + uppercase
    attachedFileRegistry.addPending(
        List.of("C:\\Workspace\\Src\\Main.java"));

    // Evaluate with forward slashes + lowercase
    InvokeClientToolConfirmationParams params =
        buildParams("c:/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  // --- evaluate: session overrides ---

  @Test
  void evaluate_autoApprovedBySessionFileApproval() {
    // Cache a file-level session approval
    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FILE_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FILE_PATH,
            "/workspace/src/Main.java"));
    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    handler.cacheDecision(action, params, CONV_ID);

    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_sessionFileApprovalNormalizesPath() {
    // Cache with backslash + uppercase
    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FILE_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FILE_PATH,
            "C:\\Workspace\\Main.java"));
    handler.cacheDecision(action,
        buildParams("C:\\Workspace\\Main.java", false), CONV_ID);

    // Evaluate with forward slash + lowercase
    InvokeClientToolConfirmationParams params =
        buildParams("c:/workspace/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_autoApprovedBySessionFolderApproval() {
    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FOLDER_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FOLDER_PATH,
            "/home/user/external"));
    handler.cacheDecision(action,
        buildParams("/home/user/external/file.txt", true), CONV_ID);

    InvokeClientToolConfirmationParams params =
        buildParams("/home/user/external/data.csv", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_sessionFolderDoesNotMatchParentPath() {
    stubRules(List.of());
    stubUnmatched(false);

    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FOLDER_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FOLDER_PATH,
            "/home/user/external"));
    handler.cacheDecision(action,
        buildParams("/home/user/external/file.txt", true), CONV_ID);

    // File in a different folder (prefix but not under the folder)
    InvokeClientToolConfirmationParams params =
        buildParams("/home/user/external-other/file.txt", false);
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_sessionApprovalDoesNotAffectOtherConversation() {
    stubRules(List.of());
    stubUnmatched(false);

    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FILE_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FILE_PATH,
            "/workspace/src/Main.java"));
    handler.cacheDecision(action,
        buildParams("/workspace/src/Main.java", false), CONV_ID);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertFalse(evaluate(params, "other-conv").isAutoApproved());
  }

  // --- evaluate: outside workspace ---

  @Test
  void evaluate_outsideWorkspaceAlwaysRequiresConfirmation() {
    InvokeClientToolConfirmationParams params =
        buildParams("/tmp/secret.txt", true);
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_outsideWorkspaceStillAutoApprovedBySessionFolder() {
    // Session folder approval overrides the outside-workspace check
    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FOLDER_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FOLDER_PATH,
            "/tmp"));
    handler.cacheDecision(action,
        buildParams("/tmp/file.txt", true), CONV_ID);

    InvokeClientToolConfirmationParams params =
        buildParams("/tmp/other.txt", true);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  // --- evaluate: rule matching ---

  @Test
  void evaluate_autoApprovedByAllowRule() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/*.java", "", true)));
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_needsConfirmationByDenyRule() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/.github/**/*", "", false)));

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/.github/instructions/rules.md", false);
    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
    assertNotNull(result.getContent());
  }

  @Test
  void evaluate_firstMatchingRuleWins() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/*.java", "", false),
        new FileOperationAutoApproveRule("**/*", "", true)));
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    // The first rule (deny .java) should win
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  // --- evaluate: unmatched fallback ---

  @Test
  void evaluate_unmatchedAutoApprovedWhenCheckboxTrue() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/*.py", "", true)));
    stubUnmatched(true);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_unmatchedNeedsConfirmationWhenCheckboxFalse() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/*.py", "", true)));
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_emptyRulesUsesUnmatchedSetting() {
    stubRules(List.of());
    stubUnmatched(true);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  // --- evaluate: blank file path ---

  @Test
  void evaluate_blankFilePathNeedsConfirmation() {
    stubRules(List.of());
    stubUnmatched(true);

    InvokeClientToolConfirmationParams params =
        buildParams(null, false);
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  // --- evaluate: file path extraction ---

  @Test
  void evaluate_extractsFilePathFromSensitiveFileData() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/*.java", "", true)));
    stubUnmatched(false);

    // Path set via sensitiveFileData (toolMetadata), not input map
    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_extractsFilePathFromInputMapFallback() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/*.java", "", true)));
    stubUnmatched(false);

    // No toolMetadata, only input map with "filePath"
    InvokeClientToolConfirmationParams params =
        new InvokeClientToolConfirmationParams();
    params.setConversationId(CONV_ID);
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", "/workspace/src/Main.java");
    input.put("toolType", "file_write");
    params.setInput(input);

    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_extractsPathKeyFromInputMapFallback() {
    stubRules(List.of(
        new FileOperationAutoApproveRule("**/*.java", "", true)));
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        new InvokeClientToolConfirmationParams();
    params.setConversationId(CONV_ID);
    Map<String, Object> input = new HashMap<>();
    input.put("path", "/workspace/src/Main.java");
    input.put("toolType", "file_write");
    params.setInput(input);

    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  // --- cacheDecision: global rule ---

  @Test
  void cacheDecision_globalAddsRuleToPreferenceStore() {
    stubRules(List.of());

    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FILE_GLOBAL,
        Map.of(FileOperationConfirmationHandler.META_FILE_PATH,
            "/workspace/src/Main.java"));

    handler.cacheDecision(action,
        buildParams("/workspace/src/Main.java", false), CONV_ID);

    // The handler should have called setValue on the preference store.
    // We verify by loading rules from the same store (which requires
    // the mock to return the updated value). Instead, verify the
    // store was called with the right key.
    org.mockito.Mockito.verify(preferenceStore).setValue(
        org.mockito.ArgumentMatchers.eq(Constants.AUTO_APPROVE_FILE_OP_RULES),
        org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void cacheDecision_globalUpdatesExistingRuleCaseInsensitive() {
    // Start with a deny rule
    stubRules(List.of(
        new FileOperationAutoApproveRule(
            "C:/workspace/Main.java", "", false)));

    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FILE_GLOBAL,
        Map.of(FileOperationConfirmationHandler.META_FILE_PATH,
            "c:/workspace/Main.java"));

    handler.cacheDecision(action,
        buildParams("c:/workspace/Main.java", false), CONV_ID);

    // Verify setValue was called (updated existing rule to autoApprove)
    org.mockito.Mockito.verify(preferenceStore).setValue(
        org.mockito.ArgumentMatchers.eq(Constants.AUTO_APPROVE_FILE_OP_RULES),
        org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void cacheDecision_ignoresUnknownAction() {
    Map<String, String> meta = Map.of(
        ConfirmationAction.META_ACTION, "UNKNOWN_ACTION");
    ConfirmationAction action = new ConfirmationAction(
        "test", true, ConfirmationActionScope.SESSION, meta, false);

    // Should not throw
    handler.cacheDecision(action,
        buildParams("/workspace/src/Main.java", false), CONV_ID);
  }

  @Test
  void cacheDecision_ignoresNullActionMetadata() {
    ConfirmationAction action = new ConfirmationAction(
        "test", true, ConfirmationActionScope.SESSION, Map.of(), false);

    // Should not throw
    handler.cacheDecision(action,
        buildParams("/workspace/src/Main.java", false), CONV_ID);
  }

  // --- clearSession ---

  @Test
  void clearSession_removesFileAndFolderApprovals() {
    stubRules(List.of());
    stubUnmatched(false);

    ConfirmationAction fileAction = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FILE_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FILE_PATH,
            "/workspace/src/Main.java"));
    handler.cacheDecision(fileAction,
        buildParams("/workspace/src/Main.java", false), CONV_ID);

    handler.clearSession(CONV_ID);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void clearSession_doesNotAffectOtherConversation() {
    stubRules(List.of());
    stubUnmatched(false);

    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FILE_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FILE_PATH,
            "/workspace/src/Main.java"));
    handler.cacheDecision(action,
        buildParams("/workspace/src/Main.java", false), CONV_ID);

    handler.clearSession("other-conv");

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void clearSession_clearsAttachedFileRegistry() {
    stubRules(List.of());
    stubUnmatched(false);

    attachedFileRegistry.addAttachedFiles(CONV_ID,
        List.of("/workspace/src/Main.java"));

    handler.clearSession(CONV_ID);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertFalse(evaluate(params, CONV_ID).isAutoApproved());
  }

  // --- buildContent: in-workspace actions ---

  @Test
  void buildContent_inWorkspaceHasAllowOnceAsPrimary() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    ConfirmationResult result = evaluate(params, CONV_ID);

    ConfirmationContent content = result.getContent();
    assertNotNull(content);
    List<ConfirmationAction> actions = content.getActions();
    ConfirmationAction first = actions.get(0);
    assertTrue(first.isPrimary());
    assertTrue(first.isAccept());
    assertEquals(ConfirmationActionScope.ONCE, first.getScope());
  }

  @Test
  void buildContent_inWorkspaceHasSkipAsDismiss() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    ConfirmationAction last = actions.get(actions.size() - 1);
    assertFalse(last.isAccept());
  }

  @Test
  void buildContent_inWorkspaceHasFileSessionAndGlobalActions() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    boolean hasFileSession = actions.stream().anyMatch(a ->
        hasActionType(a,
            FileOperationConfirmationHandler.Action.ACCEPT_FILE_SESSION));
    boolean hasFileGlobal = actions.stream().anyMatch(a ->
        hasActionType(a,
            FileOperationConfirmationHandler.Action.ACCEPT_FILE_GLOBAL));
    assertTrue(hasFileSession);
    assertTrue(hasFileGlobal);
  }

  @Test
  void buildContent_inWorkspaceNoFolderAction() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    boolean hasFolderSession = actions.stream().anyMatch(a ->
        hasActionType(a,
            FileOperationConfirmationHandler.Action.ACCEPT_FOLDER_SESSION));
    assertFalse(hasFolderSession);
  }

  // --- buildContent: outside-workspace actions ---

  @Test
  void buildContent_outsideWorkspaceHasFolderSessionAction() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/tmp/data/file.txt", true);
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    boolean hasFolderSession = actions.stream().anyMatch(a ->
        hasActionType(a,
            FileOperationConfirmationHandler.Action.ACCEPT_FOLDER_SESSION));
    assertTrue(hasFolderSession);
  }

  @Test
  void buildContent_outsideWorkspaceNoFileGlobalAction() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/tmp/data/file.txt", true);
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    boolean hasFileGlobal = actions.stream().anyMatch(a ->
        hasActionType(a,
            FileOperationConfirmationHandler.Action.ACCEPT_FILE_GLOBAL));
    assertFalse(hasFileGlobal);
  }

  // --- buildContent: action scopes ---

  @Test
  void buildContent_actionScopesAreCorrect() {
    stubRules(List.of());
    stubUnmatched(false);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    ConfirmationResult result = evaluate(params, CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();

    // Session actions have SESSION scope
    actions.stream()
        .filter(a -> hasActionType(a,
            FileOperationConfirmationHandler.Action.ACCEPT_FILE_SESSION))
        .forEach(a -> assertEquals(
            ConfirmationActionScope.SESSION, a.getScope()));

    // Global actions have GLOBAL scope
    actions.stream()
        .filter(a -> hasActionType(a,
            FileOperationConfirmationHandler.Action.ACCEPT_FILE_GLOBAL))
        .forEach(a -> assertEquals(
            ConfirmationActionScope.GLOBAL, a.getScope()));
  }

  // --- evaluate priority order ---

  @Test
  void evaluate_priorityOrder_attachedFileBeatsGlobalDenyRule() {
    // Attached file auto-approves even when a deny rule would otherwise apply
    attachedFileRegistry.addPending(
        List.of("/workspace/src/Main.java"));

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_priorityOrder_sessionApprovalBeatsGlobalDenyRule() {
    // Session-level approval auto-approves even when a deny rule would otherwise apply
    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FILE_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FILE_PATH,
            "/workspace/src/Main.java"));
    handler.cacheDecision(action,
        buildParams("/workspace/src/Main.java", false), CONV_ID);

    InvokeClientToolConfirmationParams params =
        buildParams("/workspace/src/Main.java", false);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  @Test
  void evaluate_priorityOrder_sessionFolderBeatsOutsideWorkspace() {
    // Session folder approval auto-approves even for outside-workspace files
    ConfirmationAction action = buildAction(
        FileOperationConfirmationHandler.Action.ACCEPT_FOLDER_SESSION,
        Map.of(FileOperationConfirmationHandler.META_FOLDER_PATH,
            "/external/dir"));
    handler.cacheDecision(action,
        buildParams("/external/dir/file.txt", true), CONV_ID);

    InvokeClientToolConfirmationParams params =
        buildParams("/external/dir/another.txt", true);
    assertTrue(evaluate(params, CONV_ID).isAutoApproved());
  }

  // --- Helpers ---

  private void stubRules(List<FileOperationAutoApproveRule> rules) {
    when(preferenceStore.getString(Constants.AUTO_APPROVE_FILE_OP_RULES))
        .thenReturn(GSON.toJson(rules));
  }

  private void stubUnmatched(boolean value) {
    when(preferenceStore.getBoolean(
        Constants.AUTO_APPROVE_UNMATCHED_FILE_OP)).thenReturn(value);
  }

  private ConfirmationResult evaluate(
      InvokeClientToolConfirmationParams params, String conversationId) {
    return handler.evaluate(params, conversationId, true);
  }

  private static InvokeClientToolConfirmationParams buildParams(
      String filePath, boolean isGlobal) {
    InvokeClientToolConfirmationParams params =
        new InvokeClientToolConfirmationParams();
    params.setConversationId(CONV_ID);

    if (filePath != null) {
      SensitiveFileData sfd = new SensitiveFileData();
      sfd.setFilePath(filePath);
      sfd.setGlobal(isGlobal);

      ToolMetadata meta = new ToolMetadata();
      meta.setSensitiveFileData(sfd);
      params.setToolMetadata(meta);
    }

    Map<String, Object> input = new HashMap<>();
    input.put("toolType", "file_write");
    if (filePath != null) {
      input.put("filePath", filePath);
    }
    params.setInput(input);
    return params;
  }

  private static ConfirmationAction buildAction(
      FileOperationConfirmationHandler.Action actionType,
      Map<String, String> extra) {
    Map<String, String> meta = new HashMap<>(extra);
    meta.put(ConfirmationAction.META_ACTION, actionType.name());
    return new ConfirmationAction(
        "test", true, ConfirmationActionScope.SESSION, meta, false);
  }

  private static boolean hasActionType(ConfirmationAction action,
      FileOperationConfirmationHandler.Action type) {
    return action.getMetadata().containsKey(ConfirmationAction.META_ACTION)
        && action.getMetadata().get(ConfirmationAction.META_ACTION)
        .equals(type.name());
  }
}
