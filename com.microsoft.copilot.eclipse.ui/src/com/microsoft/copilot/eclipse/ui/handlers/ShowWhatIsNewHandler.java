package com.microsoft.copilot.eclipse.ui.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handler for opening the change log.
 */
public class ShowWhatIsNewHandler extends AbstractHandler {

  private static final String WHATISNEW_RESOURCES_PATH = "intro/whatsnew";
  private static final String WHATISNEW_FILE_NAME = "WHATISNEW.md";
  private static final String HTML_EDITOR_ID = "org.eclipse.ui.browser.editor";
  private static final String FALLBACK_EDITOR_ID = "org.eclipse.ui.DefaultTextEditor";

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    try {
      File changelogResources = locateChangelogResources();
      if (changelogResources == null) {
        return null;
      }

      File markdownFile = new File(changelogResources, WHATISNEW_FILE_NAME);

      String markdownContent = readMarkdownFile(markdownFile);
      String htmlContent = convertMarkdownToHtml(markdownContent);
      File htmlFile = createHtmlFile(markdownFile, htmlContent);

      openHtmlFileInEditor(htmlFile, markdownFile);
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Failed to process changelog", e);
    }

    return null;
  }

  /**
   * Locates the changelog file in the bundle.
   *
   * @return The changelog file or null if not found.
   * @throws IOException if there's an error locating the file
   * @throws URISyntaxException if there's an error converting URLs
   */
  private File locateChangelogResources() throws IOException, URISyntaxException {
    Bundle bundle = FrameworkUtil.getBundle(this.getClass());
    if (bundle == null) {
      CopilotCore.LOGGER.error(new IllegalStateException("Bundle not found"));
      return null;
    }

    URL fileUrl = FileLocator.find(bundle, new Path(WHATISNEW_RESOURCES_PATH));
    if (fileUrl == null) {
      CopilotCore.LOGGER.error(new IllegalStateException(WHATISNEW_RESOURCES_PATH + " not found"));
      return null;
    }

    return URIUtil.toFile(URIUtil.toURI(FileLocator.toFileURL(fileUrl)));
  }

  /**
   * Reads the content of the markdown file.
   *
   * @param file The markdown file to read.
   * @return The content of the file as a string
   * @throws IOException if there's an error reading the file
   */
  private String readMarkdownFile(File file) throws IOException {
    StringBuilder content = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        content.append(line).append("\n");
      }
    }
    return content.toString();
  }

  /**
   * Converts markdown content to HTML.
   *
   * @param markdown The markdown content to convert.
   * @return The converted HTML content
   */
  private String convertMarkdownToHtml(String markdown) {
    StringWriter writer = new StringWriter();
    HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
    builder.setEmitAsDocument(true);

    MarkupParser markupParser = new MarkupParser(new MarkdownLanguage());
    markupParser.setBuilder(builder);
    markupParser.parse(markdown);

    return writer.toString();
  }

  /**
   * Creates an HTML file based on the markdown file path.
   *
   * @param markdownFile The original markdown file.
   * @param htmlContent The HTML content to write
   * @return The created HTML file
   * @throws IOException if there's an error writing the file
   */
  private File createHtmlFile(File markdownFile, String htmlContent) throws IOException {
    String htmlFileName = markdownFile.getAbsolutePath().replace(".md", ".html");
    File htmlFile = new File(htmlFileName);

    try (BufferedWriter htmlWriter = new BufferedWriter(new FileWriter(htmlFile))) {
      htmlWriter.write(htmlContent);
    }

    return htmlFile;
  }

  /**
   * Opens the HTML file in the Eclipse editor.
   *
   * @param htmlFile The HTML file to open.
   * @param fallbackFile The fallback markdown file to open if the HTML file fails.
   */
  private void openHtmlFileInEditor(File htmlFile, File fallbackFile) {
    SwtUtils.invokeOnDisplayThread(() -> {
      IWorkbenchPage page = UiUtils.getActivePage();
      if (page == null) {
        CopilotCore.LOGGER.error(new IllegalStateException("Workbench page not found"));
        return;
      }
      try {
        IDE.openEditor(page, htmlFile.toURI(), HTML_EDITOR_ID, true);
      } catch (PartInitException e) {
        // If opening the HTML file fails, fall back to opening the markdown file
        try {
          IDE.openEditor(page, fallbackFile.toURI(), FALLBACK_EDITOR_ID, true);
        } catch (PartInitException e1) {
          CopilotCore.LOGGER.error(e1);
        }
      }
    });
  }
}
