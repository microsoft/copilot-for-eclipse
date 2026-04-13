package com.microsoft.copilot.eclipse.core.lsp.mcp;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * MCP server tools collection - includes all server features plus tools Note: Despite the name "Tools", this collection
 * includes all server features (tools, resources, resourceTemplates) for backward compatibility. This maintains
 * compatibility with existing getAllTools() method while supporting the expanded feature set.
 */
public class McpServerToolsCollection {
  private String name;

  private String prefix;

  private McpServerStatus status;

  private List<McpToolInformation> tools;

  private List<McpResource> resources;

  private List<McpResourceTemplate> resourceTemplates;

  private List<McpPrompt> prompts;

  private String error;

  private String registryInfo;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public McpServerStatus getStatus() {
    return status;
  }

  public void setStatus(McpServerStatus status) {
    this.status = status;
  }

  public List<McpToolInformation> getTools() {
    return tools;
  }

  public void setTools(List<McpToolInformation> tools) {
    this.tools = tools;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public List<McpResource> getResources() {
    return resources;
  }

  public void setResources(List<McpResource> resources) {
    this.resources = resources;
  }

  public List<McpResourceTemplate> getResourceTemplates() {
    return resourceTemplates;
  }

  public void setResourceTemplates(List<McpResourceTemplate> resourceTemplates) {
    this.resourceTemplates = resourceTemplates;
  }

  public List<McpPrompt> getPrompts() {
    return prompts;
  }

  public void setPrompts(List<McpPrompt> prompts) {
    this.prompts = prompts;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getRegistryInfo() {
    return registryInfo;
  }

  public void setRegistryInfo(String registryInfo) {
    this.registryInfo = registryInfo;
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, name, prefix, prompts, registryInfo, resourceTemplates, resources, status, tools);
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
    McpServerToolsCollection other = (McpServerToolsCollection) obj;
    return Objects.equals(error, other.error) && Objects.equals(name, other.name)
        && Objects.equals(prefix, other.prefix) && Objects.equals(prompts, other.prompts)
        && Objects.equals(registryInfo, other.registryInfo)
        && Objects.equals(resourceTemplates, other.resourceTemplates) && Objects.equals(resources, other.resources)
        && status == other.status && Objects.equals(tools, other.tools);
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("name", name);
    builder.append("prefix", prefix);
    builder.append("status", status);
    builder.append("tools", tools);
    builder.append("resources", resources);
    builder.append("resourceTemplates", resourceTemplates);
    builder.append("prompts", prompts);
    builder.append("error", error);
    builder.append("registryInfo", registryInfo);
    return builder.toString();
  }

}