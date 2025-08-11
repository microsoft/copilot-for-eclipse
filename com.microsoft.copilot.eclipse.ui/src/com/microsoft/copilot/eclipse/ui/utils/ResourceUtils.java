package com.microsoft.copilot.eclipse.ui.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.copilot.eclipse.core.utils.FileUtils;

/**
 * Utilities for handling Copilot reference resources in Eclipse UI.
 * Supports selection validation, resource adaptation, and statistics collection.
 */
public final class ResourceUtils {

  private ResourceUtils() {
    // prevent instantiation
  }

  /**
   * Collect valid resources from the selection.
   */
  public static List<IResource> collectValidResources(IStructuredSelection selection) {
    List<IResource> validResources = new ArrayList<>();

    if (selection == null || selection.isEmpty()) {
      return validResources;
    }

    for (Object obj : selection.toList()) {
      IResource resource = adaptToResource(obj);
      if (isValidResource(resource)) {
        validResources.add(resource);
      }
    }

    return validResources;
  }

  /**
   * Analyze the selection and return statistics about files, folders, and invalid resources.
   */
  public static SelectionStats analyzeSelection(IStructuredSelection selection) {
    int fileCount = 0;
    int folderCount = 0;
    int invalidCount = 0;

    if (selection != null && !selection.isEmpty()) {
      for (Object obj : selection.toList()) {
        IResource resource = adaptToResource(obj);
        if (resource instanceof IFile file) {
          if (isValidFile(file)) {
            fileCount++;
          } else {
            invalidCount++;
          }
        } else if (resource instanceof IFolder) {
          folderCount++;
        } else {
          invalidCount++;
        }
      }
    }

    return new SelectionStats(fileCount, folderCount, invalidCount);
  }

  /**
   * Adapt an object to IResource.
   */
  private static IResource adaptToResource(Object obj) {
    if (obj instanceof IResource r) {
      return r;
    }
    if (obj instanceof IAdaptable a) {
      IResource r = a.getAdapter(IResource.class);
      if (r != null) {
        return r;
      }
    }
    return (IResource) Platform.getAdapterManager().getAdapter(obj, IResource.class);
  }

  /**
   * Check if the file is valid (not excluded from referenced files).
   */
  private static boolean isValidFile(IFile file) {
    return file != null && !FileUtils.isExcludedFromReferencedFiles(file);
  }

  /**
   * Check if the resource is a valid folder or file.
   */
  private static boolean isValidResource(IResource resource) {
    if (resource instanceof IFolder) {
      return true;
    }
    if (resource instanceof IFile file) {
      return isValidFile(file);
    }
    return false;
  }
  
  /**
   * Statistics about the selection of resources.
   */
  public static final class SelectionStats {
    public final int fileCount;
    public final int folderCount;
    public final int invalidCount;

    /**
     * Constructor for SelectionStats.
     *
     * @param fileCount    the count of valid files
     * @param folderCount  the count of folders
     * @param invalidCount the count of invalid resources
     */
    public SelectionStats(int fileCount, int folderCount, int invalidCount) {
      this.fileCount = fileCount;
      this.folderCount = folderCount;
      this.invalidCount = invalidCount;
    }

    /**
     * Check if the selection has only files.
     *
     * @return true if there are only files, false otherwise
     */
    public boolean hasOnlyFiles() {
      return fileCount > 0 && folderCount == 0 && invalidCount == 0;
    }

    /**
     * Check if the selection has only folders.
     *
     * @return true if there are only folders, false otherwise
     */
    public boolean hasOnlyFolders() {
      return folderCount > 0 && fileCount == 0 && invalidCount == 0;
    }

    /**
     * Check if the selection has only valid resources (files or folders).
     *
     * @return true if there are valid resources, false if all are invalid
     */
    public boolean hasOnlyValidResources() {
      return invalidCount == 0 && (fileCount > 0 || folderCount > 0);
    }
  }
}
