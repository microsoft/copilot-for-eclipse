package com.microsoft.copilot.eclipse.core.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.lsp.mcp.GetServerParams;
import com.microsoft.copilot.eclipse.core.lsp.mcp.ListServersParams;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpRegistryAllowList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.ServerDetail;
import com.microsoft.copilot.eclipse.core.lsp.mcp.ServerList;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatCreateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatTurnResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CheckStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationAgent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCodeCopyParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCreateParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationModesParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTurnParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
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
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokApiKey;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListApiKeyResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokListModelResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.byok.ByokModel;
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
   */
  @JsonRequest
  CompletableFuture<CopilotStatusResult> checkStatus(CheckStatusParams param);

  /**
   * Check the uesr's quota status.
   */
  @JsonRequest
  CompletableFuture<CheckQuotaResult> checkQuota(NullParams param);

  /**
   * Get single completion for the given parameters.
   */
  @JsonRequest
  CompletableFuture<CompletionResult> getCompletions(CompletionParams params);

  /**
   * Initiate the sign in process.
   */
  @JsonRequest
  CompletableFuture<SignInInitiateResult> signInInitiate(NullParams param);

  /**
   * Confirm the sign in process.
   */
  @JsonRequest
  CompletableFuture<CopilotStatusResult> signInConfirm(SignInConfirmParams param);

  /**
   * Sign out the current user.
   */
  @JsonRequest
  CompletableFuture<CopilotStatusResult> signOut(NullParams params);

  /**
   * Notify the language server that the completion was shown.
   */
  @JsonRequest
  CompletableFuture<String> notifyShown(NotifyShownParams params);

  /**
   * Notify the language server that the completion was accepted.
   */
  @JsonRequest
  CompletableFuture<String> notifyAccepted(NotifyAcceptedParams params);

  /**
   * Notify the language server that the completion was rejected.
   */
  @JsonRequest
  CompletableFuture<String> notifyRejected(NotifyRejectedParams params);

  /**
   * Send exception telemetry to github sentry.
   */
  @JsonRequest("telemetry/exception")
  CompletableFuture<Object> sendExceptionTelemetry(TelemetryExceptionParams params);

  /**
   * Create a new conversation.
   */
  @JsonRequest("conversation/create")
  CompletableFuture<ChatCreateResult> create(ConversationCreateParams param);

  /**
   * Create a new conversation.
   */
  @JsonRequest("conversation/turn")
  CompletableFuture<ChatTurnResult> addTurn(ConversationTurnParams param);

  /**
   * List conversation templates.
   */
  @JsonRequest("conversation/templates")
  CompletableFuture<ConversationTemplate[]> listTemplates(NullParams param);

  /**
   * List conversation modes.
   */
  @JsonRequest("conversation/modes")
  CompletableFuture<ConversationMode[]> listModes(ConversationModesParams params);

  /**
   * Used to track telemetry from users copying code from chat.
   */
  @JsonRequest("conversation/copyCode")
  CompletableFuture<String> copyCode(ConversationCodeCopyParams param);

  /**
   * Used to get the persistence token for the current user.
   */
  @JsonRequest("conversation/persistence")
  CompletableFuture<ChatPersistence> persistence(NullParams param);

  /**
   * Register agent tools to the language server.
   */
  @JsonRequest("conversation/registerTools")
  CompletableFuture<List<LanguageModelToolInformation>> registerTools(RegisterToolsParams params);

  /**
   * Update the status of conversation tools (built-in tools for Agent mode).
   */
  @JsonRequest("conversation/updateToolsStatus")
  CompletableFuture<Object> updateConversationToolsStatus(UpdateConversationToolsStatusParams params);

  /**
   * List copilot models.
   */
  @JsonRequest("copilot/models")
  CompletableFuture<CopilotModel[]> listModels(NullParams param);

  /**
   * Get the conversation agents.
   */
  @JsonRequest("conversation/agents")
  CompletableFuture<ConversationAgent[]> listAgents(NullParams params);

  /**
   * Notify the code acceptance.
   */
  @JsonRequest("conversation/notifyCodeAcceptance")
  CompletableFuture<String> notifyCodeAcceptance(NotifyCodeAcceptanceParams params);

  /**
   * Generate commit messages.
   */
  @JsonRequest("git/commitGenerate")
  CompletableFuture<GenerateCommitMessageResult> generateCommitMessage(GenerateCommitMessageParams params);

  /**
   * List BYOK models.
   */
  @JsonRequest("copilot/byok/listModels")
  CompletableFuture<ByokListModelResponse> listByokModels(ByokListModelParams params);

  /**
   * Save BYOK model.
   */
  @JsonRequest("copilot/byok/saveModel")
  CompletableFuture<ByokStatusResponse> saveByokModel(ByokModel model);

  /**
   * Delete BYOK model.
   */
  @JsonRequest("copilot/byok/deleteModel")
  CompletableFuture<ByokStatusResponse> deleteByokModel(ByokModel model);

  /**
   * Save BYOK API key.
   */
  @JsonRequest("copilot/byok/saveApiKey")
  CompletableFuture<ByokStatusResponse> saveByokApiKey(ByokApiKey apiKey);

  /**
   * Delete BYOK API key.
   */
  @JsonRequest("copilot/byok/deleteApiKey")
  CompletableFuture<ByokStatusResponse> deleteByokApiKey(ByokApiKey apiKey);

  /**
   * List All BYOK API keys.
   */
  @JsonRequest("copilot/byok/listApiKeys")
  CompletableFuture<ByokListApiKeyResponse> listByokApiKeys(ByokApiKey apiKey);

  /**
   * Update the status of the mcp server and tools.
   */
  @JsonRequest("mcp/updateToolsStatus")
  CompletableFuture<List<McpServerToolsCollection>> updateMcpToolsStatus(UpdateMcpToolsStatusParams param);

  /**
   * Get the MCP server list.
   */
  @JsonRequest("mcp/registry/listServers")
  CompletableFuture<ServerList> listMcpServers(ListServersParams params);

  /**
   * Get the details of a specific MCP server.
   */
  @JsonRequest("mcp/registry/getServer")
  CompletableFuture<ServerDetail> getMcpServer(GetServerParams params);

  /**
   * Get the MCP registry allowlist for the current user or organization.
   */
  @JsonRequest("mcp/registry/getAllowlist")
  CompletableFuture<McpRegistryAllowList> getMcpAllowlist(Object params);

  /**
   * Next Edit Suggestions request.
   */
  @JsonRequest("textDocument/copilotInlineEdit")
  CompletableFuture<NextEditSuggestionsResult> getNextEditSuggestions(NextEditSuggestionsParams params);

  /**
   * Search GitHub Pull Requests.
   */
  @JsonRequest("githubApi/searchPR")
  CompletableFuture<SearchPrResponse> searchPr(SearchPrParams params);
}
