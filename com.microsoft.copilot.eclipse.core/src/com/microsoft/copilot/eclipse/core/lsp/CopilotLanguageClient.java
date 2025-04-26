package com.microsoft.copilot.eclipse.core.lsp;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.ProgressParams;
import org.eclipse.lsp4j.ShowDocumentParams;
import org.eclipse.lsp4j.ShowDocumentResult;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.chat.service.IChatServiceManager;
import com.microsoft.copilot.eclipse.core.chat.service.IReferencedFileService;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatProgressValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationContextParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CurrentEditorContext;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GetWatchedFilesRequest;
import com.microsoft.copilot.eclipse.core.lsp.protocol.GetWatchedFilesResponse;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolConfirmationParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InvokeClientToolParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Language client for the Copilot language server.
 */
@SuppressWarnings("restriction")
public class CopilotLanguageClient extends LanguageClientImpl {

  private WatchedFileManager watchedFileManager;

  private static final String SIGNUP_URL = "https://github.com/github-copilot/signup";

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
  public CompletableFuture<Object[]> getConversationContext(ConversationContextParams params) {
    switch (params.skillId()) {
      case ConversationCapabilities.CURRENT_EDITOR_SKILL:
        IChatServiceManager chatServiceManager = CopilotCore.getPlugin().getChatServiceManager();
        if (chatServiceManager == null) {
          CopilotCore.LOGGER.error(new IllegalStateException("Chat service manager is null"));
          break;
        }
        IReferencedFileService fileService = chatServiceManager.getReferencedFileService();
        if (fileService == null) {
          CopilotCore.LOGGER.error(new IllegalStateException("File service is null"));
          break;
        }

        IFile file = fileService.getCurrentFile();
        if (file == null) {
          break;
        }
        String uri = FileUtils.getResourceUri(file);
        return CompletableFuture.completedFuture(new Object[] { new CurrentEditorContext(uri), null });
      default:
        break;
    }
    return CompletableFuture.completedFuture(new Object[] { null, null });
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
   * Prompt for user confirmation before invoking a tool.
   */
  @JsonRequest("conversation/invokeClientToolConfirmation")
  public CompletableFuture<Object[]> confirmClientTool(InvokeClientToolConfirmationParams params) {
    return CopilotCore.getPlugin().getChatEventsManager().confirmAgentToolInvocation(params).thenApply(result -> {
      if (result == null) {
        return new Object[] { null,
            new ResponseError(ResponseErrorCode.InternalError, "Failed to get the confirmation from user.", null) };
      } else {
        return new Object[] { result, null };
      }
    }).exceptionally(e -> {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      CopilotCore.LOGGER.error(e);
      return new Object[] { null, new ResponseError(ResponseErrorCode.RequestFailed,
          "Failed to get the confirmation from user due to exception", cause) };
    });
  }

  @Override
  public CompletableFuture<List<WorkspaceFolder>> workspaceFolders() {
    // Ideally, we should return each IProject as a workspace folder, but given that when
    // creating a new conversation or new conversation turn, the uri of the workspace folder
    // is required to use the @project (or @workspace) agent. There is no easy way to guess which
    // IProject should be used. So we are returning the workspace root as a single workspace folder.
    final WorkspaceFolder folder = new WorkspaceFolder();
    folder.setUri(PlatformUtils.getWorkspaceRootUri());
    folder.setName("workspace-root"); // $NON-NLS-1$
    return CompletableFuture.completedFuture(List.of(folder));
  }

  /**
   * Get the conversation context for the given request.
   */
  @JsonRequest("copilot/watchedFiles")
  public CompletableFuture<GetWatchedFilesResponse> getWatchedFiles(GetWatchedFilesRequest params) {
    if (watchedFileManager == null) {
      watchedFileManager = new WatchedFileManager();
    }
    return CompletableFuture.completedFuture(new GetWatchedFilesResponse(watchedFileManager.getWatchedFiles(params)));
  }

  /**
   * Handles the progress notification for chat replies.
   */
  @Override
  public void notifyProgress(ProgressParams progress) {
    var chatProgress = (ChatProgressValue) progress.getValue().getLeft();
    CopilotCore.getPlugin().getChatEventsManager().notifyProgress(chatProgress);
  }

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
}
