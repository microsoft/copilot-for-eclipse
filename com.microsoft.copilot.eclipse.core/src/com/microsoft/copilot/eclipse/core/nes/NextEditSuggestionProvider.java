package com.microsoft.copilot.eclipse.core.nes;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsResult.CopilotInlineEdit;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;

/**
 * Provider to fetch Next Edit Suggestions (NES).
 */
public class NextEditSuggestionProvider {

  private static final int NES_TIMEOUT_MILLIS = 5000;
  private final Set<NextEditSuggestionListener> listeners = new LinkedHashSet<>();
  private CompletableFuture<NextEditSuggestionsResult> currentRequest;
  private final CopilotLanguageServerConnection lsConnection;
  
  /**
   * Construct a NextEditSuggestionProvider.
   */
  public NextEditSuggestionProvider(CopilotLanguageServerConnection ls) {
    this.lsConnection = ls;
  }

  /**
   * Add a listener to receive next edit suggestions.
   *
   * @param l the listener to add
   */
  public void addListener(NextEditSuggestionListener l) {
    if (l != null) {
      listeners.add(l);
    }
  }

  /**
   * Remove a listener.
   *
   * @param l the listener to remove
   */
  public void removeListener(NextEditSuggestionListener l) {
    if (l != null) {
      listeners.remove(l);
    }
  }

  /**
   * Fetch next edit suggestion for the given file and position.
   *
   * @param file the file to fetch suggestion for
   * @param position the position to fetch suggestion for
   */
  public void fetchSuggestion(IFile file, Position position) {
    if (file == null || position == null) {
      return;
    }
    cancelCurrentRequest();
    VersionedTextDocumentIdentifier doc = new VersionedTextDocumentIdentifier();
    String uriString = FileUtils.getResourceUri(file);
    doc.setUri(uriString);
    int currentVersion = lsConnection.getDocumentVersion(URI.create(uriString));
    doc.setVersion(currentVersion);
    NextEditSuggestionsParams params = new NextEditSuggestionsParams(doc, position);
    currentRequest = lsConnection.getNextEditSuggestions(params).orTimeout(NES_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

    currentRequest.thenAccept(result -> {
      if (currentRequest != null && !currentRequest.isCancelled()) {
        CopilotInlineEdit validEdit = null;
        if (result != null && result.getEdits() != null && !result.getEdits().isEmpty()) {
          CopilotInlineEdit edit = result.getEdits().get(0);
          if (edit != null) {
            validEdit = edit;
          }
        }
        for (NextEditSuggestionListener l : listeners) {
          l.onNextEditSuggestion(file, validEdit);
        }
      }
    }).exceptionally(ex -> {
      if (currentRequest != null && !currentRequest.isCancelled()) {
        CopilotCore.LOGGER.error(ex);
        // Always notify listeners with null on error/timeout
        for (NextEditSuggestionListener l : listeners) {
          l.onNextEditSuggestion(file, null);
        }
      }
      return null;
    });
  }

  /** Cancel the current request if any. */
  public void cancelCurrentRequest() {
    if (currentRequest != null && !currentRequest.isDone()) {
      currentRequest.cancel(true);
    }
    currentRequest = null;
  }
}
