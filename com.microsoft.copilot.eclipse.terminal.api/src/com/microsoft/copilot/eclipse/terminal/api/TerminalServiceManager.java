package com.microsoft.copilot.eclipse.terminal.api;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.microsoft.copilot.eclipse.core.CopilotCore;

/**
 * Central manager for terminal services. Handles service discovery, bundle installation, and notification to interested
 * parties.
 */
public class TerminalServiceManager {

  private static final String TERMINAL_BUNDLES_PATH = "terminal-bundles/";
  private static final String MODERN_TERMINAL_BUNDLE_JAR = 
      "com.microsoft.copilot.eclipse.ui.terminal.jar";
  private static final String TM_TERMINAL_BUNDLE_JAR = 
      "com.microsoft.copilot.eclipse.ui.terminal.tm.jar";
  private static final String MODERN_TERMINAL_BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.terminal";
  private static final String TM_TERMINAL_BUNDLE_NAME = "com.microsoft.copilot.eclipse.ui.terminal.tm";
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
  }

  private TerminalServiceManager(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
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
    } else {
      // No service available, try to install terminal bundle
      attemptTerminalBundleInstallation();
    }
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
      try {
        listener.onServiceAvailable(service);
      } catch (Exception e) {
        CopilotCore.LOGGER.error("Error notifying terminal service listener", e);
      }
    }
  }

  /**
   * Attempt to install terminal bundle from embedded JAR.
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
          monitor.subTask("Determining terminal bundle version");
          String bundleJarName = determineTerminalBundleJar();
          if (bundleJarName == null) {
            monitor.done();
            return Status.CANCEL_STATUS;
          }
          monitor.worked(20);

          monitor.subTask("Locating bundle resource");
          URL bundleUrl = getBundleResourceUrl(bundleJarName);
          if (bundleUrl == null) {
            monitor.done();
            return Status.CANCEL_STATUS;
          }
          monitor.worked(40);

          monitor.subTask("Installing and starting bundle");
          IStatus installStatus = installAndRefreshBundle(bundleUrl.toString(), monitor);
          monitor.worked(40);
          monitor.done();
          if (!installStatus.isOK()) {
            return installStatus;
          }
          return Status.OK_STATUS;

        } catch (Exception e) {
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
   * Install and refresh bundle following JDT pattern.
   */
  private IStatus installAndRefreshBundle(String bundleLocation, IProgressMonitor monitor) throws Exception {
    monitor.beginTask("Installing and refreshing bundle", 100);
    monitor.subTask("Checking existing installation");
    String symbolicName = getBundleSymbolicName();
    Bundle existingBundle = getInstalledBundle(symbolicName);

    if (existingBundle != null) {
      ensureStarted(existingBundle);
      monitor.done();
      return Status.OK_STATUS;
    }
    monitor.worked(20);
    monitor.subTask("Installing bundle");
    Bundle installedBundle = bundleContext.installBundle(bundleLocation);
    Set<Bundle> bundlesToStart = new HashSet<>();
    Set<Bundle> toRefresh = new HashSet<>();
    bundlesToStart.add(installedBundle);
    toRefresh.add(installedBundle);
    monitor.worked(30);
    monitor.subTask("Refreshing bundles");
    refreshBundles(toRefresh);
    monitor.worked(30);
    monitor.subTask("Starting bundles");
    ensureStarted(installedBundle);
    monitor.worked(20);
    monitor.done();
    return Status.OK_STATUS;
  }

  private String getBundleSymbolicName() {
    String bundleJarName = determineTerminalBundleJar();
    if (MODERN_TERMINAL_BUNDLE_JAR.equals(bundleJarName)) {
      return MODERN_TERMINAL_BUNDLE_NAME;
    } else {
      return TM_TERMINAL_BUNDLE_NAME;
    }
  }

  private Bundle getInstalledBundle(String symbolicName) {
    for (Bundle bundle : bundleContext.getBundles()) {
      if (symbolicName.equals(bundle.getSymbolicName())) {
        return bundle;
      }
    }
    return null;
  }

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

  private void ensureStarted(Bundle bundle) throws BundleException {
    if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
      bundle.start(Bundle.START_TRANSIENT | Bundle.START_ACTIVATION_POLICY);
    }
  }

  private String determineTerminalBundleJar() {
    try {
      Version currentVersion = Platform.getBundle("org.eclipse.platform").getVersion();
      if (currentVersion.compareTo(TARGET_PLATFORM_VERSION) >= 0) {
        return MODERN_TERMINAL_BUNDLE_JAR;
      } else {
        return TM_TERMINAL_BUNDLE_JAR;
      }
    } catch (Exception e) {
      return MODERN_TERMINAL_BUNDLE_JAR;
    }
  }

  private URL getBundleResourceUrl(String bundleJarName) {
    try {
      Bundle[] bundles = bundleContext.getBundles();
      for (Bundle bundle : bundles) {
        if ("com.microsoft.copilot.eclipse.ui".equals(bundle.getSymbolicName())) {
          URL bundleUrl = FileLocator.find(bundle, new Path(TERMINAL_BUNDLES_PATH + bundleJarName));
          if (bundleUrl != null) {
            return FileLocator.resolve(bundleUrl);
          }
        }
      }
      return null;

    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to locate bundle resource: " + bundleJarName, e);
      return null;
    }
  }
}
