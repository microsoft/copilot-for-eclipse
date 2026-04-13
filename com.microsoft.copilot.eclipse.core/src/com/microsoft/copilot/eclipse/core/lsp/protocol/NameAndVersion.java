package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;

/**
 * A name and version pair.
 */
public class NameAndVersion {
  @NonNull
  private String name;

  @NonNull
  private String version;

  /**
   * Creates a new NameAndVersion.
   *
   * @param name the name.
   * @param version the version.
   */
  public NameAndVersion(@NonNull String name, @NonNull String version) {
    this.name = Objects.requireNonNull(name, "name");
    this.version = Objects.requireNonNull(version, "version");
  }

  @NonNull
  public String getName() {
    return name;
  }

  public void setName(@NonNull String name) {
    this.name = name;
  }

  @NonNull
  public String getVersion() {
    return version;
  }

  public void setVersion(@NonNull String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.append("name", name);
    b.append("version", version);
    return b.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, version);
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
    NameAndVersion other = (NameAndVersion) obj;
    return Objects.equals(name, other.name) && Objects.equals(version, other.version);
  }
}
