// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.terminal.api;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.utils.PlatformUtils;

/**
 * Central manager for terminal services. Handles service discovery, bundle installation.
 */
public class TerminalServiceManager {

  private static final String TERMINAL_BUNDLES_PATH = "terminal-bundles/";
  private static final String MODERN_TERMINAL_BUNDLE_JAR = "com.microsoft.copilot.eclipse.ui.terminal.jar";
  private static final String TM_TERMINAL_BUNDLE_JAR = "com.microsoft.copilot.eclipse.ui.terminal.tm.jar";
  private static final String MODERN_TERMINAL_BUNDLE_ID = "com.microsoft.copilot.eclipse.ui.terminal";
  private static final String TM_TERMINAL_BUNDLE_ID = "com.microsoft.copilot.eclipse.ui.terminal.tm";
  private static final String MODERN_TERMINAL_TYPE = "Terminal Feature";
  private static final String TM_TERMINAL_TYPE = "TM Terminal";
  private static final Version TARGET_PLATFORM_VERSION = new Version(4, 37, 0);

  /**
   * Static inner class for lazy initialization of singleton instance.
   */
  private static class InstanceHolder {
    private static final TerminalServiceManager INSTANCE = createInstance();

    private static TerminalServiceManager createInstance() {
      Bundle apiBundle = FrameworkUtil.getBundle(TerminalServiceManager.class);
      if (apiBundle != null) {
        BundleContext context = apiBundle.getBundleContext();
        if (context != null) {
          TerminalServiceManager instance = new TerminalServiceManager(context);
          instance.start();
          return instance;
        }
      }
      return null;
    }
  }

  private final BundleContext bundleContext;
  private ServiceTracker<IRunInTerminalTool, IRunInTerminalTool> serviceTracker;
  private final AtomicBoolean installationAttempted = new AtomicBoolean(false);
  private final CopyOnWriteArrayList<TerminalServiceListener> listeners = new CopyOnWriteArrayList<>();
  private volatile IRunInTerminalTool currentService = null;
  private volatile List<String> cachedMissingDependencies = null;
  private final boolean isModernTerminal;

  /**
   * Interface for listening to terminal service availability changes.
   */
  public interface TerminalServiceListener {
    /**
     * Called when a terminal service becomes available.
     *
     * @param service the available terminal service
     */
    void onServiceAvailable(IRunInTerminalTool service);

    /**
     * Called when terminal dependencies are missing and need user action.
     *
     * @param terminalType the type of terminal (e.g., "Terminal Feature" or "TM Terminal")
     * @param missingDependencies list of missing bundle symbolic names
     */
    void onMissingDependencies(String terminalType, List<String> missingDependencies);
  }

  private TerminalServiceManager(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
    this.isModernTerminal = detectModernTerminal();
  }

  /**
   * Get the singleton instance of TerminalServiceManager.
   */
  public static TerminalServiceManager getInstance() {
    return InstanceHolder.INSTANCE;
  }

  /**
   * Add a listener for terminal service events.
   */
  public void addListener(TerminalServiceListener listener) {
    if (listener != null) {
      listeners.add(listener);

      // If service is already available, notify immediately
      if (currentService != null) {
        listener.onServiceAvailable(currentService);
      }

      // Notify with cached missing dependencies if available
      if (cachedMissingDependencies != null && !cachedMissingDependencies.isEmpty()) {
        listener.onMissingDependencies(isModernTerminal ? MODERN_TERMINAL_TYPE : TM_TERMINAL_TYPE,
            cachedMissingDependencies);
      }
    }
  }

  /**
   * Remove a listener for terminal service events.
   */
  public void removeListener(TerminalServiceListener listener) {
    listeners.remove(listener);
  }

  /**
   * Get the current terminal service if available.
   */
  public IRunInTerminalTool getCurrentService() {
    return currentService;
  }

  /**
   * Start tracking terminal services.
   */
  private void start() {
    ServiceTrackerCustomizer<IRunInTerminalTool, IRunInTerminalTool> customizer = new ServiceTrackerCustomizer<>() {
      @Override
      public IRunInTerminalTool addingService(ServiceReference<IRunInTerminalTool> reference) {
        IRunInTerminalTool service = bundleContext.getService(reference);
        if (service != null) {
          currentService = service;
          notifyServiceAvailable(service);
        }
        return service;
      }

      @Override
      public void modifiedService(ServiceReference<IRunInTerminalTool> reference, IRunInTerminalTool service) {
        if (service != null) {
          currentService = service;
          notifyServiceAvailable(service);
        }
      }

      @Override
      public void removedService(ServiceReference<IRunInTerminalTool> reference, IRunInTerminalTool service) {
      }
    };

    serviceTracker = new ServiceTracker<>(bundleContext, IRunInTerminalTool.class, customizer);
    serviceTracker.open();

    IRunInTerminalTool[] services = serviceTracker.getServices(new IRunInTerminalTool[0]);
    if (services != null && services.length > 0) {
      currentService = services[0];
    }
    // Check if bundle needs to be installed or updated
    attemptTerminalBundleInstallation();
  }

  /**
   * Stop the service manager.
   */
  public void stop() {
    if (serviceTracker != null) {
      serviceTracker.close();
      serviceTracker = null;
    }
    currentService = null;
    listeners.clear();
  }

  private void notifyServiceAvailable(IRunInTerminalTool service) {
    for (TerminalServiceListener listener : listeners) {
      listener.onServiceAvailable(service);
    }
  }

  private void notifyMissingDependencies(String terminalType, List<String> missingDependencies) {
    for (TerminalServiceListener listener : listeners) {
      listener.onMissingDependencies(terminalType, missingDependencies);
    }
  }

  /**
   * Attempt to install or update terminal bundle. Skips if bundle is already installed with sufficient version.
   */
  private void attemptTerminalBundleInstallation() {
    if (!installationAttempted.compareAndSet(false, true)) {
      return;
    }

    Job installationJob = new Job("Installing Terminal Bundle") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Installing terminal bundle", 100);
        try {
          // Step 1: Check if installation/update is needed
          monitor.subTask("Checking existing installation");
          String symbolicName = isModernTerminal ? MODERN_TERMINAL_BUNDLE_ID : TM_TERMINAL_BUNDLE_ID;
          Bundle existingBundle = getInstalledBundle(symbolicName);
          Version requiredVersion = getEmbeddedBundleVersion();

          if (existingBundle != null) {
            Version installedVersion = existingBundle.getVersion();
            boolean needsUpdate = requiredVersion != null && installedVersion.compareTo(requiredVersion) < 0;

            if (!needsUpdate) {
              // Already up-to-date, just ensure it's started
              ensureStarted(existingBundle);
              monitor.done();
              return Status.OK_STATUS;
            }
          }
          monitor.worked(20);

          // Step 2: Check if required dependencies are available
          monitor.subTask("Checking dependencies");
          List<String> missingDependencies = getMissingDependencies();
          monitor.worked(20);

          if (missingDependencies.isEmpty()) {
            // Dependencies available, install or update from embedded JAR
            return installFromEmbeddedJar(existingBundle, monitor);
          } else {
            // Dependencies missing, cache and notify listeners
            cachedMissingDependencies = missingDependencies;
            notifyMissingDependencies(isModernTerminal ? MODERN_TERMINAL_TYPE : TM_TERMINAL_TYPE, missingDependencies);
            return new Status(IStatus.WARNING, bundleContext.getBundle().getSymbolicName(),
                "Terminal dependencies not available. User needs to install them manually.");
          }

        } catch (BundleException e) {
          monitor.done();
          CopilotCore.LOGGER.error("Failed to install terminal bundle", e);
          return new Status(IStatus.ERROR, bundleContext.getBundle().getSymbolicName(),
              "Failed to install terminal bundle", e);
        }
      }
    };
    installationJob.setSystem(true);
    installationJob.schedule();
  }

  /**
   * Install terminal bundle from embedded JAR file (bundled with the plugin).
   *
   * @param existingBundle the existing bundle to update, or null if installing new
   * @param monitor progress monitor
   */
  private IStatus installFromEmbeddedJar(Bundle existingBundle, IProgressMonitor monitor) {
    try {
      monitor.subTask("Locating embedded bundle resource");
      String jarName = isModernTerminal ? MODERN_TERMINAL_BUNDLE_JAR : TM_TERMINAL_BUNDLE_JAR;
      URL bundleUrl = getEmbeddedBundleUrl(jarName);
      if (bundleUrl == null) {
        return new Status(IStatus.ERROR, bundleContext.getBundle().getSymbolicName(),
            "Failed to locate terminal bundle JAR: " + jarName);
      }
      monitor.worked(20);

      monitor.subTask(existingBundle != null ? "Updating bundle" : "Installing bundle");
      return installOrUpdateBundle(bundleUrl, existingBundle, monitor);
    } catch (InterruptedException | BundleException e) {
      CopilotCore.LOGGER.error("Failed to install from embedded JAR", e);
      return new Status(IStatus.ERROR, bundleContext.getBundle().getSymbolicName(),
          "Failed to install from embedded JAR", e);
    }
  }

  /**
   * Get the version of the terminal bundle. All Copilot bundles share the same version.
   *
   * @return the terminal bundle version, or null if not available
   */
  private Version getEmbeddedBundleVersion() {
    String versionStr = PlatformUtils.getBundleVersion();
    if (versionStr == null || "unknown".equals(versionStr)) {
      return null;
    }
    try {
      return Version.parseVersion(versionStr);
    } catch (IllegalArgumentException e) {
      CopilotCore.LOGGER.error("Failed to parse Copilot bundle version: " + versionStr, e);
      return null;
    }
  }

  /**
   * Get URL to the embedded terminal bundle JAR from the UI plugin.
   */
  private URL getEmbeddedBundleUrl(String bundleJarName) {
    try {
      Bundle[] bundles = bundleContext.getBundles();
      for (Bundle bundle : bundles) {
        if ("com.microsoft.copilot.eclipse.ui".equals(bundle.getSymbolicName())) {
          URL bundleUrl = FileLocator.find(bundle, new Path(TERMINAL_BUNDLES_PATH + bundleJarName));
          if (bundleUrl != null) {
            return FileLocator.toFileURL(bundleUrl);
          }
        }
      }
      return null;

    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to locate embedded bundle resource: " + bundleJarName, e);
      return null;
    }
  }

  /**
   * Install or update bundle from embedded JAR URL.
   *
   * @param bundleUrl URL to the bundle JAR
   * @param existingBundle existing bundle to update, or null to install new
   * @param monitor progress monitor
   */
  private IStatus installOrUpdateBundle(URL bundleUrl, Bundle existingBundle, IProgressMonitor monitor)
      throws InterruptedException, BundleException {
    Bundle targetBundle;

    if (existingBundle != null) {
      // Update existing bundle
      try (var in = bundleUrl.openStream()) {
        existingBundle.update(in);
        targetBundle = existingBundle;
      } catch (BundleException | IOException e) {
        CopilotCore.LOGGER.error("Failed to update terminal bundle", e);
        return new Status(IStatus.ERROR, bundleContext.getBundle().getSymbolicName(),
            "Failed to update terminal bundle: " + e.getMessage(), e);
      }
    } else {
      // Install new bundle
      try {
        targetBundle = bundleContext.installBundle(bundleUrl.toString());
      } catch (BundleException e) {
        CopilotCore.LOGGER.error("Failed to install terminal bundle", e);
        return new Status(IStatus.ERROR, bundleContext.getBundle().getSymbolicName(),
            "Failed to install terminal bundle: " + e.getMessage(), e);
      }
    }
    monitor.worked(20);

    // Refresh and start the bundle
    monitor.subTask("Refreshing bundles");
    refreshBundles(Set.of(targetBundle));
    monitor.worked(20);

    monitor.subTask("Starting bundle");
    ensureStarted(targetBundle);
    monitor.worked(20);

    return Status.OK_STATUS;
  }

  /**
   * Get missing dependencies by reading Require-Bundle from the embedded JAR's MANIFEST.MF and checking which ones are
   * not available in the runtime.
   *
   * @return list of missing bundle symbolic names, empty if all dependencies are available
   */
  private List<String> getMissingDependencies() {
    List<String> missing = new ArrayList<>();

    try {
      String jarName = isModernTerminal ? MODERN_TERMINAL_BUNDLE_JAR : TM_TERMINAL_BUNDLE_JAR;
      URL bundleUrl = getEmbeddedBundleUrl(jarName);
      if (bundleUrl == null) {
        return missing;
      }

      try (JarInputStream jarStream = new JarInputStream(bundleUrl.openStream())) {
        Manifest manifest = jarStream.getManifest();
        if (manifest != null) {
          String requireBundle = manifest.getMainAttributes().getValue("Require-Bundle");
          if (requireBundle != null) {
            for (String bundleName : parseRequireBundle(requireBundle)) {
              if (Platform.getBundle(bundleName) == null) {
                missing.add(bundleName);
              }
            }
          }
        }
      }
    } catch (IOException e) {
      CopilotCore.LOGGER.error("Failed to check dependencies from embedded JAR", e);
    }

    return missing;
  }

  /**
   * Parse the Require-Bundle manifest header to extract bundle symbolic names. Uses Eclipse's ManifestElement parser
   * for accurate OSGi header parsing.
   *
   * @param requireBundle the Require-Bundle header value
   * @return array of bundle symbolic names
   */
  private String[] parseRequireBundle(String requireBundle) {
    List<String> dependencies = new ArrayList<>();

    try {
      ManifestElement[] elements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, requireBundle);
      if (elements != null) {
        for (ManifestElement element : elements) {
          String bundleName = element.getValue();
          if (bundleName != null && !bundleName.isEmpty()) {
            dependencies.add(bundleName);
          }
        }
      }
    } catch (BundleException e) {
      CopilotCore.LOGGER.error("Failed to parse Require-Bundle header", e);
    }

    return dependencies.toArray(new String[0]);
  }

  /**
   * Detect if the current Eclipse platform supports modern terminal (Eclipse 4.37+).
   */
  private static boolean detectModernTerminal() {
    Version currentVersion = PlatformUtils.getEclipseVersion();
    if (currentVersion == null) {
      // Default to modern terminal if version check fails
      return true;
    }
    return currentVersion.compareTo(TARGET_PLATFORM_VERSION) >= 0;
  }

  /**
   * Get an installed bundle by symbolic name.
   */
  private Bundle getInstalledBundle(String symbolicName) {
    for (Bundle bundle : bundleContext.getBundles()) {
      if (symbolicName.equals(bundle.getSymbolicName())) {
        return bundle;
      }
    }
    return null;
  }

  /**
   * Refresh bundles to apply wiring changes.
   */
  private void refreshBundles(Set<Bundle> toRefresh) throws InterruptedException {
    if (!toRefresh.isEmpty()) {
      final CountDownLatch latch = new CountDownLatch(1);
      FrameworkWiring frameworkWiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
      frameworkWiring.refreshBundles(toRefresh, new FrameworkListener() {
        @Override
        public void frameworkEvent(FrameworkEvent event) {
          if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
            latch.countDown();
          } else if (event.getType() == FrameworkEvent.ERROR) {
            latch.countDown();
          }
        }
      });
      latch.await(10, TimeUnit.SECONDS);
    }
  }

  /**
   * Ensure a bundle is started.
   */
  private void ensureStarted(Bundle bundle) throws BundleException {
    if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
      bundle.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);
    }
  }
}
