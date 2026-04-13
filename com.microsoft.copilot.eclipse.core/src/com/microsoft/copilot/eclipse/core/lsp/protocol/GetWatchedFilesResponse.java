package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Response to get the list of watched files. See:
 * https://github.com/microsoft/copilot-client/blob/main/agent/API_INTERNAL.md#copilotwatchedfiles
 */
public class GetWatchedFilesResponse {

  private List<String> files;

  /**
   * Gets the list of files.
   *
   * @param files the URI string list of files.
   */
  public GetWatchedFilesResponse(List<String> files) {
    this.files = files;
  }

  public List<String> getFiles() {
    return files;
  }

  public void setFiles(List<String> files) {
    this.files = files;
  }

  @Override
  public int hashCode() {
    return Objects.hash(files);
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
    GetWatchedFilesResponse other = (GetWatchedFilesResponse) obj;
    return Objects.equals(files, other.files);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("files", files);
    return builder.toString();
  }

}
