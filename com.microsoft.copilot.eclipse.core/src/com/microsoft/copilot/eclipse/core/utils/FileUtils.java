package com.microsoft.copilot.eclipse.core.utils;

import java.net.URI;

import org.eclipse.core.resources.IFile;
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
   * @param file The file resource
   * @return A URI string or null if no URI could be determined
   */
  public static String getFileUri(IFile file) {
    // Try standard file location first
    if (file.getLocation() != null) {
      URI url = LSPEclipseUtils.toUri((IResource) file);
      if (url != null) {
        return url.toASCIIString();
      }
    }

    // Try getting direct URI (works for remote resources)
    if (file.getLocationURI() != null) {
      URI url = file.getLocationURI();
      if (url != null) {
        return url.toASCIIString();
      }
    }

    // Fall back to platform resource URI
    // See:
    // https://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/org/eclipse/e4/ui/css/swt/helpers/URI.html#createPlatformResourceURI(java.lang.String,boolean)
    return "platform:/resource" + file.getFullPath().toString();
  }
}
