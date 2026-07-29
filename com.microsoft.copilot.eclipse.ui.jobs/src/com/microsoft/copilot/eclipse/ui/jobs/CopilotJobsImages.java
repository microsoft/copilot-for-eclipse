// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.jobs;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Centralized access to all static icons in the Copilot Jobs UI bundle.
 *
 * <p>Bundle images are owned by the plugin's {@link ImageRegistry} and disposed automatically when
 * the plugin stops. Callers must <em>not</em> dispose images returned by {@link #getImage(String)}.
 */
public final class CopilotJobsImages {

  // Path prefixes
  private static final String ICONS_ROOT = "icons/";
  private static final String ICONS_STATUS = ICONS_ROOT + "status/";

  // Icons
  public static final String IMG_REPO = ICONS_ROOT + "repo.png";
  public static final String IMG_INFORMATION = ICONS_ROOT + "information.png";
  public static final String IMG_STATUS_LOADING = ICONS_STATUS + "loading.png";
  public static final String IMG_STATUS_COMPLETE = ICONS_STATUS + "complete.png";

  private CopilotJobsImages() {
    // prevent instantiation
  }

  /**
   * Returns the plugin's image registry.
   */
  static ImageRegistry getImageRegistry() {
    return CopilotJobs.getPlugin().getImageRegistry();
  }

  /**
   * Registers all static icon descriptors. Called once from
   * {@link CopilotJobs#initializeImageRegistry(ImageRegistry)}.
   */
  static void initialize(ImageRegistry registry) {
    register(registry, IMG_REPO);
    register(registry, IMG_INFORMATION);
    register(registry, IMG_STATUS_LOADING);
    register(registry, IMG_STATUS_COMPLETE);
  }

  private static void register(ImageRegistry registry, String path) {
    URL url = CopilotJobsImages.class.getResource("/" + path);
    ImageDescriptor descriptor = url != null
        ? ImageDescriptor.createFromURL(url) : ImageDescriptor.getMissingImageDescriptor();
    registry.put(path, descriptor);
  }

  /**
   * Returns the image for the given key. The returned image is owned by the plugin registry;
   * callers must <em>not</em> dispose it.
   *
   * @param key one of the {@code IMG_*} constants defined in this class
   * @return the registry-owned image for the given key
   */
  public static Image getImage(String key) {
    return getImageRegistry().get(key);
  }

  /**
   * Returns the {@link ImageDescriptor} for the given key. Useful where a descriptor is required,
   * e.g. for {@code Action.setImageDescriptor()}.
   *
   * @param key one of the {@code IMG_*} constants defined in this class
   * @return the image descriptor for the given key
   */
  public static ImageDescriptor getImageDescriptor(String key) {
    return getImageRegistry().getDescriptor(key);
  }

  /**
   * Convenience access to Eclipse's workbench shared images.
   *
   * @param imageId a constant from {@link ISharedImages}, e.g. {@link ISharedImages#IMG_OBJS_WARN_TSK}
   * @return the shared workbench image for the given id
   */
  public static Image getSharedImage(String imageId) {
    return PlatformUI.getWorkbench().getSharedImages().getImage(imageId);
  }

}
