package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Information about the MCP tool. Referenced by McpServerToolsCollection.
 */
public class McpToolInformation extends LanguageModelToolInformation {

  @SerializedName("_status")
  private McpToolStatus status;

  public McpToolStatus getStatus() {
    return status;
  }

  public void setStatus(McpToolStatus status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(status);
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
    McpToolInformation other = (McpToolInformation) obj;
    return Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("status", status);
    builder.add("name", getName());
    builder.add("description", getDescription());
    builder.add("inputSchema", getInputSchema());
    builder.add("confirmationMessages", getConfirmationMessages());
    return builder.toString();
  }
}
