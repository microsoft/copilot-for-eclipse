package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Information about a repository in the MCP Registry.
 */
public class Repository {
  private String url;
  private String source;
  private String id;
  private String subfolder;

  /**
   * Constructor for Repository.
   *
   * @param url The URL of the repository.
   * @param source The source of the repository.
   * @param id The unique identifier of the repository.
   * @param subfolder Optional relative path from repository root to the server location within a monorepo structure
   */
  public Repository(String url, String source, String id, String subfolder) {
    this.url = url;
    this.source = source;
    this.id = id;
    this.subfolder = subfolder;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSubfolder() {
    return subfolder;
  }

  public void setSubfolder(String subfolder) {
    this.subfolder = subfolder;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, source, subfolder, url);
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
    Repository other = (Repository) obj;
    return Objects.equals(id, other.id) && Objects.equals(source, other.source)
        && Objects.equals(subfolder, other.subfolder) && Objects.equals(url, other.url);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("url", url);
    builder.append("source", source);
    builder.append("id", id);
    builder.append("subfolder", subfolder);
    return builder.toString();
  }

}
