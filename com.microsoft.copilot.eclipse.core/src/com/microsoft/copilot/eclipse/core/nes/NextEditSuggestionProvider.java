// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.nes;

import java.net.URI;
import java.util.Collections;
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
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsResult.CopilotInlineEdit;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;

/**
 * Provider to fetch Next Edit Suggestions (NES).
 */
public class NextEditSuggestionProvider {

  private static final int NES_TIMEOUT_MILLIS = 5000;
  private final Set<NextEditSuggestionListener> listeners = Collections.synchronizedSet(new LinkedHashSet<>());
  private final CopilotLanguageServerConnection lsConnection;
  private volatile CompletableFuture<Void> currentRequest;

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

    currentRequest = lsConnection.getNextEditSuggestions(params).orTimeout(NES_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        .handle((result, ex) -> {
          CopilotInlineEdit validEdit = null;
          if (ex != null) {
            CopilotCore.LOGGER.error("NES request failed", ex);
          } else if (result != null && result.getEdits() != null && !result.getEdits().isEmpty()) {
            CopilotInlineEdit edit = result.getEdits().get(0);
            if (edit != null) {
              validEdit = edit;
            }
          }
          // Notify listeners
          for (NextEditSuggestionListener l : listeners) {
            try {
              l.onNextEditSuggestion(file, validEdit);
            } catch (Exception e) {
              CopilotCore.LOGGER.error("Listener notification failed", e);
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
  }

  /**
   * Check if there is a request currently in progress (including listener notification).
   *
   * @return true if a request is pending (not yet completed), false otherwise
   */
  public boolean hasRequestInProgress() {
    return currentRequest != null && !currentRequest.isDone();
  }
}
