package com.microsoft.copilot.eclipse.core.lsp;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.ShowDocumentParams;
import org.eclipse.lsp4j.ShowDocumentResult;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationContextResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Language client for the Copilot language server.
 */
@SuppressWarnings("restriction")
public class CopilotLanguageClient extends LanguageClientImpl {

  private static final String SIGNUP_URL = "https://github.com/github-copilot/signup";

  @Override
  public CompletableFuture<ShowDocumentResult> showDocument(ShowDocumentParams params) {
    if (params.getUri() != null && params.getUri().startsWith(LSPEclipseUtils.HTTP) && params.getSelection() == null) {
      // override the method to open the URL in the browser, ideally, core should not have UI dependencies,
      // TODO: we should figure out a way to move this to UI plugin.
      openLink(params.getUri());
      if (params.getUri().contains(SIGNUP_URL)) {
        // refresh the status after user signs up copilot
        Job job = new Job("Refreshing Copilot status") {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
            AuthStatusManager manager = CopilotCore.getPlugin().getAuthStatusManager();
            if (manager != null) {
              manager.checkStatus();
            }
            return Status.OK_STATUS;
          }
        };
        job.setSystem(true);
        job.setPriority(Job.LONG);
        job.schedule(5 * 1000L /* ms */);
      }

      return CompletableFuture.completedFuture(new ShowDocumentResult(true));
    } else {
      return super.showDocument(params);
    }
  }

  private static boolean openLink(String link) {
    String encodedUrl = PlatformUtils.escapeSpaceInUrl(link);
    IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
    try {
      IWebBrowser browser = browserSupport.createBrowser(IWorkbenchBrowserSupport.AS_EXTERNAL, null, null, null);
      browser.openURL(new URI(encodedUrl).toURL());
    } catch (Exception e) {
      CopilotCore.LOGGER.error(e);
      return false;
    }
    return true;
  }

  /**
   * Get the conversation context for the given request.
   */
  @JsonRequest("conversation/context")
  public CompletableFuture<ConversationContextResult[]> getConversationContext(Object params) {
    return CompletableFuture.completedFuture(new ConversationContextResult[] { null, null });
  }

  /**
   * Invokes a client tool from the server.
   */
  @JsonRequest("conversation/invokeClientTool")
  public CompletableFuture<Object> invokeClientTool(InvokeClientToolParams params) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        LanguageModelToolResult[] toolResult = CopilotCore.getPlugin().getChatEventsManager().invokeAgentTool(params)
            .get();
        return toolResult;
      } catch (InterruptedException | ExecutionException e) {
        CopilotCore.LOGGER.error(e);
        return new String[] { "Failed to invoke the tool due to exception: " + e.getMessage() };
      }
    });
  }

  /**
   * Handles the progress notification for chat replies.
   */
  @Override
  public void notifyProgress(ProgressParams progress) {
    var chatProgress = (ChatProgressValue) progress.getValue().getLeft();
    CopilotCore.getPlugin().getChatEventsManager().notifyProgress(chatProgress);
  }
}
