package com.microsoft.copilot.eclipse.core.lsp.protocol;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Settings for the DidChangeConfigurationParams.
 */
public class CopilotLanguageServerSettings {
  private boolean showEditorCompletions;
  private boolean enableAutoCompletions;

  /**
   * Http settings.
   */
  public class Http {

    private String proxy;
    @SerializedName("proxyStrictSSL")
    private boolean proxyStrictSsl;
    private String proxyKerberosServicePrincipal;


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

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.add("proxy", proxy);
      builder.add("proxyStrictSsl", proxyStrictSsl);
      builder.add("proxyKerberosServicePrincipal", proxyKerberosServicePrincipal);
      return builder.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(proxy, proxyKerberosServicePrincipal, proxyStrictSsl);
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
          && proxyStrictSsl == other.proxyStrictSsl;
    }

  }

  /**
   * Github Enterprise settings.
   */
  public class GithubEnterprise {
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

  @SerializedName("github-enterprise")
  private GithubEnterprise githubEnterprise;
  private Http http;

  /**
   * Constructor.
   */
  public CopilotLanguageServerSettings() {
    this.showEditorCompletions = true;
    this.enableAutoCompletions = true;
    this.http = new Http();
    this.githubEnterprise = new GithubEnterprise();
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

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.add("showEditorCompletions", showEditorCompletions);
    builder.add("enableAutoCompletions", enableAutoCompletions);
    builder.add("githubEnterprise", githubEnterprise);
    builder.add("http", http);
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(enableAutoCompletions, githubEnterprise, http, showEditorCompletions);
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
    return enableAutoCompletions == other.enableAutoCompletions
        && Objects.equals(githubEnterprise, other.githubEnterprise) && Objects.equals(http, other.http)
        && showEditorCompletions == other.showEditorCompletions;
  }

}
