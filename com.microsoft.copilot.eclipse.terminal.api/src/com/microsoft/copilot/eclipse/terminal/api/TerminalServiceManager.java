// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.terminal.api;

import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Central manager for terminal services. Tracks the {@link IRunInTerminalTool} OSGi service provided by the terminal
 * implementation bundle and notifies listeners when it becomes available.
 */
public class TerminalServiceManager {

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
      listener.onServiceAvailable(service);
    }
  }
}
