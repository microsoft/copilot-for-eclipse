// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A known MCP resource that the server is capable of reading.
 */
public class McpResource {
  private String name;
  private String title;
  private String url;
  private String description;
  private String mimeType;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, mimeType, name, title, url);
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
    McpResource other = (McpResource) obj;
    return Objects.equals(description, other.description) && Objects.equals(mimeType, other.mimeType)
        && Objects.equals(name, other.name) && Objects.equals(title, other.title) && Objects.equals(url, other.url);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name);
    builder.append("title", title);
    builder.append("url", url);
    builder.append("description", description);
    builder.append("mimeType", mimeType);
    return builder.toString();
  }

}
