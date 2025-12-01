package com.microsoft.copilot.eclipse.core.lsp.mcp.registry;

import java.util.List;

import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

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
    builder.add("registryType", registryType);
    builder.add("registryBaseUrl", registryBaseUrl);
    builder.add("identifier", identifier);
    builder.add("version", version);
    builder.add("fileSha256", fileSha256);
    builder.add("runtimeHint", runtimeHint);
    builder.add("transport", transport);
    builder.add("runtimeArguments", runtimeArguments);
    builder.add("packageArguments", packageArguments);
    builder.add("environmentVariables", environmentVariables);
    return builder.toString();
  }
}
