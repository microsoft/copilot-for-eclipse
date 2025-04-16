package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * See https://github.com/microsoft/copilot-client/blob/main/agent/API_INTERNAL.md#workspacedidchangewatchedfiles.
 */
public class DidChangeCopilotWatchedFilesParams extends DidChangeWatchedFilesParams {
  private String workspaceUri;

  /**
   * Constructor.
   */
  public DidChangeCopilotWatchedFilesParams(@NonNull final String workkpaceUri,
      @NonNull final List<FileEvent> changes) {
    super(changes);
    this.workspaceUri = workkpaceUri;
  }

  public String getWorkkpaceUri() {
    return workspaceUri;
  }

  public void setWorkkpaceUri(String workkpaceUri) {
    this.workspaceUri = workkpaceUri;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(workspaceUri);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DidChangeCopilotWatchedFilesParams other = (DidChangeCopilotWatchedFilesParams) obj;
    return Objects.equals(workspaceUri, other.workspaceUri);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("workkpaceUri", workspaceUri);
    builder.add("changes", getChanges());
    return builder.toString();
  }

}
