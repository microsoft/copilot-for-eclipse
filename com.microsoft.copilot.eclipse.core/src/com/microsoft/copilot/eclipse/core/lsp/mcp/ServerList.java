package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A list of MCP servers from the MCP Registry.
 */
public class ServerList {
  private List<ServerDetail> servers;
  private Metadata metadata;

  /**
   * Constructor.
   */
  public ServerList(List<ServerDetail> servers) {
    this.servers = servers;
  }

  /**
   * Constructor with metadata.
   */
  public ServerList(List<ServerDetail> servers, Metadata metadata) {
    this.servers = servers;
    this.metadata = metadata;
  }

  public List<ServerDetail> getServers() {
    return servers;
  }

  public void setServers(List<ServerDetail> servers) {
    this.servers = servers;
  }

  public Metadata getMetadata() {
    return metadata;
  }

  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, servers);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ServerList)) {
      return false;
    }
    ServerList other = (ServerList) obj;
    return Objects.equals(metadata, other.metadata) && Objects.equals(servers, other.servers);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("servers", servers);
    builder.append("metadata", metadata);
    return builder.toString();
  }

  /**
   * Metadata about the server list, such as pagination info.
   */
  public static class Metadata {
    @SerializedName("next_cursor")
    private String nextCursor;

    @SerializedName("count")
    private int count;

    /**
     * Default constructor for JSON deserialization.
     */
    public Metadata() {
    }

    /**
     * Constructor.
     *
     * @param nextCursor The cursor for the next page of results, or null if there are no more results.
     * @param count The total number of servers available.
     */
    public Metadata(String nextCursor, int count) {
      this.nextCursor = nextCursor;
      this.count = count;
    }

    // Getters and setters
    public String getNextCursor() {
      return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
      this.nextCursor = nextCursor;
    }

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    @Override
    public int hashCode() {
      return Objects.hash(count, nextCursor);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof Metadata)) {
        return false;
      }
      Metadata other = (Metadata) obj;
      return Objects.equals(count, other.count) && Objects.equals(nextCursor, other.nextCursor);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("nextCursor", nextCursor);
      builder.append("count", count);
      return builder.toString();
    }

  }
}
