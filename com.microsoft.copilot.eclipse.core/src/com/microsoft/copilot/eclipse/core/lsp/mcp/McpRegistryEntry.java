package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Represents an entry in the MCP Registry allowlist. Each entry defines a registry URL, its access permissions, and
 * ownership information. This controls which MCP registries are accessible and what level of access is granted.
 */
public class McpRegistryEntry {
  public String url;

  /**
   * The access level granted for this registry. Defines the scope of access: {@code "registry_only"} - Access limited
   * to registry operations only</li> {@code "allow_all"} - Full access to all registry features</li>
   */
  @SerializedName("registry_access")
  public RegistryAccess registryAccess;

  /**
   * Information about the owner of this registry entry.
   */
  public McpRegistryOwner owner;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public RegistryAccess getRegistryAccess() {
    return registryAccess;
  }

  public void setRegistryAccess(RegistryAccess registryAccess) {
    this.registryAccess = registryAccess;
  }

  public McpRegistryOwner getOwner() {
    return owner;
  }

  public void setOwner(McpRegistryOwner owner) {
    this.owner = owner;
  }

  @Override
  public int hashCode() {
    return Objects.hash(owner, registryAccess, url);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof McpRegistryEntry)) {
      return false;
    }
    McpRegistryEntry other = (McpRegistryEntry) obj;
    return Objects.equals(owner, other.owner) && Objects.equals(registryAccess, other.registryAccess)
        && Objects.equals(url, other.url);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("url", url);
    builder.append("registryAccess", registryAccess);
    builder.append("owner", owner);
    return builder.toString();
  }

}
