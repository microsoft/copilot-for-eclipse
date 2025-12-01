package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import com.google.gson.annotations.SerializedName;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Metadata about the server.
 *
 * @param publisherProvided The publisher-provided metadata.
 */
public record ServerDetailMeta(
    @SerializedName("io.modelcontextprotocol.registry/publisher-provided") PublisherProvided publisherProvided) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("publisherProvided", publisherProvided);
    return builder.toString();
  }

  /**
   * Metadata provided by the server publisher.
   *
   * @param tool      The tool used to build the server.
   * @param version   The version of the tool.
   * @param buildInfo The build information.
   */
  public record PublisherProvided(String tool, String version, BuildInfo buildInfo) {

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("tool", tool);
      builder.add("version", version);
      builder.add("buildInfo", buildInfo);
      return builder.toString();
    }
  }

  /**
   * Build information about the server.
   *
   * @param commit     The commit hash.
   * @param timestamp  The build timestamp.
   * @param pipelineId The pipeline ID.
   */
  public record BuildInfo(String commit, String timestamp, String pipelineId) {

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("commit", commit);
      builder.add("timestamp", timestamp);
      builder.add("pipelineId", pipelineId);
      return builder.toString();
    }
  }
}
