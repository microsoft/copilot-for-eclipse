package com.microsoft.copilot.eclipse.ui.jobs.utils;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * Utilities for Eclipse UI in the jobs package.
 */
public class UiUtils {

  private UiUtils() {
    // prevent instantiation
  }

  /**
   * Builds an image descriptor from a PNG file at the given path.
   *
   * @param path the path to the PNG file
   * @return the image descriptor, or null if the resource is not found
   */
  public static ImageDescriptor buildImageDescriptorFromPngPath(String path) {
    return ImageDescriptor.createFromURL(UiUtils.class.getResource(path));
  }

  /**
   * Builds an image from a PNG file at the given path.
   *
   * @param path the path to the PNG file
   * @return the image, or null if the resource is not found
   */
  public static Image buildImageFromPngPath(String path) {
    ImageDescriptor descriptor = buildImageDescriptorFromPngPath(path);
    return descriptor != null ? descriptor.createImage() : null;
  }
}
