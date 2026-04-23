// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result of the {@code workspace/findFiles} request, containing URIs of files matching the glob pattern.
 */
public class FindFilesResult {

  private List<String> uris;

  /**
   * Constructs a new FindFilesResult object.
   *
   * @param uris the list of file URIs matching the glob pattern
   */
  public FindFilesResult(List<String> uris) {
    this.uris = uris;
  }

  public List<String> getUris() {
    return uris;
  }

  public void setUris(List<String> uris) {
    this.uris = uris;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uris);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FindFilesResult other = (FindFilesResult) obj;
    return Objects.equals(uris, other.uris);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("uris", uris);
    return builder.toString();
  }

}
