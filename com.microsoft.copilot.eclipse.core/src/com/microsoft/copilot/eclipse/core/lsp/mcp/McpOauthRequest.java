package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Represents a request for Dynamic OAuth authentication in the MCP.
 */
public class McpOauthRequest {

  private String title;

  private String header;

  private String detail;

  private List<DynamicOauthInput> inputs;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getHeader() {
    return header;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public List<DynamicOauthInput> getInputs() {
    return inputs;
  }

  public void setInputs(List<DynamicOauthInput> inputs) {
    this.inputs = inputs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, header, detail, inputs);
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
    McpOauthRequest other = (McpOauthRequest) obj;
    return Objects.equals(title, other.title)
        && Objects.equals(header, other.header)
        && Objects.equals(detail, other.detail)
        && Objects.equals(inputs, other.inputs);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("title", title);
    builder.add("header", header);
    builder.add("detail", detail);
    builder.add("inputs", inputs);
    return builder.toString();
  }

  /**
   * Represents an input field for Dynamic OAuth authentication.
   */
  public static class DynamicOauthInput {
    private String title;
    private String value;
    private String description;
    private String placeholder;
    private Boolean required;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getPlaceholder() {
      return placeholder;
    }

    public void setPlaceholder(String placeholder) {
      this.placeholder = placeholder;
    }

    public Boolean getRequired() {
      return required;
    }

    public void setRequired(Boolean required) {
      this.required = required;
    }

    @Override
    public int hashCode() {
      return Objects.hash(title, value, description, placeholder, required);
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
      DynamicOauthInput other = (DynamicOauthInput) obj;
      return Objects.equals(title, other.title)
          && Objects.equals(value, other.value)
          && Objects.equals(description, other.description)
          && Objects.equals(placeholder, other.placeholder)
          && Objects.equals(required, other.required);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("title", title);
      builder.add("value", value);
      builder.add("description", description);
      builder.add("placeholder", placeholder);
      builder.add("required", required);
      return builder.toString();
    }
  }
}
