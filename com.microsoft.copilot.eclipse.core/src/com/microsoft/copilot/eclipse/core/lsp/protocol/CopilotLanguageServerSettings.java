package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Settings for the DidChangeConfigurationParams.
 */
public class CopilotLanguageServerSettings {
  private boolean showEditorCompletions;
  private Boolean enableAutoCompletions;

  /**
   * Http settings.
   */
  public static class Http {

    private String proxy;
    @SerializedName("proxyStrictSSL")
    private boolean proxyStrictSsl;
    private String proxyKerberosServicePrincipal;
    private String[] noProxy;

    /**
     * get proxy.
     *
     * @return the proxy
     */
    public String getProxy() {
      return proxy;
    }

    /**
     * set proxy.
     *
     * @param proxy the proxy to set
     */
    public void setProxy(String proxy) {
      this.proxy = proxy;
    }

    /**
     * is proxy strict ssl.
     *
     * @return the proxyStrictSsl
     */
    public boolean isProxyStrictSsl() {
      return proxyStrictSsl;
    }

    /**
     * set proxy strict ssl.
     *
     * @param proxyStrictSsl the proxyStrictSsl to set
     */
    public void setProxyStrictSsl(boolean proxyStrictSsl) {
      this.proxyStrictSsl = proxyStrictSsl;
    }

    /**
     * get proxy kerberos service principal.
     *
     * @return the proxyKerberosServicePrincipal
     */
    public String getProxyKerberosServicePrincipal() {
      return proxyKerberosServicePrincipal;
    }

    /**
     * set proxy kerberos service principal.
     *
     * @param proxyKerberosServicePrincipal the proxyKerberosServicePrincipal to set
     */
    public void setProxyKerberosServicePrincipal(String proxyKerberosServicePrincipal) {
      this.proxyKerberosServicePrincipal = proxyKerberosServicePrincipal;
    }

    public String[] getNoProxy() {
      return noProxy;
    }

    public void setNoProxy(String[] noProxy) {
      this.noProxy = noProxy;
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("proxy", proxy);
      builder.add("proxyStrictSsl", proxyStrictSsl);
      builder.add("proxyKerberosServicePrincipal", proxyKerberosServicePrincipal);
      builder.add("noProxy", noProxy);
      return builder.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(proxy, proxyKerberosServicePrincipal, proxyStrictSsl, noProxy);
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
      Http other = (Http) obj;
      return Objects.equals(proxy, other.proxy)
          && Objects.equals(proxyKerberosServicePrincipal, other.proxyKerberosServicePrincipal)
          && proxyStrictSsl == other.proxyStrictSsl
          && Arrays.equals(noProxy, other.noProxy);
    }

  }

  /**
   * Github Enterprise settings.
   */
  public static class GithubEnterprise {
    private String uri;

    /**
     * get Uri.
     *
     * @return the uri
     */
    public String getUri() {
      return uri;
    }

    /**
     * set Uri.
     *
     * @param uri the uri to set
     */
    public void setUri(String uri) {
      this.uri = uri;
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("uri", uri);
      return builder.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(uri);
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
      GithubEnterprise other = (GithubEnterprise) obj;
      return Objects.equals(uri, other.uri);
    }

  }

  /**
   * Github settings.
   */
  public static class GitHubSettings {
    @SerializedName("copilot")
    private CopilotSettings copilotSettings;

    /**
     * Constructor.
     */
    public GitHubSettings() {
      this.copilotSettings = new CopilotSettings();
    }

    public CopilotSettings getCopilotSettings() {
      return copilotSettings;
    }

    public void setCopilotSettings(CopilotSettings copilotSettings) {
      this.copilotSettings = copilotSettings;
    }

    /**
     * set workspace Copilot instructions.
     *
     * @param workspaceInstructions the workspace instructions to set
     */
    public void setWorkspaceCopilotInstructions(String workspaceInstructions) {
      this.copilotSettings.setWorkspaceCopilotInstructions(workspaceInstructions);
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("copilotSettings", copilotSettings);
      return builder.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(copilotSettings);
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
      GitHubSettings other = (GitHubSettings) obj;
      return Objects.equals(copilotSettings, other.copilotSettings);
    }
  }

  /**
   * Copilot settings.
   */
  public static class CopilotSettings {
    @SerializedName("mcp")
    private String mcpServers;

    @SerializedName("globalCopilotInstructions")
    private String workspaceCopilotInstructions;

    /**
     * Constructor.
     */
    public CopilotSettings() {
      this.mcpServers = null;
      this.workspaceCopilotInstructions = null;
    }

    public String getMcpServers() {
      return mcpServers;
    }

    public void setMcpServers(String mcpServers) {
      this.mcpServers = mcpServers;
    }

    public String getWorkspaceCopilotInstructions() {
      return workspaceCopilotInstructions;
    }

    public void setWorkspaceCopilotInstructions(String workspaceCopilotInstructions) {
      this.workspaceCopilotInstructions = workspaceCopilotInstructions;
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("mcpServers", mcpServers);
      builder.add("workspaceCopilotInstructions", workspaceCopilotInstructions);
      return builder.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(mcpServers, workspaceCopilotInstructions);
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
      CopilotSettings other = (CopilotSettings) obj;
      return Objects.equals(mcpServers, other.mcpServers)
          && Objects.equals(workspaceCopilotInstructions, other.workspaceCopilotInstructions);
    }
  }

  @SerializedName("github-enterprise")
  private GithubEnterprise githubEnterprise;
  private Http http;
  @SerializedName("github")
  private GitHubSettings githubSettings;

  /**
   * Constructor.
   */
  public CopilotLanguageServerSettings() {
    this.showEditorCompletions = true;
    this.enableAutoCompletions = true;
    this.http = new Http();
    this.githubEnterprise = new GithubEnterprise();
    this.githubSettings = new GitHubSettings();
  }

  /**
   * Constructor with parameters.
   */
  public CopilotLanguageServerSettings(@Nullable Boolean enableAutoCompletions, @Nullable Http http,
      @Nullable GithubEnterprise githubEnterprise, @Nullable GitHubSettings githubSettings) {
    this.showEditorCompletions = true;
    this.enableAutoCompletions = enableAutoCompletions;
    this.http = http;
    this.githubEnterprise = githubEnterprise;
    this.githubSettings = githubSettings;
  }

  /**
   * is show editor completions.
   *
   * @return the showEditorCompletions
   */
  public boolean isShowEditorCompletions() {
    return showEditorCompletions;
  }

  /**
   * set show editor completions.
   *
   * @param showEditorCompletions the showEditorCompletions to set
   */
  public void setShowEditorCompletions(boolean showEditorCompletions) {
    this.showEditorCompletions = showEditorCompletions;
  }

  /**
   * is enable auto completions.
   *
   * @return the enableAutoCompletions
   */
  public boolean isEnableAutoCompletions() {
    return enableAutoCompletions;
  }

  /**
   * set enable auto completions.
   *
   * @param enableAutoCompletions the enableAutoCompletions to set
   */
  public void setEnableAutoCompletions(boolean enableAutoCompletions) {
    this.enableAutoCompletions = enableAutoCompletions;
  }

  /**
   * get github enterprise.
   *
   * @return the githubEnterprise
   */
  public GithubEnterprise getGithubEnterprise() {
    return githubEnterprise;
  }

  /**
   * set github enterprise.
   *
   * @param githubEnterprise the githubEnterprise to set
   */
  public void setGithubEnterprise(GithubEnterprise githubEnterprise) {
    this.githubEnterprise = githubEnterprise;
  }

  /**
   * get http.
   *
   * @return the http
   */
  public Http getHttp() {
    return http;
  }

  /**
   * set http.
   *
   * @param http the http to set
   */
  public void setHttp(Http http) {
    this.http = http;
  }

  public GitHubSettings getGithubSettings() {
    return githubSettings;
  }

  public void setGithubSettings(GitHubSettings githubSettings) {
    this.githubSettings = githubSettings;
  }

  /**
   * set mcp servers.
   */
  public void setMcpServers(String mcpServersPreference) {
    String mcpServers = parseMcpServers(mcpServersPreference);
    this.getGithubSettings().getCopilotSettings().setMcpServers(mcpServers);
  }

  /**
   * add mcp servers.
   */
  public void addMcpServers(String mcpServersJson) {
    String mcpServers = parseMcpServers(mcpServersJson);
    if (StringUtils.isNotBlank(mcpServers)) {
      String existingMcpServers = this.getGithubSettings().getCopilotSettings().getMcpServers();
      if (StringUtils.isBlank(existingMcpServers)) {
        this.getGithubSettings().getCopilotSettings().setMcpServers(mcpServers);
      } else {
        // merge the existing and new MCP servers
        try {
          Gson gson = new Gson();
          Map<String, Object> existingMap = gson.fromJson(existingMcpServers, new TypeToken<Map<String, Object>>() {
          }.getType());
          Map<String, Object> newMap = gson.fromJson(mcpServers, new TypeToken<Map<String, Object>>() {
          }.getType());
          if (existingMap != null && newMap != null) {
            existingMap.putAll(newMap);
            String mergedMcpServers = gson.toJson(existingMap);
            this.getGithubSettings().getCopilotSettings().setMcpServers(mergedMcpServers);
          }
        } catch (JsonParseException e) {
          CopilotCore.LOGGER.error("Failed to parse MCP servers JSON during merge", e);
        }
      }
    }
  }

  private String parseMcpServers(String mcpServersPreference) {
    if (StringUtils.isBlank(mcpServersPreference)) {
      return mcpServersPreference;
    }

    try {
      Gson gson = new Gson();
      Map<String, Object> jsonMap = gson.fromJson(mcpServersPreference, new TypeToken<Map<String, Object>>() {
      }.getType());

      if (jsonMap != null && jsonMap.containsKey("servers")) {
        Object serversObj = jsonMap.get("servers");
        return gson.toJson(serversObj);
      }

      return mcpServersPreference;
    } catch (JsonParseException e) {
      CopilotCore.LOGGER.error("Failed to parse MCP servers JSON", e);
      return null;
    }
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("showEditorCompletions", showEditorCompletions);
    builder.add("enableAutoCompletions", enableAutoCompletions);
    builder.add("githubEnterprise", githubEnterprise);
    builder.add("http", http);
    builder.add("githubSettings", githubSettings);
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(enableAutoCompletions, githubSettings, githubEnterprise, http, showEditorCompletions);
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
    CopilotLanguageServerSettings other = (CopilotLanguageServerSettings) obj;
    return enableAutoCompletions == other.enableAutoCompletions && Objects.equals(githubSettings, other.githubSettings)
        && Objects.equals(githubEnterprise, other.githubEnterprise) && Objects.equals(http, other.http)
        && showEditorCompletions == other.showEditorCompletions;
  }

}