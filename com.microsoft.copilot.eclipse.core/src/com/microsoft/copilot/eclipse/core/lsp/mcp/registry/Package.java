package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * An MCP package definition.
 *
 * @param registryType         The type of registry.
 * @param registryBaseUrl      The base URL of the registry.
 * @param identifier           The package identifier.
 * @param version              The package version.
 * @param fileSha256           The SHA256 hash of the package file.
 * @param runtimeHint          A hint about the runtime to use.
 * @param transport            The transport configuration.
 * @param runtimeArguments     The runtime arguments.
 * @param packageArguments     The package arguments.
 * @param environmentVariables The environment variables.
 */
public record Package(
    String registryType,
    String registryBaseUrl,
    String identifier,
    String version,
    String fileSha256,
    String runtimeHint,
    Transport transport,
    List<Argument> runtimeArguments,
    List<Argument> packageArguments,
    List<KeyValueInput> environmentVariables) {

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("registryType", registryType);
    builder.append("registryBaseUrl", registryBaseUrl);
    builder.append("identifier", identifier);
    builder.append("version", version);
    builder.append("fileSha256", fileSha256);
    builder.append("runtimeHint", runtimeHint);
    builder.append("transport", transport);
    builder.append("runtimeArguments", runtimeArguments);
    builder.append("packageArguments", packageArguments);
    builder.append("environmentVariables", environmentVariables);
    return builder.toString();
  }
}
