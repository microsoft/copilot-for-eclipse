package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Request to get the list of watched files. See:
 * https://github.com/microsoft/copilot-client/blob/main/agent/API_INTERNAL.md#copilotwatchedfiles
 */
public class GetWatchedFilesRequest {

  private String workspaceUri;

  private boolean excludeGitignoredFiles;

  private boolean excludeIdeIgnoredFiles;
  
  /**
   * An optional token for reporting partial results via $/progress notifications.
   */
  private Either<String, Integer> partialResultToken;

  public String getWorkspaceUri() {
    return workspaceUri;
  }

  public void setWorkspaceUri(String uri) {
    this.workspaceUri = uri;
  }

  public boolean isExcludeGitignoredFiles() {
    return excludeGitignoredFiles;
  }

  public void setExcludeGitignoredFiles(boolean excludeGitignoredFiles) {
    this.excludeGitignoredFiles = excludeGitignoredFiles;
  }

  public boolean isExcludeIdeIgnoredFiles() {
    return excludeIdeIgnoredFiles;
  }

  public void setExcludeIdeIgnoredFiles(boolean excludeIdeIgnoredFiles) {
    this.excludeIdeIgnoredFiles = excludeIdeIgnoredFiles;
  }
  
  public Either<String, Integer> getPartialResultToken() {
    return partialResultToken;
  }
  
  public void setPartialResultToken(Either<String, Integer> partialResultToken) {
    this.partialResultToken = partialResultToken;
  }

  @Override
  public int hashCode() {
    return Objects.hash(excludeGitignoredFiles, excludeIdeIgnoredFiles, workspaceUri, partialResultToken);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    GetWatchedFilesRequest other = (GetWatchedFilesRequest) obj;
    return excludeGitignoredFiles == other.excludeGitignoredFiles
        && excludeIdeIgnoredFiles == other.excludeIdeIgnoredFiles 
        && Objects.equals(workspaceUri, other.workspaceUri)
        && Objects.equals(partialResultToken, other.partialResultToken);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("uri", workspaceUri);
    builder.add("excludeGitignoredFiles", excludeGitignoredFiles);
    builder.add("excludeIdeIgnoredFiles", excludeIdeIgnoredFiles);
    builder.add("partialResultToken", partialResultToken);
    return builder.toString();
  }

}
