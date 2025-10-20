package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A prompt or prompt template that the server offers.
 */
public class McpPrompt {
  private String name;
  private String description;
  private String title;
  private List<McpPromptArgument> arguments;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<McpPromptArgument> getArguments() {
    return arguments;
  }

  public void setArguments(List<McpPromptArgument> arguments) {
    this.arguments = arguments;
  }

  @Override
  public int hashCode() {
    return Objects.hash(arguments, description, name, title);
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
    McpPrompt other = (McpPrompt) obj;
    return Objects.equals(arguments, other.arguments) && Objects.equals(description, other.description)
        && Objects.equals(name, other.name) && Objects.equals(title, other.title);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name);
    builder.append("description", description);
    builder.append("title", title);
    builder.append("arguments", arguments);
    return builder.toString();
  }

  private static class McpPromptArgument {
    private String name;
    private String description;
    private boolean required;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public boolean isRequired() {
      return required;
    }

    public void setRequired(boolean required) {
      this.required = required;
    }

    @Override
    public int hashCode() {
      return Objects.hash(description, name, required);
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
      McpPromptArgument other = (McpPromptArgument) obj;
      return Objects.equals(description, other.description) && Objects.equals(name, other.name)
          && required == other.required;
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("name", name);
      builder.append("description", description);
      builder.append("required", required);
      return builder.toString();
    }
  }
}
