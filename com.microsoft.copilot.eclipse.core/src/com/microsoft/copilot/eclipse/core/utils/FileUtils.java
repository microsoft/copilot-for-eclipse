package com.microsoft.copilot.eclipse.core.utils;

import java.net.URI;

import org.eclipse.core.resources.IResource;
import org.eclipse.lsp4e.LSPEclipseUtils;

/**
 * Utilities for the core module.
 */
public class FileUtils {
  private FileUtils() {
  }

  /**
   * Gets a URI string for a resource using multiple approaches.
   *
   * @param resource The resource
   * @return A URI string or null if no URI could be determined
   */
  public static String getResourceUri(IResource resource) {
    if (resource == null) {
      return null;
    }

    // Try standard file location first
    if (resource.getLocation() != null) {
      URI url = LSPEclipseUtils.toUri(resource);
      if (url != null) {
        return url.toASCIIString();
      }
    }

    // Try getting direct URI (works for remote resources)
    if (resource.getLocationURI() != null) {
      URI url = resource.getLocationURI();
      if (url != null) {
        return url.toASCIIString();
      }
    }

    // Fall back to platform resource URI
    // See:
    // https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/e4/ui/css/swt/helpers/URI.html#createPlatformResourceURI(java.lang.String,boolean)
    return "platform:/resource" + resource.getFullPath().toString();
  }
}
