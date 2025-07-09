package com.microsoft.copilot.eclipse.core.lsp;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatCreateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatTurnResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CheckStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationAgent;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCodeCopyParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCreateParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTurnParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeCopilotWatchedFilesParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.McpServerToolsCollection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyAcceptedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyCodeAcceptanceParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyRejectedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyShownParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NullParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.RegisterToolsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInConfirmParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.TelemetryExceptionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.UpdateMcpToolsStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.git.GenerateCommitMessageParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.git.GenerateCommitMessageResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.quota.CheckQuotaResult;
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
    } catch (IOException e) {
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
  public CompletableFuture<ChatCreateResult> createConversation(String workDoneToken, String message, List<IFile> files,
      IFile currentFile, String modelName, String chatModeName) {
    Function<LanguageServer, CompletableFuture<ChatCreateResult>> fn = server -> {
      ConversationCreateParams param = new ConversationCreateParams(message, workDoneToken);
      param.setWorkspaceFolder(PlatformUtils.getWorkspaceRootUri());
      param.setWorkspaceFolders(LSPEclipseUtils.getWorkspaceFolders());
      param.addFileRefs(files);
      param.setModel(modelName);
      param.setChatMode(chatModeName);
      // TODO: remove needToolCallConfirmation when CLS fully supports it across all IDEs.
      param.setNeedToolCallConfirmation(true);
      if (currentFile != null) {
        param.setTextDocument(new TextDocumentIdentifier(FileUtils.getResourceUri(currentFile)));
      }
      return ((CopilotLanguageServer) server).create(param);
    };
    return this.languageServerWrapper.execute(fn);
  }

  /**
   * Create a conversation with the given parameters.
   */
  public CompletableFuture<ChatTurnResult> addConversationTurn(String workDoneToken, String conversationId,
      String message, List<IFile> files, IFile currentFile, String modelName, String chatModeName) {
    Function<LanguageServer, CompletableFuture<ChatTurnResult>> fn = server -> {
      ConversationTurnParams param = new ConversationTurnParams(workDoneToken, conversationId, message);
      param.addFileRefs(files);
      param.setModel(modelName);
      param.setChatMode(chatModeName);
      param.setWorkspaceFolder(PlatformUtils.getWorkspaceRootUri());
      param.setWorkspaceFolders(LSPEclipseUtils.getWorkspaceFolders());
      // TODO: remove needToolCallConfirmation when CLS fully supports it across all IDEs.
      param.setNeedToolCallConfirmation(true);
      if (currentFile != null) {
        param.setTextDocument(new TextDocumentIdentifier(FileUtils.getResourceUri(currentFile)));
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
  public CompletableFuture<String> registerTools(RegisterToolsParams params) {
    Function<LanguageServer, CompletableFuture<String>> fn = server -> ((CopilotLanguageServer) server)
        .registerTools(params);
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
   * Notify the language server that watched files have changed.
   */
  public void didChangeWatchedFiles(DidChangeCopilotWatchedFilesParams params) {
    this.languageServerWrapper.sendNotification(server -> server.getWorkspaceService().didChangeWatchedFiles(params));
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
   * Stop the language server.
   */
  public void stop() {
    this.languageServerWrapper.stop();
  }

}
