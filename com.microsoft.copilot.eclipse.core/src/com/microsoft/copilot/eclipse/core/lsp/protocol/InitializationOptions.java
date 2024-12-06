package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * Initialization options for the Copilot language server.
 */
public class InitializationOptions {
  @NonNull
  private NameAndVersion editorInfo;

  @NonNull
  private NameAndVersion editorPluginInfo;

  private CopilotCapabilities capabilities;

  /**
   * Creates a new InitializationOptions.
   */
  public InitializationOptions(NameAndVersion editorInfo, NameAndVersion editorPluginInfo) {
    this.editorInfo = editorInfo;
    this.editorPluginInfo = editorPluginInfo;
  }

  /**
   * Creates a new InitializationOptions.
   */
  public InitializationOptions(NameAndVersion editorInfo, NameAndVersion editorPluginInfo,
      CopilotCapabilities capabilities) {
    this.editorInfo = editorInfo;
    this.editorPluginInfo = editorPluginInfo;
    this.capabilities = capabilities;
  }

  @NonNull
  public NameAndVersion getEditorInfo() {
    return editorInfo;
  }

  public void setEditorInfo(@NonNull NameAndVersion editorInfo) {
    this.editorInfo = editorInfo;
  }

  @NonNull
  public NameAndVersion getEditorPluginInfo() {
    return editorPluginInfo;
  }

  public void setEditorPluginInfo(@NonNull NameAndVersion editorPluginInfo) {
    this.editorPluginInfo = editorPluginInfo;
  }

  public CopilotCapabilities getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(CopilotCapabilities capabilities) {
    this.capabilities = capabilities;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("editorInfo", editorInfo);
    builder.add("editorPluginInfo", editorPluginInfo);
    builder.add("capabilities", capabilities);
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(capabilities, editorInfo, editorPluginInfo);
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
    InitializationOptions other = (InitializationOptions) obj;
    return Objects.equals(capabilities, other.capabilities) && Objects.equals(editorInfo, other.editorInfo)
        && Objects.equals(editorPluginInfo, other.editorPluginInfo);
  }
}
