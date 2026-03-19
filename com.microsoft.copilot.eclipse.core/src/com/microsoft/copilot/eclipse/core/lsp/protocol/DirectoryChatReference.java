package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.core.resources.IFolder;

import com.microsoft.copilot.eclipse.core.utils.FileUtils;

/**
 * Represents a directory chat reference.
 */
public class DirectoryChatReference implements ChatReference {
  private final String type = ReferenceType.directory.toString();
  private String uri;

  /**
   * Constructs a DirectoryChatReference with the specified URI.
   *
   * @param folder The folder for which the reference is created.
   */
  public DirectoryChatReference(IFolder folder) {
    this.uri = FileUtils.getResourceUri(folder);
  }

  public String getType() {
    return type;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, uri);
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
    DirectoryChatReference other = (DirectoryChatReference) obj;
    return Objects.equals(type, other.type) && Objects.equals(uri, other.uri);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("type", type);
    builder.append("text", uri);
    return builder.toString();
  }
}
