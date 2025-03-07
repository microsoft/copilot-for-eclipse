package com.microsoft.copilot.eclipse.core.lsp;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatCreateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatPersistence;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatTurnResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CheckStatusParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCodeCopyParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCreateParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTemplate;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationTurnParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyAcceptedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyRejectedParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NotifyShownParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NullParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInConfirmParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.SignInInitiateResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.TelemetryExceptionParams;

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
   * List copilot models.
   */
  @JsonRequest("copilot/models")
  CompletableFuture<CopilotModel[]> listModels(NullParams param);

}
