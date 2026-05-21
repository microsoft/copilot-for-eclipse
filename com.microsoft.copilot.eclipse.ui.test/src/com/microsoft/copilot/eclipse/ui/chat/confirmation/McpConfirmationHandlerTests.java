// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.confirmation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationAction;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationActionScope;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationContent;
import com.microsoft.copilot.eclipse.core.chat.ConfirmationResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ToolAnnotations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class McpConfirmationHandlerTests {

  private static final String CONV_ID = "conv-mcp-1";
  private static final String SERVER = "myServer";
  private static final String TOOL = "myTool";
  private static final Gson GSON = new Gson();

  @Mock
  private IPreferenceStore preferenceStore;

  private McpConfirmationHandler handler;

  @BeforeEach
  void setUp() {
    handler = new McpConfirmationHandler(preferenceStore);
    stubGlobalServers(List.of());
    stubGlobalTools(List.of());
    stubTrustAnnotations(false);
  }

  // --- evaluate: global server list ---

  @Test
  void evaluate_autoApprovedWhenServerInGlobalList() {
    stubGlobalServers(List.of(SERVER));

    ConfirmationResult result = evaluate(
        buildParams(SERVER, TOOL), CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  @Test
  void evaluate_autoApprovedWhenServerInGlobalListCaseInsensitive() {
    stubGlobalServers(List.of(SERVER.toUpperCase()));

    ConfirmationResult result = evaluate(
        buildParams(SERVER.toLowerCase(), TOOL), CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  @Test
  void evaluate_notAutoApprovedWhenServerNotInGlobalList() {
    stubGlobalServers(List.of("otherServer"));

    ConfirmationResult result = evaluate(
        buildParams(SERVER, TOOL), CONV_ID);

    assertFalse(result.isAutoApproved());
  }

  // --- evaluate: global tool list ---

  @Test
  void evaluate_autoApprovedWhenToolInGlobalList() {
    String toolKey = SERVER.toLowerCase() + "::" + TOOL.toLowerCase();
    stubGlobalTools(List.of(toolKey));

    ConfirmationResult result = evaluate(
        buildParams(SERVER, TOOL), CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  @Test
  void evaluate_autoApprovedWhenToolInGlobalListCaseInsensitive() {
    String toolKey = SERVER.toUpperCase() + "::" + TOOL.toUpperCase();
    stubGlobalTools(List.of(toolKey));

    ConfirmationResult result = evaluate(
        buildParams(SERVER.toLowerCase(), TOOL.toLowerCase()), CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  @Test
  void evaluate_notAutoApprovedWhenOnlyOtherToolInGlobalList() {
    String otherKey = SERVER.toLowerCase() + "::otherTool";
    stubGlobalTools(List.of(otherKey));

    ConfirmationResult result = evaluate(
        buildParams(SERVER, TOOL), CONV_ID);

    assertFalse(result.isAutoApproved());
  }

  // --- evaluate: trust annotations ---

  @Test
  void evaluate_autoApprovedWhenReadOnlyAndTrustAnnotationsEnabled() {
    stubTrustAnnotations(true);
    ToolAnnotations annotations = new ToolAnnotations();
    annotations.setReadOnlyHint(true);
    annotations.setOpenWorldHint(false);

    InvokeClientToolConfirmationParams params = buildParams(SERVER, TOOL);
    params.setAnnotations(annotations);

    ConfirmationResult result = evaluate(params, CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  @Test
  void evaluate_notAutoApprovedWhenReadOnlyButOpenWorldHint() {
    stubTrustAnnotations(true);
    ToolAnnotations annotations = new ToolAnnotations();
    annotations.setReadOnlyHint(true);
    annotations.setOpenWorldHint(true);

    InvokeClientToolConfirmationParams params = buildParams(SERVER, TOOL);
    params.setAnnotations(annotations);

    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
  }

  @Test
  void evaluate_notAutoApprovedWhenAnnotationsTrustedButNotReadOnly() {
    stubTrustAnnotations(true);
    ToolAnnotations annotations = new ToolAnnotations();
    annotations.setReadOnlyHint(false);
    annotations.setOpenWorldHint(false);

    InvokeClientToolConfirmationParams params = buildParams(SERVER, TOOL);
    params.setAnnotations(annotations);

    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
  }

  @Test
  void evaluate_notAutoApprovedWhenReadOnlyButTrustAnnotationsDisabled() {
    stubTrustAnnotations(false);
    ToolAnnotations annotations = new ToolAnnotations();
    annotations.setReadOnlyHint(true);
    annotations.setOpenWorldHint(false);

    InvokeClientToolConfirmationParams params = buildParams(SERVER, TOOL);
    params.setAnnotations(annotations);

    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
  }

  // --- evaluate: session approvals ---

  @Test
  void evaluate_autoApprovedWhenToolApprovedForSession() {
    InvokeClientToolConfirmationParams params = buildParams(SERVER, TOOL);
    ConfirmationAction action = buildAction(
        McpConfirmationHandler.Action.ACCEPT_TOOL_SESSION,
        Map.of(McpConfirmationHandler.META_TOOL_KEY,
            SERVER.toLowerCase() + "::" + TOOL.toLowerCase()));
    handler.cacheDecision(action, params, CONV_ID);

    ConfirmationResult result = evaluate(params, CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  @Test
  void evaluate_notAutoApprovedWhenToolApprovedForDifferentSession() {
    InvokeClientToolConfirmationParams params = buildParams(SERVER, TOOL);
    ConfirmationAction action = buildAction(
        McpConfirmationHandler.Action.ACCEPT_TOOL_SESSION,
        Map.of(McpConfirmationHandler.META_TOOL_KEY,
            SERVER.toLowerCase() + "::" + TOOL.toLowerCase()));
    handler.cacheDecision(action, params, "other-conv");

    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
  }

  @Test
  void evaluate_autoApprovedWhenServerApprovedForSession() {
    InvokeClientToolConfirmationParams params = buildParams(SERVER, TOOL);
    ConfirmationAction action = buildAction(
        McpConfirmationHandler.Action.ACCEPT_SERVER_SESSION,
        Map.of(McpConfirmationHandler.META_SERVER_NAME, SERVER));
    handler.cacheDecision(action, params, CONV_ID);

    ConfirmationResult result = evaluate(params, CONV_ID);

    assertTrue(result.isAutoApproved());
  }

  // --- cacheDecision: global persistence ---

  @Test
  void cacheDecision_acceptToolGlobal_writesToPreferenceStore() {
    String toolKey = SERVER.toLowerCase() + "::" + TOOL.toLowerCase();
    stubGlobalTools(List.of());

    ConfirmationAction action = buildAction(
        McpConfirmationHandler.Action.ACCEPT_TOOL_GLOBAL,
        Map.of(McpConfirmationHandler.META_TOOL_KEY, toolKey));
    handler.cacheDecision(action, buildParams(SERVER, TOOL), CONV_ID);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(preferenceStore).setValue(
        org.mockito.ArgumentMatchers.eq(Constants.AUTO_APPROVE_MCP_TOOLS),
        captor.capture());
    assertTrue(captor.getValue().contains(toolKey));
  }

  @Test
  void cacheDecision_acceptServerGlobal_writesToPreferenceStore() {
    stubGlobalServers(List.of());

    ConfirmationAction action = buildAction(
        McpConfirmationHandler.Action.ACCEPT_SERVER_GLOBAL,
        Map.of(McpConfirmationHandler.META_SERVER_NAME, SERVER));
    handler.cacheDecision(action, buildParams(SERVER, TOOL), CONV_ID);

    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(preferenceStore).setValue(
        org.mockito.ArgumentMatchers.eq(
            Constants.AUTO_APPROVE_MCP_SERVERS),
        captor.capture());
    assertTrue(captor.getValue().contains(SERVER));
  }

  @Test
  void cacheDecision_acceptToolGlobal_noDuplicateWrite() {
    String toolKey = SERVER.toLowerCase() + "::" + TOOL.toLowerCase();
    stubGlobalTools(List.of(toolKey));

    ConfirmationAction action = buildAction(
        McpConfirmationHandler.Action.ACCEPT_TOOL_GLOBAL,
        Map.of(McpConfirmationHandler.META_TOOL_KEY, toolKey));
    handler.cacheDecision(action, buildParams(SERVER, TOOL), CONV_ID);

    // setValue should NOT be called — key already present
    verify(preferenceStore, org.mockito.Mockito.never())
        .setValue(org.mockito.ArgumentMatchers.eq(
            Constants.AUTO_APPROVE_MCP_TOOLS),
            org.mockito.ArgumentMatchers.anyString());
  }

  // --- clearSession ---

  @Test
  void clearSession_removesSessionApprovalsForConversation() {
    InvokeClientToolConfirmationParams params = buildParams(SERVER, TOOL);
    ConfirmationAction action = buildAction(
        McpConfirmationHandler.Action.ACCEPT_TOOL_SESSION,
        Map.of(McpConfirmationHandler.META_TOOL_KEY,
            SERVER.toLowerCase() + "::" + TOOL.toLowerCase()));
    handler.cacheDecision(action, params, CONV_ID);

    handler.clearSession(CONV_ID);

    ConfirmationResult result = evaluate(params, CONV_ID);
    assertFalse(result.isAutoApproved());
  }

  @Test
  void clearSession_doesNotAffectOtherConversation() {
    InvokeClientToolConfirmationParams params = buildParams(SERVER, TOOL);
    ConfirmationAction action = buildAction(
        McpConfirmationHandler.Action.ACCEPT_TOOL_SESSION,
        Map.of(McpConfirmationHandler.META_TOOL_KEY,
            SERVER.toLowerCase() + "::" + TOOL.toLowerCase()));
    handler.cacheDecision(action, params, CONV_ID);

    handler.clearSession("other-conv");

    ConfirmationResult result = evaluate(params, CONV_ID);
    assertTrue(result.isAutoApproved());
  }

  // --- buildContent (actions) ---

  @Test
  void buildContent_hasAllowOnceAsFirstAction() {
    ConfirmationResult result = evaluate(
        buildParams(SERVER, TOOL), CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    assertTrue(actions.get(0).isPrimary());
    assertTrue(actions.get(0).isAccept());
    assertEquals(ConfirmationActionScope.ONCE, actions.get(0).getScope());
  }

  @Test
  void buildContent_hasSkipAsLastAction() {
    ConfirmationResult result = evaluate(
        buildParams(SERVER, TOOL), CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    assertFalse(actions.get(actions.size() - 1).isAccept());
  }

  @Test
  void buildContent_hasAllFourScopedActions() {
    ConfirmationResult result = evaluate(
        buildParams(SERVER, TOOL), CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    assertTrue(hasAction(actions,
        McpConfirmationHandler.Action.ACCEPT_TOOL_SESSION));
    assertTrue(hasAction(actions,
        McpConfirmationHandler.Action.ACCEPT_TOOL_GLOBAL));
    assertTrue(hasAction(actions,
        McpConfirmationHandler.Action.ACCEPT_SERVER_SESSION));
    assertTrue(hasAction(actions,
        McpConfirmationHandler.Action.ACCEPT_SERVER_GLOBAL));
  }

  @Test
  void buildContent_toolAndServerActionsHaveCorrectScopes() {
    ConfirmationResult result = evaluate(
        buildParams(SERVER, TOOL), CONV_ID);

    List<ConfirmationAction> actions = result.getContent().getActions();
    actions.stream()
        .filter(a -> hasAction(a,
            McpConfirmationHandler.Action.ACCEPT_TOOL_SESSION)
            || hasAction(a,
            McpConfirmationHandler.Action.ACCEPT_SERVER_SESSION))
        .forEach(a -> assertEquals(
            ConfirmationActionScope.SESSION, a.getScope()));
    actions.stream()
        .filter(a -> hasAction(a,
            McpConfirmationHandler.Action.ACCEPT_TOOL_GLOBAL)
            || hasAction(a,
            McpConfirmationHandler.Action.ACCEPT_SERVER_GLOBAL))
        .forEach(a -> assertEquals(
            ConfirmationActionScope.GLOBAL, a.getScope()));
  }

  @Test
  void buildContent_contentHasTitleWithToolAndServer() {
    ConfirmationResult result = evaluate(
        buildParams(SERVER, TOOL), CONV_ID);

    ConfirmationContent content = result.getContent();
    assertNotNull(content);
    assertNotNull(content.getTitle());
    assertTrue(content.getTitle().contains(TOOL));
    assertTrue(content.getTitle().contains(SERVER));
  }

  @Test
  void buildContent_noActionsWhenServerAndToolNull() {
    InvokeClientToolConfirmationParams params =
        buildParams(null, null);

    ConfirmationResult result = evaluate(params, CONV_ID);

    assertFalse(result.isAutoApproved());
    List<ConfirmationAction> actions = result.getContent().getActions();
    assertFalse(hasAction(actions,
        McpConfirmationHandler.Action.ACCEPT_TOOL_SESSION));
    assertFalse(hasAction(actions,
        McpConfirmationHandler.Action.ACCEPT_SERVER_SESSION));
  }

  // --- Helpers ---

  private void stubGlobalServers(List<String> servers) {
    when(preferenceStore.getString(Constants.AUTO_APPROVE_MCP_SERVERS))
        .thenReturn(GSON.toJson(servers));
  }

  private void stubGlobalTools(List<String> tools) {
    when(preferenceStore.getString(Constants.AUTO_APPROVE_MCP_TOOLS))
        .thenReturn(GSON.toJson(tools));
  }

  private void stubTrustAnnotations(boolean value) {
    when(preferenceStore.getBoolean(
        Constants.AUTO_APPROVE_TRUST_TOOL_ANNOTATIONS))
        .thenReturn(value);
  }

  private ConfirmationResult evaluate(
      InvokeClientToolConfirmationParams params, String conversationId) {
    return handler.evaluate(params, conversationId, true);
  }

  private static InvokeClientToolConfirmationParams buildParams(
      String serverName, String toolName) {
    InvokeClientToolConfirmationParams params =
        new InvokeClientToolConfirmationParams();
    params.setConversationId(CONV_ID);
    Map<String, Object> input = new java.util.HashMap<>();
    input.put("toolType", "mcp_tool");
    if (serverName != null) {
      input.put("mcpServerName", serverName);
    }
    if (toolName != null) {
      input.put("mcpToolName", toolName);
    }
    params.setInput(input);
    return params;
  }

  private static ConfirmationAction buildAction(
      McpConfirmationHandler.Action type, Map<String, String> extra) {
    Map<String, String> meta = new java.util.HashMap<>(extra);
    meta.put(ConfirmationAction.META_ACTION, type.name());
    return new ConfirmationAction(
        "test", true, ConfirmationActionScope.SESSION, meta, false);
  }

  private static boolean hasAction(List<ConfirmationAction> actions,
      McpConfirmationHandler.Action type) {
    return actions.stream().anyMatch(a -> hasAction(a, type));
  }

  private static boolean hasAction(ConfirmationAction action,
      McpConfirmationHandler.Action type) {
    return action.getMetadata().containsKey(ConfirmationAction.META_ACTION)
        && action.getMetadata().get(ConfirmationAction.META_ACTION)
        .equals(type.name());
  }
}
