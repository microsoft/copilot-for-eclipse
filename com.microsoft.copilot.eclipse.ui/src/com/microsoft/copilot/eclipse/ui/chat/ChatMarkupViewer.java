package com.microsoft.copilot.eclipse.ui.chat;

import java.io.StringWriter;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.MultipleHyperlinkPresenter;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.ui.viewer.MarkupViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

class ChatMarkupViewer extends MarkupViewer {

  public ChatMarkupViewer(Composite parent, int styles) {
    super(parent, null, styles);
    this.setMarkupLanguage(new MarkdownLanguage());
    this.setDisplayImages(false);

    IHyperlinkDetector[] hyperlinkDetectors = {
        new FileAnnotationHyperlinkDetector()
    };
    this.setHyperlinkDetectors(hyperlinkDetectors, SWT.NONE);

    // TODO: set hyperlink color based on theme
    MultipleHyperlinkPresenter hyperlinkPresenter = new MultipleHyperlinkPresenter((RGB) null);
    this.setHyperlinkPresenter(hyperlinkPresenter);
  }

  // MarkupViewer will write errors when failed to parse the markup, which will send the error to the Copilot.
  // so overwrite the setMarkup method to avoid sending the error.
  @Override
  public void setMarkup(String source) {
    try {
      String htmlText = this.computeHtml(source);
      setHtml(htmlText);
      // reset text presentation to update the style, otherwise the style won't be updated
      this.setTextPresentation(getTextPresentation());
    } catch (Throwable t) {
      if (getTextPresentation() != null) {
        getTextPresentation().clear();
      }
      setDocumentNoMarkup(new Document(source), new AnnotationModel());
      // TODO: Whether we should track the parse exception?
    }
  }

  // computeHtml(String) is a private method in MarkupViewer, so copy it here.
  private String computeHtml(String markupContent) {
    StringWriter out = new StringWriter();
    HtmlDocumentBuilder builder = new HtmlDocumentBuilder(out);
    builder.setFilterEntityReferences(true);

    getParser().setBuilder(builder);
    getParser().parse(markupContent);
    getParser().setBuilder(null);

    String htmlText = out.toString();
    return htmlText;
  }
}
