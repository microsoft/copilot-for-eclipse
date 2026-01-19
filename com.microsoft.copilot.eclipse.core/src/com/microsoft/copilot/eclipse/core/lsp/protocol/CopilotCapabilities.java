package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

import com.microsoft.copilot.eclipse.core.utils.JdtUtils;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Copilot Capabilities of the Copilot language server.
 */
public class CopilotCapabilities {
  private boolean fetch;

  private boolean watchedFiles;

  private boolean didChangeFeatureFlags;

  private boolean stateDatabase;

  private boolean subAgent;

  private boolean cveRemediatorAgent;

  private boolean debuggerAgent;

  /**
   * Creates a new CopilotCapabilities.
   */
  public CopilotCapabilities(boolean fetch, boolean watchedFiles, boolean subAgent) {
    this.didChangeFeatureFlags = true;
    this.stateDatabase = true;
    this.cveRemediatorAgent = true;
    this.debuggerAgent = JdtUtils.isJdtDebugAvailable() && PlatformUtils.isNightly();
    this.fetch = fetch;
    this.watchedFiles = watchedFiles;
    this.subAgent = subAgent;
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

  public boolean isSubAgent() {
    return subAgent;
  }

  public void setSubAgent(boolean subAgent) {
    this.subAgent = subAgent;
  }

  public boolean isCveRemediatorAgent() {
    return cveRemediatorAgent;
  }

  public void setCveRemediatorAgent(boolean cveRemediatorAgent) {
    this.cveRemediatorAgent = cveRemediatorAgent;
  }

  public boolean isDebuggerAgent() {
    return debuggerAgent;
  }

  public void setDebuggerAgent(boolean debuggerAgent) {
    this.debuggerAgent = debuggerAgent;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("fetch", fetch);
    builder.add("watchedFiles", watchedFiles);
    builder.add("didChangeFeatureFlags", didChangeFeatureFlags);
    builder.add("stateDatabase", stateDatabase);
    builder.add("subAgent", subAgent);
    builder.add("cveRemediatorAgent", cveRemediatorAgent);
    builder.add("debuggerAgent", debuggerAgent);
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(cveRemediatorAgent, debuggerAgent, didChangeFeatureFlags, fetch, stateDatabase, subAgent,
        watchedFiles);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof CopilotCapabilities)) {
      return false;
    }
    CopilotCapabilities other = (CopilotCapabilities) obj;
    return cveRemediatorAgent == other.cveRemediatorAgent && debuggerAgent == other.debuggerAgent
        && didChangeFeatureFlags == other.didChangeFeatureFlags && fetch == other.fetch
        && stateDatabase == other.stateDatabase && subAgent == other.subAgent && watchedFiles == other.watchedFiles;
  }
}
