package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Copilot Capabilities of the Copilot language server.
 */
public class CopilotCapabilities {
  private boolean fetch;

  private boolean watchedFiles;

  private boolean didChangeFeatureFlags;

  private boolean stateDatabase;

  /**
   * Creates a new CopilotCapabilities.
   */
  public CopilotCapabilities(boolean fetch, boolean watchedFiles) {
    this.didChangeFeatureFlags = true;
    this.fetch = fetch;
    this.watchedFiles = watchedFiles;
    this.stateDatabase = true;
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

  public void setStateDatabase(boolean stateDatabase) {
    this.stateDatabase = stateDatabase;
  }

  public boolean isStateDatabase() {
    return stateDatabase;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("fetch", fetch);
    builder.append("watchedFiles", watchedFiles);
    builder.append("didChangeFeatureFlags", didChangeFeatureFlags);
    builder.append("stateDatabase", stateDatabase);
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(didChangeFeatureFlags, fetch, stateDatabase, watchedFiles);
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
        && stateDatabase == other.stateDatabase && watchedFiles == other.watchedFiles;
  }
}
