package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.URLHyperlink;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.mylyn.internal.wikitext.ui.viewer.AnnotationHyperlinkDetector;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A hyperlink detector that detects file links in markdown format. It opens the file in the editor when the link is
 * clicked.
 */
public class FileAnnotationHyperlinkDetector extends AnnotationHyperlinkDetector {

  @Override
  protected IHyperlink createUrlHyperlink(IRegion region, String href) {
    return new FileHyperlink(region, href);
  }

  private class FileHyperlink extends URLHyperlink {

    public FileHyperlink(IRegion region, String urlString) {
      super(region, LSPEclipseUtils.toUri(urlString).toString());
    }

    @Override
    public void open() {
      String urlString = getURLString();
      if (urlString.startsWith(LSPEclipseUtils.FILE_URI)) {
        IResource targetResource = LSPEclipseUtils.findResourceFor(urlString);
        if (targetResource != null && targetResource.getType() == IResource.FILE) {
          Location location = new Location();
          location.setUri(urlString);
          LSPEclipseUtils.openInEditor(location);
          return;
        }
      } else {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(urlString));

        if (file.exists()) {
          var workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
          if (workbenchWindow == null) {
            return;
          }

          IWorkbenchPage page = UiUtils.getActivePage();
          if (page == null) {
            return;
          }

          try {
            IDE.openEditor(page, file);
          } catch (PartInitException e) {
            CopilotCore.LOGGER.error("Failed to open file: " + urlString, e);
          }
          return;
        }
      }

      super.open();
    }
  }
}
