// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Detailed information about a server from the MCP Registry.
 *
 * @param name        The name of the server.
 * @param description The description of the server.
 * @param title       The title of the server.
 * @param repository  The repository information.
 * @param version     The version of the server.
 * @param websiteUrl  The website URL of the server.
 * @param icons       The icons associated with the server.
 * @param schema      The JSON schema reference.
 * @param packages    The packages available for the server.
 * @param remotes     The remote configurations for the server.
 * @param meta        The metadata about the server.
 */
public record ServerDetail(
    String name,
    String description,
    String title,
    Repository repository,
    String version,
    String websiteUrl,
    List<Icon> icons,
    @SerializedName("$schema") String schema,
    List<Package> packages,
    List<Remote> remotes,
    @SerializedName("_meta") ServerDetailMeta meta) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name);
    builder.append("description", description);
    builder.append("title", title);
    builder.append("repository", repository);
    builder.append("version", version);
    builder.append("websiteUrl", websiteUrl);
    builder.append("icons", icons);
    builder.append("schema", schema);
    builder.append("packages", packages);
    builder.append("remotes", remotes);
    builder.append("meta", meta);
    return builder.toString();
  }
}
