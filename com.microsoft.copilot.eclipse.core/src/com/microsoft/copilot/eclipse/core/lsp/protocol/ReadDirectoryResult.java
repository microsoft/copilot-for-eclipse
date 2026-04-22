// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Result of reading a directory, containing the directory entries.
 */
public class ReadDirectoryResult {

  private List<DirectoryEntry> entries;

  /**
   * Constructor with entries.
   *
   * @param entries the directory entries
   */
  public ReadDirectoryResult(List<DirectoryEntry> entries) {
    this.entries = entries;
  }

  /**
   * Gets the directory entries.
   *
   * @return the directory entries
   */
  public List<DirectoryEntry> getEntries() {
    return entries;
  }

  /**
   * Sets the directory entries.
   *
   * @param entries the directory entries
   */
  public void setEntries(List<DirectoryEntry> entries) {
    this.entries = entries;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entries);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ReadDirectoryResult other = (ReadDirectoryResult) obj;
    return Objects.equals(entries, other.entries);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("entries", entries).toString();
  }

  /**
   * A single directory entry with name and file type.
   */
  public static class DirectoryEntry {

    /** VS Code FileType constants. */
    public static final int FILE_TYPE_UNKNOWN = 0;
    public static final int FILE_TYPE_FILE = 1;
    public static final int FILE_TYPE_DIRECTORY = 2;

    private String name;
    private int type;

    /**
     * Constructor with name and type.
     *
     * @param name the entry name
     * @param type the file type (0=Unknown, 1=File, 2=Directory)
     */
    public DirectoryEntry(String name, int type) {
      this.name = name;
      this.type = type;
    }

    /**
     * Gets the entry name.
     */
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    /**
     * Gets the file type.
     */
    public int getType() {
      return type;
    }

    public void setType(int type) {
      this.type = type;
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      DirectoryEntry other = (DirectoryEntry) obj;
      return Objects.equals(name, other.name) && type == other.type;
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this).append("name", name).append("type", type).toString();
    }
  }
}
