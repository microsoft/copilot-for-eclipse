package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Copilot Capabilities of the Copilot language server.
 */
public class CopilotCapabilities {
  private boolean fetch;

  private boolean watchedFiles;
  
  private boolean didChangeFeatureFlags;

  /**
   * Creates a new CopilotCapabilities.
   */
  public CopilotCapabilities(boolean fetch, boolean watchedFiles) {
    this.didChangeFeatureFlags = true;
    this.fetch = fetch;
    this.watchedFiles = watchedFiles;
  }

  public boolean isFetch() {
    return fetch;
  }

  public void setFetch(boolean fetch) {
    this.fetch = fetch;
  }

  public boolean isWatchedFiles() {
    return watchedFiles;
  }

  public void setWatchedFiles(boolean watchedFiles) {
    this.watchedFiles = watchedFiles;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("fetch", fetch);
    builder.add("watchedFiles", watchedFiles);
    builder.add("didChangeFeatureFlags", didChangeFeatureFlags);
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(didChangeFeatureFlags, fetch, watchedFiles);
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
    CopilotCapabilities other = (CopilotCapabilities) obj;
    return didChangeFeatureFlags == other.didChangeFeatureFlags && fetch == other.fetch
        && watchedFiles == other.watchedFiles;
  }
}
