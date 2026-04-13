package com.microsoft.copilot.eclipse.core.logger;

import java.io.IOException;
import java.net.NetworkInterface;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.SSLContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.eclipse.core.net.proxy.IProxyData;
import org.osgi.framework.Bundle;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.TelemetryExceptionParams;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * The panic error report for Github.
 */
public class GithubPanicErrorReport {
  private static final String PANIC_ENDPOINT = "https://copilot-telemetry.githubusercontent.com/telemetry";
  private static final Gson GSON = new GsonBuilder().create();
  private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSX")
      .withZone(ZoneOffset.UTC);
  public static final String PLUGIN_VERSION = "editor_plugin_version";
  public static final String VERSION = "editor_version";
  private static String sessionId = UUID.randomUUID().toString();
  private static BasicHttpClientConnectionManager connectionManager = null;
  private IProxyData proxyData;
  private boolean proxyStrictSsl;

  public void setProxyData(IProxyData proxyData) {
    this.proxyData = proxyData;
  }

  public void setProxyStrictSsl(boolean proxyStrictSsl) {
    this.proxyStrictSsl = proxyStrictSsl;
  }

  /**
   * The message.
   *
   * @throws IOException dsfdsg.
   */
  public void report(Throwable ex) {
    // payload
    TelemetryExceptionParams params = new TelemetryExceptionParams(ex);
    var failbotPayload = Map.of("context", Map.of(), "app", "copilot-eclipse", "catalog_service", "CopilotEclipse",
        "release", "copilot-eclipse@" + PlatformUtils.getBundleVersion(), "rollup_id", "auto", "platform", "eclipse",
        "exception_detail", params.getExceptionDetail());
    var payload = createExceptionPayload(TelemetryChannel.Standard, ex,
        Map.of("failbot_payload", GSON.toJson(failbotPayload)));
    String payloadStr = GSON.toJson(new Object[] { payload });

    // build client
    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    clientBuilder = configureHttpClient(clientBuilder, proxyData, proxyStrictSsl);
    CloseableHttpClient client = clientBuilder.build();

    // send panic report
    ClassicHttpRequest req = ClassicRequestBuilder.post(PANIC_ENDPOINT).addHeader("accept", "application/json")
        .addHeader("Content-Type", "application/json").setEntity(payloadStr).build();
    try {
      client.execute(req, response -> {
        if (response.getCode() != 200) {
          throw new RuntimeException("Failed to send panic report: " + response.getCode());
        }
        return null;
      });
    } catch (IOException e) {
      // do nothing if panic log fails.
    }
  }

  private static HttpClientBuilder configureHttpClient(HttpClientBuilder builder, IProxyData proxyData,
      boolean proxyStrictSsl) {
    if (proxyData == null || proxyData.getHost().isEmpty()) {
      return builder;
    }

    HttpHost proxy = new HttpHost(proxyData.getType(), proxyData.getHost(), proxyData.getPort());
    builder = builder.setProxy(proxy);
    if (proxyStrictSsl) {
      return builder;
    }
    if (connectionManager != null) {
      return builder.setConnectionManager(connectionManager);
    }
    try {
      TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
      SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(null, acceptingTrustStrategy).build();
      SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, (hostname, session) -> true);
      Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
          .register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();

      connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry);
      builder = builder.setConnectionManager(connectionManager);
    } catch (KeyManagementException e) {
      // do nothing if panic log fails.
    } catch (NoSuchAlgorithmException e) {
      // do nothing if panic log fails.
    } catch (KeyStoreException e) {
      // do nothing if panic log fails.
    }
    return builder;
  }

  private static Map<String, Object> createExceptionPayload(TelemetryChannel channel, Throwable throwable,
      Map<String, String> properties) {
    HashMap<String, String> dataProperties = new HashMap<>();
    dataProperties.putAll(CopilotTelemetryContext.staticProperties);
    dataProperties.putAll(properties);
    return createPayload(channel, "Error",
        Map.of(
            "baseType", "ExceptionData", "baseData", Map.of("ver", 2, "severityLevel", "Error", "exceptions",
                new ArrayList<String>(), "name", "agent/error.exception", "properties", dataProperties),
            "measurements", Map.of()));
  }

  /**
   * Computes the file name from the stack trace element.
   *
   * @param element the stack trace element
   * @return the computed file name
   */
  public static String computeFileName(StackTraceElement element) {
    String[] classNameParts = element.getClassName().split("\\.");
    classNameParts[classNameParts.length - 1] = element.getFileName();
    return String.join("/", classNameParts);
  }

  private static Map<String, Object> createPayload(TelemetryChannel channel, String severityLevel,
      Map<String, Object> dataProperties) {
    return Map.of("ver", 1, "time", dateFormat.format(Instant.now()), "severityLevel", severityLevel, "name",
        "Microsoft.ApplicationInsights.standard.Event", "iKey", channel.getKey(), "data", dataProperties);
  }

  /**
   * The telemetry channel.
   */
  public enum TelemetryChannel {
    Standard, Restricted;

    String getKey() {
      switch (this) {
        case Standard:
          return "7d7048df-6dd0-4048-bb23-b716c1461f8f";
        case Restricted:
          return "3fdd7f28-937a-48c8-9a21-ba337db23bd1";
        default:
          throw new IllegalStateException("Unsupported value: " + this);
      }
    }
  }

  final class CopilotTelemetryContext {

    private CopilotTelemetryContext() {
    }

    static final Map<String, String> staticProperties;

    static {
      HashMap<String, String> properties = new HashMap<>();
      // machine id
      properties.put("common_vscodemachineid", getMacMachineId());
      properties.put("client_machineid", getMacMachineId());

      // // session
      properties.put("common_vscodesessionid", sessionId);
      properties.put("client_sessionid", sessionId);

      // os
      properties.put("common_os", vscodeOsName());
      properties.put("common_platformversion", getOsVersion());
      properties.put("common_uikind", "desktop");

      // ide version
      properties.put(VERSION, "Eclipse/" + PlatformUtils.getEclipseVersionString());
      properties.put("common_extname", "copilot-eclipse");

      String bundleVersion = PlatformUtils.getBundleVersion();
      properties.put("common_extversion", bundleVersion);
      properties.put(PLUGIN_VERSION, "copilot-eclipse/" + bundleVersion);
      // build
      properties.put("copilot_build", build());
      properties.put("copilot_buildType", String.valueOf(isDeveloperMode()));

      staticProperties = Collections.unmodifiableMap(properties);
    }

    /**
     * Mimics VSCode's OS name.
     */
    private static String vscodeOsName() {
      if (PlatformUtils.isWindows()) {
        return "win32";
      }
      if (PlatformUtils.isLinux()) {
        return "linux";
      }
      if (PlatformUtils.isMac()) {
        return "darwin";
      }
      return "unknown";
    }

    static boolean isDeveloperMode() {
      return true;
    }

    /**
     * Mimics VSCode's build property.
     */
    static String build() {
      if (isDeveloperMode()) {
        return "dev";
      }
      return "";
    }

  }

  private static String getMacMachineId() {
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        byte[] mac = iface.getHardwareAddress();
        if (mac != null && mac.length > 0) {
          MessageDigest digest = MessageDigest.getInstance("SHA-256");
          digest.update(mac);

          // Convert the hash to a hexadecimal string
          byte[] hashBytes = digest.digest();
          StringBuilder sb = new StringBuilder();
          for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
          }
          return sb.toString();
        }
      }
    } catch (Throwable t) {
      // do nothing because nothing can handle this exception when panic
      // throw new RuntimeException(t);
      return "unknown";
    }
    // do nothing because nothing can handle this exception when panic
    // throw new RuntimeException("Failed to retrieve MAC address");
    return "unknown";
  }

  private static String getOsVersion() {
    String version = System.getProperty("os.version").toLowerCase(Locale.ENGLISH);
    return version;
  }
}
