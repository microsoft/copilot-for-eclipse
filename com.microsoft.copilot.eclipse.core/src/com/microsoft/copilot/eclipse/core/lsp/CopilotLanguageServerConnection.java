package com.microsoft.copilot.eclipse.core.lsp;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpRegistryAllowList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.GetServerParams;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ListServersParams;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerList;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatCompletionContentPart;
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
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeCopilotWatchedFilesParams;
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
import com.microsoft.copilot.eclipse.core.lsp.protocol.TodoItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.Turn;
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
import com.microsoft.copilot.eclipse.core.utils.ChatMessageUtils;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Language Server for Copilot agent.
 */
@SuppressWarnings({ "restriction" })
public class CopilotLanguageServerConnection {

  public static final String SERVER_ID = "com.microsoft.copilot.eclipse.ls";

  private LanguageServerWrapper languageServerWrapper;

  /**
   * Constructor for the CopilotLanguageServer.
   *
   * @param languageServerWrapper the language server wrapper.
   */
  public CopilotLanguageServerConnection(LanguageServerWrapper languageServerWrapper) {
    this.languageServerWrapper = languageServerWrapper;
  }

  /**
   * Connect the document to the language server. The LSP4E will take care of all the document lifecycle events after
   * that.
   */
  public CompletableFuture<LanguageServerWrapper> connectDocument(IDocument document, IFile file) {
    try {
      return languageServerWrapper.connect(document, file);
    } catch (Exception e) {
      CopilotCore.LOGGER.error(e);
      return null;
    }
  }

  /**
   * Disconnect the document from the language server.
   */
  public void disconnectDocument(URI uri) {
    this.languageServerWrapper.disconnect(uri);
  }

  /**
   * Get the document version for the given URI.
   */
  public int getDocumentVersion(URI uri) {
    return this.languageServerWrapper.getTextDocumentVersion(uri);
  }

  /**
   * Check the login status for current machine.
   */
  public CompletableFuture<CopilotStatusResult> checkStatus(Boolean localCheckOnly) {
    Function<LanguageServer, CompletableFuture<CopilotStatusResult>> fn = server -> {
      CheckStatusParams param = new CheckStatusParams();
      param.setLocalChecksOnly(localCheckOnly);
      return ((CopilotLanguageServer) server).checkStatus(param);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Check the user's quota status.
   */
  public CompletableFuture<CheckQuotaResult> checkQuota() {
    Function<LanguageServer, CompletableFuture<CheckQuotaResult>> fn = server -> ((CopilotLanguageServer) server)
        .checkQuota(new NullParams());
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Get single completion for the given parameters.
   */
  public CompletableFuture<CompletionResult> getCompletions(CompletionParams params) {
    Function<LanguageServer, CompletableFuture<CompletionResult>> fn = server -> ((CopilotLanguageServer) server)
        .getCompletions(params);
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Update the configuration for the language server.
   */
  public void updateConfig(DidChangeConfigurationParams params) {
    this.languageServerWrapper.sendNotification(server -> server.getWorkspaceService().didChangeConfiguration(params));
  }

  /**
   * Please use the {@link CopilotStatusManager#signInInitiate()} method instead.
   * </p>
   * Initiate the sign in process.
   */
  public CompletableFuture<SignInInitiateResult> signInInitiate() {
    Function<LanguageServer, CompletableFuture<SignInInitiateResult>> fn = (server) -> ((CopilotLanguageServer) server)
        .signInInitiate(new NullParams());
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Please use the {@link AuthStatusManager#signInConfirm()} method instead.
   * </p>
   * Confirm the sign in process.
   */
  public CompletableFuture<CopilotStatusResult> signInConfirm(String userCode) {
    Function<LanguageServer, CompletableFuture<CopilotStatusResult>> fn = (server) -> {
      SignInConfirmParams param = new SignInConfirmParams(userCode);
      return ((CopilotLanguageServer) server).signInConfirm(param);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Please use the {@link AuthStatusManager#signOut()} method instead.
   * </p>
   * Sign out from the GitHub Copilot.
   */
  public CompletableFuture<CopilotStatusResult> signOut() {
    Function<LanguageServer, CompletableFuture<CopilotStatusResult>> fn = (server) -> ((CopilotLanguageServer) server)
        .signOut(new NullParams());
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Notify the language server that the completion was shown.
   */
  public CompletableFuture<String> notifyShown(NotifyShownParams params) {
    Function<LanguageServer, CompletableFuture<String>> fn = server -> ((CopilotLanguageServer) server)
        .notifyShown(params);
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Notify the language server that the completion was accepted.
   */
  public CompletableFuture<String> notifyAccepted(NotifyAcceptedParams params) {
    Function<LanguageServer, CompletableFuture<String>> fn = server -> ((CopilotLanguageServer) server)
        .notifyAccepted(params);
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Notify the language server that the completion was rejected.
   */
  public CompletableFuture<String> notifyRejected(NotifyRejectedParams params) {
    Function<LanguageServer, CompletableFuture<String>> fn = server -> ((CopilotLanguageServer) server)
        .notifyRejected(params);
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Send the exception telemetry to the language server.
   */
  public CompletableFuture<Object> sendExceptionTelemetry(Throwable ex) {
    TelemetryExceptionParams telemParams = new TelemetryExceptionParams(ex);
    Function<LanguageServer, CompletableFuture<Object>> fn = server -> ((CopilotLanguageServer) server)
        .sendExceptionTelemetry(telemParams);
    return this.languageServerWrapper.execute(fn).exceptionally(exception -> {
      // Ignore exceptions to avoid infinite loop.
      return null;
    });
  }

  /**
   * Create a conversation with the given parameters.
   */
  public CompletableFuture<ChatCreateResult> createConversation(String workDoneToken, String message,
      List<IResource> files, IFile currentFile, List<Turn> turns, CopilotModel activeModel, String chatModeName,
      String customChatModeId, List<TodoItem> todos) {
    return createConversation(workDoneToken, message, files, currentFile, null, turns, activeModel, chatModeName,
        customChatModeId, todos, null, null);
  }

  /**
   * Create a conversation with the given parameters and optional agentSlug, agentJobWorkspaceFolder.
   */
  public CompletableFuture<ChatCreateResult> createConversation(String workDoneToken, String message,
      List<IResource> files, IFile currentFile, List<Turn> turns, CopilotModel activeModel, String chatModeName,
      String customChatModeId, List<TodoItem> todos, String agentSlug, String agentJobWorkspaceFolder) {
    return createConversation(workDoneToken, message, files, currentFile, null, turns, activeModel, chatModeName,
        customChatModeId, todos, agentSlug, agentJobWorkspaceFolder);
  }

  /**
   * Create a conversation with the given parameters, including optional currentSelection from the editor.
   */
  public CompletableFuture<ChatCreateResult> createConversation(String workDoneToken, String message,
      List<IResource> files, IFile currentFile, Range currentSelection, List<Turn> turns, CopilotModel activeModel,
      String chatModeName, String customChatModeId, List<TodoItem> todos, String agentSlug,
      String agentJobWorkspaceFolder) {
    boolean supportVision = activeModel.getCapabilities().supports().vision();
    Either<String, List<ChatCompletionContentPart>> messageWithImages = ChatMessageUtils
        .createMessageWithImages(message, FileUtils.filterFilesFrom(files), supportVision);
    Function<LanguageServer, CompletableFuture<ChatCreateResult>> fn = server -> {
      ConversationCreateParams param = new ConversationCreateParams(messageWithImages, workDoneToken);
      param.setReferences(FileUtils.convertToChatReferences(files));
      param.setModel(getModelName(activeModel));
      param.setModelProviderName(activeModel.getProviderName());
      param.setChatMode(chatModeName);
      param.setCustomChatModeId(customChatModeId);

      if (StringUtils.isBlank(agentSlug)) {
        param.setWorkspaceFolder(PlatformUtils.getWorkspaceRootUri());
        param.setWorkspaceFolders(LSPEclipseUtils.getWorkspaceFolders());
        param.setTodoList(todos);
      } else {
        // Set agentSlug if provided - this will modify the first turn's agentSlug
        if (param.getTurns() != null && !param.getTurns().isEmpty()) {
          param.getTurns().get(0).setAgentSlug(agentSlug);
        }
        param.setWorkspaceFolder(agentJobWorkspaceFolder);
      }

      // Set historical turns if provided.
      if (turns != null && turns.size() > 0) {
        param.getTurns().addAll(turns);
      }

      // TODO: remove needToolCallConfirmation when CLS fully supports it across all IDEs.
      param.setNeedToolCallConfirmation(true);
      if (currentFile != null) {
        param.setTextDocument(new TextDocumentIdentifier(FileUtils.getResourceUri(currentFile)));
        if (currentSelection != null) {
          param.setSelection(currentSelection);
        }
      }
      return ((CopilotLanguageServer) server).create(param);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Create a conversation turn with the given parameters.
   */
  public CompletableFuture<ChatTurnResult> addConversationTurn(String workDoneToken, String conversationId,
      String message, List<IResource> files, IFile currentFile, CopilotModel activeModel, String chatModeName,
      String customChatModeId, List<TodoItem> todoList, String agentSlug, String agentJobWorkspaceFolder) {
    return addConversationTurn(workDoneToken, conversationId, message, files, currentFile, null, activeModel,
        chatModeName, customChatModeId, todoList, agentSlug, agentJobWorkspaceFolder);
  }

  /**
   * Create a conversation turn with the given parameters, including optional currentSelection from the editor.
   */
  public CompletableFuture<ChatTurnResult> addConversationTurn(String workDoneToken, String conversationId,
      String message, List<IResource> files, IFile currentFile, Range currentSelection, CopilotModel activeModel,
      String chatModeName, String customChatModeId, List<TodoItem> todoList, String agentSlug,
      String agentJobWorkspaceFolder) {
    boolean supportVision = activeModel.getCapabilities().supports().vision();
    Either<String, List<ChatCompletionContentPart>> messageWithImages = ChatMessageUtils
        .createMessageWithImages(message, FileUtils.filterFilesFrom(files), supportVision);
    Function<LanguageServer, CompletableFuture<ChatTurnResult>> fn = server -> {
      ConversationTurnParams param = new ConversationTurnParams(workDoneToken, conversationId, messageWithImages);
      param.setReferences(FileUtils.convertToChatReferences(files));
      param.setModel(getModelName(activeModel));
      param.setModelProviderName(activeModel.getProviderName());
      param.setChatMode(chatModeName);
      param.setCustomChatModeId(customChatModeId);

      if (StringUtils.isBlank(agentSlug)) {
        param.setWorkspaceFolder(PlatformUtils.getWorkspaceRootUri());
        param.setWorkspaceFolders(LSPEclipseUtils.getWorkspaceFolders());
        param.setTodoList(todoList);
      } else {
        param.setAgentSlug(agentSlug);
        param.setWorkspaceFolder(agentJobWorkspaceFolder);
      }

      // TODO: remove needToolCallConfirmation when CLS fully supports it across all IDEs.
      param.setNeedToolCallConfirmation(true);
      if (currentFile != null) {
        param.setTextDocument(new TextDocumentIdentifier(FileUtils.getResourceUri(currentFile)));
        if (currentSelection != null) {
          param.setSelection(currentSelection);
        }
      }

      return ((CopilotLanguageServer) server).addTurn(param);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * List the conversation templates.
   */
  public CompletableFuture<ConversationTemplate[]> listConversationTemplates() {
    Function<LanguageServer, CompletableFuture<ConversationTemplate[]>> fn = server -> {
      return ((CopilotLanguageServer) server).listTemplates(new NullParams());
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * List the conversation modes.
   */
  public CompletableFuture<ConversationMode[]> listConversationModes(ConversationModesParams params) {
    Function<LanguageServer, CompletableFuture<ConversationMode[]>> fn = server -> {
      return ((CopilotLanguageServer) server).listModes(params);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * List the conversation agents.
   */
  public CompletableFuture<ConversationAgent[]> listConversationAgents() {
    Function<LanguageServer, CompletableFuture<ConversationAgent[]>> fn = server -> {
      // return ((CopilotLanguageServer) server).listAgents(new NullParams());
      // Hard code the only supported @project agent. Should revert this when @github agent is supported.
      ConversationAgent project = new ConversationAgent();
      project.setSlug("project");
      project.setName("Project");
      project.setDescription("Ask about your project");
      project.setAvatarUrl(null);

      return CompletableFuture.completedFuture(new ConversationAgent[] { project });
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Used to track telemetry from users copying code from chat.
   */
  public CompletableFuture<String> codeCopy(ConversationCodeCopyParams params) {
    Function<LanguageServer, CompletableFuture<String>> fn = server -> ((CopilotLanguageServer) server)
        .copyCode(params);
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Used to get the persistence token for the current user.
   */
  public CompletableFuture<ChatPersistence> persistence() {
    Function<LanguageServer, CompletableFuture<ChatPersistence>> fn = server -> ((CopilotLanguageServer) server)
        .persistence(new NullParams());
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Used to register the tools for the language server.
   */
  public CompletableFuture<List<LanguageModelToolInformation>> registerTools(RegisterToolsParams params) {
    // @formatter:off
    Function<LanguageServer, CompletableFuture<List<LanguageModelToolInformation>>> fn =
        server -> ((CopilotLanguageServer) server).registerTools(params);
    // @formatter:on
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * List the copilot models.
   */
  public CompletableFuture<CopilotModel[]> listModels() {
    Function<LanguageServer, CompletableFuture<CopilotModel[]>> fn = server -> {
      return ((CopilotLanguageServer) server).listModels(new NullParams());
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Update the status of the mcp server and tools.
   */
  public CompletableFuture<List<McpServerToolsCollection>> updateMcpToolsStatus(UpdateMcpToolsStatusParams params) {
    // @formatter:off
    Function<LanguageServer, CompletableFuture<List<McpServerToolsCollection>>> fn =
        server -> ((CopilotLanguageServer) server).updateMcpToolsStatus(params);
    // @formatter:on
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Update the status of conversation tools (built-in tools for Agent mode).
   */
  public CompletableFuture<Object> updateConversationToolsStatus(UpdateConversationToolsStatusParams params) {
    Function<LanguageServer, CompletableFuture<Object>> fn = server -> ((CopilotLanguageServer) server)
        .updateConversationToolsStatus(params);
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Notify the language server that watched files have changed.
   */
  public void didChangeWatchedFiles(DidChangeCopilotWatchedFilesParams params) {
    this.languageServerWrapper.sendNotification(server -> server.getWorkspaceService().didChangeWatchedFiles(params));
  }

  /**
   * Send $/progress notification to the language server. Used for reporting partial results during long-running
   * operations like file indexing.
   */
  public CompletableFuture<Void> sendProgressNotification(ProgressParams progressParams) {
    Function<LanguageServer, CompletableFuture<Void>> fn = server -> {
      if (server instanceof Endpoint endpoint) {
        endpoint.notify("$/progress", progressParams);
      }
      return CompletableFuture.completedFuture(null);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Notify the language server about code acceptance.
   */
  public CompletableFuture<String> notifyCodeAcceptance(NotifyCodeAcceptanceParams params) {
    Function<LanguageServer, CompletableFuture<String>> fn = server -> ((CopilotLanguageServer) server)
        .notifyCodeAcceptance(params);
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Generate a commit message based on the provided parameters.
   */
  public CompletableFuture<GenerateCommitMessageResult> generateCommitMessage(GenerateCommitMessageParams params) {
    // @formatter:off
    Function<LanguageServer, CompletableFuture<GenerateCommitMessageResult>> fn =
        server -> ((CopilotLanguageServer) server).generateCommitMessage(params);
    // @formatter:on
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * List BYOK models.
   */
  public CompletableFuture<ByokListModelResponse> listByokModels(ByokListModelParams params) {
    Function<LanguageServer, CompletableFuture<ByokListModelResponse>> fn = server -> {
      return ((CopilotLanguageServer) server).listByokModels(params);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Save a BYOK model.
   */
  public CompletableFuture<ByokStatusResponse> saveByokModel(ByokModel model) {
    Function<LanguageServer, CompletableFuture<ByokStatusResponse>> fn = server -> {
      return ((CopilotLanguageServer) server).saveByokModel(model);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Delete a BYOK model.
   */
  public CompletableFuture<ByokStatusResponse> deleteByokModel(ByokModel model) {
    Function<LanguageServer, CompletableFuture<ByokStatusResponse>> fn = server -> {
      return ((CopilotLanguageServer) server).deleteByokModel(model);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * List all BYOK Api keys.
   */
  public CompletableFuture<ByokListApiKeyResponse> listByokApiKeys(ByokApiKey apiKey) {
    Function<LanguageServer, CompletableFuture<ByokListApiKeyResponse>> fn = server -> {
      return ((CopilotLanguageServer) server).listByokApiKeys(apiKey);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Save a BYOK API key.
   */
  public CompletableFuture<ByokStatusResponse> saveByokApiKey(ByokApiKey apiKey) {
    Function<LanguageServer, CompletableFuture<ByokStatusResponse>> fn = server -> {
      return ((CopilotLanguageServer) server).saveByokApiKey(apiKey);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Delete a BYOK API key.
   */
  public CompletableFuture<ByokStatusResponse> deleteByokApiKey(ByokApiKey apiKey) {
    Function<LanguageServer, CompletableFuture<ByokStatusResponse>> fn = server -> {
      return ((CopilotLanguageServer) server).deleteByokApiKey(apiKey);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Get the MCP server list.
   */
  public CompletableFuture<ServerList> listMcpServers(ListServersParams params) {
    Function<LanguageServer, CompletableFuture<ServerList>> fn = server -> ((CopilotLanguageServer) server)
        .listMcpServers(params);
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Get the details of a specific MCP server.
   */
  public CompletableFuture<ServerResponse> getMcpServer(GetServerParams params) {
    Function<LanguageServer, CompletableFuture<ServerResponse>> fn = server -> ((CopilotLanguageServer) server)
        .getMcpServer(params);
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Get the MCP registry allowlist for the current user or organization.
   */
  public CompletableFuture<McpRegistryAllowList> getMcpAllowlist(Object params) {
    Function<LanguageServer, CompletableFuture<McpRegistryAllowList>> fn = server -> ((CopilotLanguageServer) server)
        .getMcpAllowlist(params);
    return this.languageServerWrapper.execute(fn).exceptionally(ex -> {
      CopilotCore.LOGGER.error(ex);
      return null;
    });
  }

  /**
   * Get next edit suggestions (inline edit) for a position.
   */
  public CompletableFuture<NextEditSuggestionsResult> getNextEditSuggestions(NextEditSuggestionsParams params) {
    // @formatter:off
    Function<LanguageServer, CompletableFuture<NextEditSuggestionsResult>> fn =
        server -> ((CopilotLanguageServer) server).getNextEditSuggestions(params);
    // @formatter:on
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Search GitHub pull requests based on the given parameters.
   */
  public CompletableFuture<SearchPrResponse> searchPr(SearchPrParams params) {
    Function<LanguageServer, CompletableFuture<SearchPrResponse>> fn = server -> ((CopilotLanguageServer) server)
        .searchPr(params);
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Stop the language server.
   */
  public void stop() {
    this.languageServerWrapper.stop();
  }

  private String getModelName(CopilotModel activeModel) {
    return activeModel == null ? null
        : activeModel.isChatFallback() ? activeModel.getId() : activeModel.getModelFamily();
  }
}
