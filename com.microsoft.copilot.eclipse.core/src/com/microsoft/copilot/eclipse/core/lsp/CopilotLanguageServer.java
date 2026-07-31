// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.lsp.mcp.McpRegistryAllowList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.GetServerParams;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ListServersParams;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatCreateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatTurnResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CheckStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCodeCopyParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCreateParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationDestroyParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationModesParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTurnParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CustomizationFileInfo;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidShowInlineEditParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GenerateThinkingTitleParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GenerateThinkingTitleResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GetDefaultFileSafetyRulesResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyAcceptedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyCodeAcceptanceParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyRejectedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyShownParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NullParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.RegisterToolsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInConfirmParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.TelemetryExceptionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.UpdateConversationToolsStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.UpdateMcpToolsStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.WorkspaceFoldersParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokApiKey;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokDeleteProviderConfigParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListApiKeyResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListProviderConfigParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListProviderConfigResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokProviderConfig;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokStatusResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.git.GenerateCommitMessageParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.git.GenerateCommitMessageResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi.SearchPrParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.githubapi.SearchPrResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;

/**
 * Interface for Copilot Language Server.
 */
public interface CopilotLanguageServer extends LanguageServer {

  /**
   * Check the login status for current machine.
   *
   * @param param the status check options.
   * @return the current Copilot status.
   */
  @JsonRequest
  CompletableFuture<CopilotStatusResult> checkStatus(CheckStatusParams param);

  /**
   * Check the uesr's quota status.
   *
   * @param param the empty quota request parameters.
   * @return the current quota status.
   */
  @JsonRequest
  CompletableFuture<CheckQuotaResult> checkQuota(NullParams param);

  /**
   * Get single completion for the given parameters.
   *
   * @param params the completion request parameters.
   * @return the completion result.
   */
  @JsonRequest
  CompletableFuture<CompletionResult> getCompletions(CompletionParams params);

  /**
   * Initiate the sign in process.
   *
   * @param param the empty sign-in initiation parameters.
   * @return the sign-in initiation result.
   */
  @JsonRequest
  CompletableFuture<SignInInitiateResult> signInInitiate(NullParams param);

  /**
   * Confirm the sign in process.
   *
   * @param param the sign-in confirmation parameters.
   * @return the updated Copilot status.
   */
  @JsonRequest
  CompletableFuture<CopilotStatusResult> signInConfirm(SignInConfirmParams param);

  /**
   * Sign out the current user.
   *
   * @param params the empty sign-out request parameters.
   * @return the updated Copilot status.
   */
  @JsonRequest
  CompletableFuture<CopilotStatusResult> signOut(NullParams params);

  /**
   * Notify the language server that the completion was shown.
   *
   * @param params the shown completion notification parameters.
   * @return the notification acknowledgement.
   */
  @JsonRequest
  CompletableFuture<String> notifyShown(NotifyShownParams params);

  /**
   * Notify the language server that the completion was accepted.
   *
   * @param params the accepted completion notification parameters.
   * @return the notification acknowledgement.
   */
  @JsonRequest
  CompletableFuture<String> notifyAccepted(NotifyAcceptedParams params);

  /**
   * Notify the language server that the completion was rejected.
   *
   * @param params the rejected completion notification parameters.
   * @return the notification acknowledgement.
   */
  @JsonRequest
  CompletableFuture<String> notifyRejected(NotifyRejectedParams params);

  /**
   * Send exception telemetry to github sentry.
   *
   * @param params the exception telemetry parameters.
   * @return the telemetry request result.
   */
  @JsonRequest("telemetry/exception")
  CompletableFuture<Object> sendExceptionTelemetry(TelemetryExceptionParams params);

  /**
   * Create a new conversation.
   *
   * @param param the conversation creation parameters.
   * @return the created conversation result.
   */
  @JsonRequest("conversation/create")
  CompletableFuture<ChatCreateResult> create(ConversationCreateParams param);

  /**
   * Create a new conversation.
   *
   * @param param the conversation turn parameters.
   * @return the conversation turn result.
   */
  @JsonRequest("conversation/turn")
  CompletableFuture<ChatTurnResult> addTurn(ConversationTurnParams param);

  /**
   * List conversation templates.
   *
   * @param params includes workspace folders for discovering workspace-specific prompt files and skills
   *
   * @return the available conversation templates.
   */
  @JsonRequest("conversation/templates")
  CompletableFuture<ConversationTemplate[]> listTemplates(WorkspaceFoldersParams params);

  /**
   * List custom skill files (each carries its on-disk {@code uri}).
   *
   * @param params includes the workspace folders to scan
   *
   * @return the available custom skill files.
   */
  @JsonRequest("copilot/customSkill/list")
  CompletableFuture<CustomizationFileInfo[]> listCustomSkills(WorkspaceFoldersParams params);

  /**
   * List custom prompt files (each carries its on-disk {@code uri}).
   *
   * @param params includes the workspace folders to scan
   *
   * @return the available custom prompt files.
   */
  @JsonRequest("copilot/customPrompt/list")
  CompletableFuture<CustomizationFileInfo[]> listCustomPrompts(WorkspaceFoldersParams params);

  /**
   * List custom instruction files (each carries its on-disk {@code uri}).
   *
   * @param params includes the workspace folders to scan
   *
   * @return the available custom instruction files.
   */
  @JsonRequest("copilot/customInstruction/list")
  CompletableFuture<CustomizationFileInfo[]> listCustomInstructions(WorkspaceFoldersParams params);

  /**
   * List custom agent files (each carries its on-disk {@code uri}).
   *
   * @param params includes the workspace folders to scan
   *
   * @return the available custom agent files.
   */
  @JsonRequest("copilot/customAgent/list")
  CompletableFuture<CustomizationFileInfo[]> listCustomAgents(WorkspaceFoldersParams params);

  /**
   * List conversation modes.
   *
   * @param params the conversation mode request parameters.
   * @return the available conversation modes.
   */
  @JsonRequest("conversation/modes")
  CompletableFuture<ConversationMode[]> listModes(ConversationModesParams params);

  /**
   * Used to track telemetry from users copying code from chat.
   *
   * @param param the copied code telemetry parameters.
   * @return the telemetry request acknowledgement.
   */
  @JsonRequest("conversation/copyCode")
  CompletableFuture<String> copyCode(ConversationCodeCopyParams param);

  /**
   * Used to get the persistence token for the current user.
   *
   * @param param the empty persistence request parameters.
   * @return the chat persistence token information.
   */
  @JsonRequest("conversation/persistence")
  CompletableFuture<ChatPersistence> persistence(NullParams param);

  /**
   * Destroy a conversation, stopping any in-progress processing.
   *
   * @param param the conversation destroy parameters.
   * @return the destroy request acknowledgement.
   */
  @JsonRequest("conversation/destroy")
  CompletableFuture<String> destroy(ConversationDestroyParams param);

  /**
   * Register agent tools to the language server.
   *
   * @param params the tool registration parameters.
   * @return the registered language model tool information.
   */
  @JsonRequest("conversation/registerTools")
  CompletableFuture<List<LanguageModelToolInformation>> registerTools(RegisterToolsParams params);

  /**
   * Update the status of conversation tools (built-in tools for Agent mode).
   *
   * @param params the conversation tool status parameters.
   * @return the update request result.
   */
  @JsonRequest("conversation/updateToolsStatus")
  CompletableFuture<Object> updateConversationToolsStatus(UpdateConversationToolsStatusParams params);

  /**
   * List copilot models.
   *
   * @param param the empty model list request parameters.
   * @return the available Copilot models.
   */
  @JsonRequest("copilot/models")
  CompletableFuture<CopilotModel[]> listModels(NullParams param);

  /**
   * Notify the code acceptance.
   *
   * @param params the code acceptance notification parameters.
   * @return the notification acknowledgement.
   */
  @JsonRequest("conversation/notifyCodeAcceptance")
  CompletableFuture<String> notifyCodeAcceptance(NotifyCodeAcceptanceParams params);

  /**
   * Generate commit messages.
   *
   * @param params the commit message generation parameters.
   * @return the generated commit message result.
   */
  @JsonRequest("git/commitGenerate")
  CompletableFuture<GenerateCommitMessageResult> generateCommitMessage(GenerateCommitMessageParams params);

  /**
   * Generate a short title summarizing a thinking block.
   *
   * @param params the thinking title generation parameters.
   * @return the generated thinking title response.
   */
  @JsonRequest("thinking/generateTitle")
  CompletableFuture<GenerateThinkingTitleResponse> generateThinkingTitle(GenerateThinkingTitleParams params);

  /**
   * List BYOK models.
   *
   * @param params the BYOK model list request parameters.
   * @return the BYOK model list response.
   */
  @JsonRequest("copilot/byok/listModels")
  CompletableFuture<ByokListModelResponse> listByokModels(ByokListModelParams params);

  /**
   * Save BYOK model.
   *
   * @param model the BYOK model to save.
   * @return the BYOK save status response.
   */
  @JsonRequest("copilot/byok/saveModel")
  CompletableFuture<ByokStatusResponse> saveByokModel(ByokModel model);

  /**
   * Delete BYOK model.
   *
   * @param model the BYOK model to delete.
   * @return the BYOK delete status response.
   */
  @JsonRequest("copilot/byok/deleteModel")
  CompletableFuture<ByokStatusResponse> deleteByokModel(ByokModel model);

  /**
   * Save BYOK API key.
   *
   * @param apiKey the BYOK API key to save.
   * @return the BYOK save status response.
   */
  @JsonRequest("copilot/byok/saveApiKey")
  CompletableFuture<ByokStatusResponse> saveByokApiKey(ByokApiKey apiKey);

  /**
   * Delete BYOK API key.
   *
   * @param apiKey the BYOK API key to delete.
   * @return the BYOK delete status response.
   */
  @JsonRequest("copilot/byok/deleteApiKey")
  CompletableFuture<ByokStatusResponse> deleteByokApiKey(ByokApiKey apiKey);

  /**
   * List All BYOK API keys.
   *
   * @param apiKey the BYOK API key filter parameters.
   * @return the BYOK API key list response.
   */
  @JsonRequest("copilot/byok/listApiKeys")
  CompletableFuture<ByokListApiKeyResponse> listByokApiKeys(ByokApiKey apiKey);

  /**
   * Save a built-in BYOK provider configuration.
   */
  @JsonRequest("copilot/byok/saveProviderConfig")
  CompletableFuture<ByokStatusResponse> saveByokProviderConfig(ByokProviderConfig providerConfig);

  /**
   * Delete a built-in BYOK provider configuration.
   */
  @JsonRequest("copilot/byok/deleteProviderConfig")
  CompletableFuture<ByokStatusResponse> deleteByokProviderConfig(ByokDeleteProviderConfigParams params);

  /**
   * List built-in BYOK provider configurations.
   */
  @JsonRequest("copilot/byok/listProviderConfigs")
  CompletableFuture<ByokListProviderConfigResponse> listByokProviderConfigs(ByokListProviderConfigParams params);

  /**
   * Update the status of the mcp server and tools.
   *
   * @param param the MCP tool status update parameters.
   * @return the updated MCP server tool collections.
   */
  @JsonRequest("mcp/updateToolsStatus")
  CompletableFuture<List<McpServerToolsCollection>> updateMcpToolsStatus(UpdateMcpToolsStatusParams param);

  /**
   * Get the MCP server list.
   *
   * @param params the MCP server list request parameters.
   * @return the MCP server list.
   */
  @JsonRequest("mcp/registry/listServers")
  CompletableFuture<ServerList> listMcpServers(ListServersParams params);

  /**
   * Get the details of a specific MCP server.
   *
   * @param params the MCP server details request parameters.
   * @return the MCP server details response.
   */
  @JsonRequest("mcp/registry/getServer")
  CompletableFuture<ServerResponse> getMcpServer(GetServerParams params);

  /**
   * Get the MCP registry allowlist for the current user or organization.
   *
   * @param params the MCP allowlist request parameters.
   * @return the MCP registry allowlist.
   */
  @JsonRequest("mcp/registry/getAllowlist")
  CompletableFuture<McpRegistryAllowList> getMcpAllowlist(Object params);

  /**
   * Next Edit Suggestions request.
   *
   * @param params the next edit suggestion request parameters.
   * @return the next edit suggestion result.
   */
  @JsonRequest("textDocument/copilotInlineEdit")
  CompletableFuture<NextEditSuggestionsResult> getNextEditSuggestions(NextEditSuggestionsParams params);

  /**
   * Search GitHub Pull Requests.
   *
   * @param params the GitHub pull request search parameters.
   * @return the GitHub pull request search response.
   */
  @JsonRequest("githubApi/searchPR")
  CompletableFuture<SearchPrResponse> searchPr(SearchPrParams params);

  /**
   * Get the default file safety rules from CLS.
   *
   * @param params the empty default file safety rules request parameters.
   * @return the default file safety rules result.
   */
  @JsonRequest("getDefaultFileSafetyRules")
  CompletableFuture<GetDefaultFileSafetyRulesResult> getDefaultFileSafetyRules(
      NullParams params);

  /**
   * Notify that an inline edit was shown.
   *
   * @param params the inline edit shown notification parameters.
   */
  @JsonNotification("textDocument/didShowInlineEdit")
  void didShowInlineEdit(DidShowInlineEditParams params);
}
