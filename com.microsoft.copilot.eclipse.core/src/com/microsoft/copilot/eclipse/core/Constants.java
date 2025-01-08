package com.microsoft.copilot.eclipse.core;

/**
 * A class to hold all the public constants used in the GitHub Copilot core.
 */
public class Constants {

  private Constants() {
    // prevent instantiation
  }

  public static final String PLUGIN_ID = "com.microsoft.copilot.eclipse";
  public static final String AUTO_SHOW_COMPLETION = "enableAutoCompletions";
  public static final String ENABLE_STRICT_SSL = "enableStrictSsl";
  public static final String PROXY_KERBEROS_SP = "proxyKerberosSp";
  public static final String GITHUB_ENTERPRISE = "githubEnterprise";
  public static final String GITHUB_COPILOT_URL = "http://github.com";
}
