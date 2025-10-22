package com.microsoft.copilot.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.microsoft.copilot.eclipse.core.lsp.mcp.Argument;
import com.microsoft.copilot.eclipse.core.lsp.mcp.KeyValueInput;
import com.microsoft.copilot.eclipse.core.lsp.mcp.NamedArgument;
import com.microsoft.copilot.eclipse.core.lsp.mcp.Package;
import com.microsoft.copilot.eclipse.core.lsp.mcp.PositionalArgument;
import com.microsoft.copilot.eclipse.core.lsp.mcp.Remote;
import com.microsoft.copilot.eclipse.core.lsp.mcp.ServerDetail;

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
    serverConfig.addProperty("url", remote.getUrl());

    // Handle optional headers for remote servers
    if (remote.getHeaders() != null && !remote.getHeaders().isEmpty()) {
      JsonObject requestInit = new JsonObject();
      JsonObject headersObject = new JsonObject();

      remote.getHeaders().forEach(header -> {
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
   * @param mcpProviderUrl The MCP provider URL
   * @return JSON configuration object
   */
  public static JsonObject createPackageServerConfiguration(Package pkg, ServerDetail serverDetail,
      String mcpProviderUrl) {
    JsonObject serverConfig = new JsonObject();

    serverConfig.addProperty("type", "stdio");

    // Determine command: use runtimeHint if available, otherwise use registryType-based logic
    String command = pkg.getRuntimeHint() != null ? pkg.getRuntimeHint() : getCommandName(pkg.getRegistryType());
    serverConfig.addProperty("command", command);

    // Handle arguments
    JsonArray argsArray = new JsonArray();

    // If runtime arguments exist, use them; otherwise, build arguments based on registry type
    if (pkg.getRuntimeArguments() != null && !pkg.getRuntimeArguments().isEmpty()) {
      for (Argument argument : pkg.getRuntimeArguments()) {
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
    if (pkg.getPackageArguments() != null && !pkg.getPackageArguments().isEmpty()) {
      for (Argument argument : pkg.getPackageArguments()) {
        for (String value : extractArgumentValues(argument)) {
          argsArray.add(value);
        }
      }
    }

    serverConfig.add("args", argsArray);

    // Handle environment variables
    if (pkg.getEnvironmentVariables() != null && !pkg.getEnvironmentVariables().isEmpty()) {
      JsonObject envObject = new JsonObject();
      for (KeyValueInput envVar : pkg.getEnvironmentVariables()) {
        envObject.addProperty(envVar.getName(), envVar.getValue() != null ? envVar.getValue() : "");
      }
      serverConfig.add("env", envObject);
    }

    // Add x-metadata section with registry information if requested
    addMetadata(serverConfig, serverDetail, mcpProviderUrl);

    return serverConfig;
  }

  /**
   * Adds metadata section to server configuration.
   *
   * @param serverConfig The server configuration JSON object
   * @param serverDetail The server detail containing metadata
   * @param mcpProviderUrl The MCP provider URL
   */
  public static void addMetadata(JsonObject serverConfig, ServerDetail serverDetail, String mcpProviderUrl) {
    JsonObject metadata = new JsonObject();
    JsonObject registry = new JsonObject();
    registry.addProperty("url", mcpProviderUrl);
    String serverId = getServerId(serverDetail);
    if (serverId != null) {
      registry.addProperty("serverId", serverId);
    }
    metadata.add("registry", registry);
    serverConfig.add("x-metadata", metadata);
  }

  /**
   * Gets the server ID from server detail.
   *
   * @param serverDetail The server detail
   * @return The server ID or null if not available
   */
  public static String getServerId(ServerDetail serverDetail) {
    if (serverDetail.getMeta() != null && serverDetail.getMeta().getOfficial() != null) {
      return serverDetail.getMeta().getOfficial().getId();
    }
    return null;
  }

  /**
   * Maps registry types to command names.
   *
   * @param registryType The registry type
   * @return The corresponding command name
   */
  public static String getCommandName(String registryType) {
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
        return registryType;
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

    switch (pkg.getRegistryType()) {
      case "npm":
        args.add("-y");
        args.add(!pkg.getVersion().isEmpty() ? pkg.getIdentifier() + "@" + pkg.getVersion() : pkg.getIdentifier());
        break;
      case "pypi":
        args.add(!pkg.getVersion().isEmpty() ? pkg.getIdentifier() + "==" + pkg.getVersion() : pkg.getIdentifier());
        break;
      case "oci":
        args.add("run");
        args.add("-i");
        args.add("--rm");
        args.add(!pkg.getVersion().isEmpty() ? pkg.getIdentifier() + ":" + pkg.getVersion() : pkg.getIdentifier());
        break;
      case "nuget":
        args.add(!pkg.getVersion().isEmpty() ? pkg.getIdentifier() + "@" + pkg.getVersion() : pkg.getIdentifier());
        args.add("--yes");
        if (pkg.getPackageArguments() != null && !pkg.getPackageArguments().isEmpty()) {
          args.add("--");
        }
        break;
      default:
        args.add(!pkg.getVersion().isEmpty() ? pkg.getIdentifier() + "@" + pkg.getVersion() : pkg.getIdentifier());
        break;
    }

    return args;
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