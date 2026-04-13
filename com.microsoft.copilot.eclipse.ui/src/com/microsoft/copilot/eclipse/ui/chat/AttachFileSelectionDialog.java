// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

import com.microsoft.copilot.eclipse.core.Constants;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

class AttachFileSelectionDialog extends FilteredResourcesSelectionDialog {
  private static final String OPENED_FILES = "${OPENED_FILES}";
  private List<IFile> openedFiles;

  public AttachFileSelectionDialog(Shell shell, boolean multi, IContainer container) {
    super(shell, multi, container, IResource.FILE | IResource.FOLDER);
    this.openedFiles = UiUtils.getOpenedFiles();

    this.setInitialPattern(OPENED_FILES, NONE);
    this.refresh();
  }

  @Override
  protected ItemsFilter createFilter() {
    return new AttachFileItemsFilter();
  }

  class AttachFileItemsFilter extends ResourceFilter {

    private Set<String> uris;

    public AttachFileItemsFilter() {
      super();
      this.uris = new HashSet<>();
    }

    @Override
    public boolean matchItem(Object item) {
      if (!(item instanceof IResource)) {
        return false;
      }
      String uri = FileUtils.getResourceUri((IResource) item);
      if (uri == null || this.uris.contains(uri)) {
        return false;
      }
      this.uris.add(uri);

      final String pattern = this.patternMatcher.getPattern();
      if (item instanceof IFile file) {
        final String extension = file.getFileExtension();
        boolean isExcludedType = extension != null && Constants.EXCLUDED_REFERENCE_FILE_TYPE.contains(extension);
        if (isExcludedType) {
          return false;
        }

        if (StringUtils.equals(pattern, OPENED_FILES)) {
          return AttachFileSelectionDialog.this.openedFiles.contains(item);
        } else {
          return super.matchItem(item);
        }
      } else if (item instanceof IFolder) {
        return super.matchItem(item);
      }
      return false;
    }
  }
}
