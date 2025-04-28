package com.microsoft.copilot.eclipse.ui.chat;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.URLHyperlink;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.mylyn.internal.wikitext.ui.viewer.AnnotationHyperlinkDetector;

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
      }

      super.open();
    }
  }
}
