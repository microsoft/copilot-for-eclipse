// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.dialogs.mcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Argument;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.KeyValueInput;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.NamedArgument;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Package;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.PositionalArgument;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.Remote;
import com.microsoft.copilot.eclipse.core.lsp.mcp.registry.ServerDetail;

/**
 * Utility class for building MCP server configuration JSON objects. Contains common configuration building logic shared
 * between dialogs.
 */
public class McpServerConfigurationBuilder {

  /**
   * Creates a JSON configuration for a remote server.
   *
   * @param remote The remote server configuration
   * @param serverDetail The server detail (optional, for metadata)
   * @param mcpProviderUrl The MCP provider URL
   * @return JSON configuration object
   */
  public static JsonObject createRemoteServerConfiguration(Remote remote, ServerDetail serverDetail,
      String mcpProviderUrl) {
    JsonObject serverConfig = new JsonObject();
    serverConfig.addProperty("type", "http");
    serverConfig.addProperty("url", remote.url());

    // Handle optional headers for remote servers
    if (remote.headers() != null && !remote.headers().isEmpty()) {
      JsonObject requestInit = new JsonObject();
      JsonObject headersObject = new JsonObject();

      remote.headers().forEach(header -> {
        if (header.getName() != null && header.getValue() != null) {
          headersObject.addProperty(header.getName(), header.getValue());
        }
      });

      requestInit.add("headers", headersObject);
      serverConfig.add("requestInit", requestInit);
    }

    // Add x-metadata section with registry information if requested
    addMetadata(serverConfig, serverDetail, mcpProviderUrl);

    return serverConfig;
  }

  /**
   * Creates a JSON configuration for a package server.
   *
   * @param pkg The package configuration
   * @param serverDetail The server detail (optional, for metadata)
   * @param mcpRegistryBaseUrl The MCP Registry Base URL
   * @return JSON configuration object
   */
  public static JsonObject createPackageServerConfiguration(Package pkg, ServerDetail serverDetail,
      String mcpRegistryBaseUrl) {
    JsonObject serverConfig = new JsonObject();
    serverConfig.addProperty("type", "stdio");

    // Determine command: use runtimeHint if available, otherwise use registryType-based logic
    String command = pkg.runtimeHint() != null ? pkg.runtimeHint() : getCommandName(pkg.registryType());
    serverConfig.addProperty("command", command);

    // Handle arguments
    JsonArray argsArray = new JsonArray();

    // If runtime arguments exist, use them; otherwise, build arguments based on registry type
    if (pkg.runtimeArguments() != null && !pkg.runtimeArguments().isEmpty()) {
      for (Argument argument : pkg.runtimeArguments()) {
        for (String value : extractArgumentValues(argument)) {
          argsArray.add(value);
        }
      }
    } else {
      // Build arguments based on registry type
      for (String arg : buildDefaultArguments(pkg)) {
        argsArray.add(arg);
      }
    }

    // Add package arguments if they exist
    if (pkg.packageArguments() != null && !pkg.packageArguments().isEmpty()) {
      for (Argument argument : pkg.packageArguments()) {
        for (String value : extractArgumentValues(argument)) {
          argsArray.add(value);
        }
      }
    }

    serverConfig.add("args", argsArray);

    // Handle environment variables
    if (pkg.environmentVariables() != null && !pkg.environmentVariables().isEmpty()) {
      JsonObject envObject = new JsonObject();
      for (KeyValueInput envVar : pkg.environmentVariables()) {
        envObject.addProperty(envVar.getName(), envVar.getValue() != null ? envVar.getValue() : "");
      }
      serverConfig.add("env", envObject);
    }

    // Add x-metadata section with registry information if requested
    addMetadata(serverConfig, serverDetail, mcpRegistryBaseUrl);

    return serverConfig;
  }

  /**
   * Adds metadata section to server configuration.
   *
   * @param serverConfig The server configuration JSON object
   * @param serverDetail The server detail containing metadata
   * @param mcpRegistryBaseUrl The MCP Registry Base URL
   */
  public static void addMetadata(JsonObject serverConfig, ServerDetail serverDetail, String mcpRegistryBaseUrl) {
    JsonObject registry = new JsonObject();

    JsonObject api = new JsonObject();
    api.addProperty("baseUrl", mcpRegistryBaseUrl);
    api.addProperty("version", Constants.MCP_REGISTRY_VERSION);
    registry.add("api", api);

    JsonObject mcpServer = new JsonObject();
    mcpServer.addProperty("name", serverDetail.name());
    mcpServer.addProperty("version", serverDetail.version());
    registry.add("mcpServer", mcpServer);

    JsonObject metadata = new JsonObject();
    metadata.add("registry", registry);
    serverConfig.add("x-metadata", metadata);
  }

  /**
   * Maps registry types to command names.
   *
   * @param registryType The registry type
   * @return The corresponding command name, or unknown string if registryType is null
   */
  public static String getCommandName(String registryType) {
    if (StringUtils.isBlank(registryType)) {
      return "unknown";
    }

    switch (registryType) {
      case "npm":
        return "npx";
      case "oci":
        return "docker";
      case "pypi":
        return "uvx";
      case "nuget":
        return "dnx";
      default:
        return "unknown";
    }
  }

  /**
   * Builds default arguments for a package based on its registry type.
   *
   * @param pkg The package
   * @return List of default arguments
   */
  public static List<String> buildDefaultArguments(Package pkg) {
    List<String> args = new ArrayList<>();

    if (StringUtils.isBlank(pkg.registryType())) {
      args.add(buildVersionedIdentifier(pkg, "@"));
      return args;
    }

    switch (pkg.registryType()) {
      case "npm":
        args.add("-y");
        args.add(buildVersionedIdentifier(pkg, "@"));
        break;
      case "pypi":
        args.add(buildVersionedIdentifier(pkg, "=="));
        break;
      case "oci":
        args.add("run");
        args.add("-i");
        args.add("--rm");
        args.add(buildVersionedIdentifier(pkg, ":"));
        break;
      case "nuget":
        args.add(buildVersionedIdentifier(pkg, "@"));
        args.add("--yes");
        if (pkg.packageArguments() != null && !pkg.packageArguments().isEmpty()) {
          args.add("--");
        }
        break;
      default:
        args.add(buildVersionedIdentifier(pkg, "@"));
        break;
    }

    return args;
  }

  /**
   * Builds a versioned package identifier with the given separator.
   *
   * @param pkg The package
   * @param separator The separator between identifier and version
   * @return The versioned package identifier
   */
  private static String buildVersionedIdentifier(Package pkg, String separator) {
    return StringUtils.isNotBlank(pkg.version()) ? pkg.identifier() + separator + pkg.version() : pkg.identifier();
  }

  /**
   * Extracts argument values from an Argument object.
   *
   * @param argument The argument
   * @return List of argument values
   */
  public static List<String> extractArgumentValues(Argument argument) {
    if (argument instanceof PositionalArgument positionalArg) {
      // For positional arguments, use the value or fall back to valueHint
      String value = positionalArg.getValue() != null ? positionalArg.getValue() : positionalArg.getValueHint();
      return value != null ? Arrays.asList(value) : Collections.emptyList();
    } else if (argument instanceof NamedArgument namedArg) {
      // For named arguments, return flag name and value as separate elements to avoid JSON escaping
      List<String> values = new ArrayList<>();
      if (namedArg.getName() != null) {
        values.add(namedArg.getName());
        if (namedArg.getValue() != null) {
          values.add(namedArg.getValue());
        }
      }
      return values;
    }
    return Collections.emptyList();
  }
}