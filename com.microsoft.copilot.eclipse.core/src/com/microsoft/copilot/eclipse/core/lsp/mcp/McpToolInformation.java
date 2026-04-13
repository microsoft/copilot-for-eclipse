package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;

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
    builder.append("status", status);
    builder.append("name", getName());
    builder.append("description", getDescription());
    builder.append("inputSchema", getInputSchema());
    builder.append("confirmationMessages", getConfirmationMessages());
    return builder.toString();
  }
}
