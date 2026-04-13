package com.microsoft.copilot.eclipse.ui.nes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.lsp4j.Range;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.swt.CssConstants;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Manages inline highlighting and annotations for Next Edit Suggestions.
 */
public class InlineHighlighter {

  private static final String SUGGESTION_CHANGE = "com.microsoft.copilot.eclipse.ui.nes.change";
  private static final String SUGGESTION_DELETE = "com.microsoft.copilot.eclipse.ui.nes.delete";

  private final ITextViewer viewer;
  private final StyledText text;

  private final List<Annotation> annotations = new ArrayList<>();
  private final List<Position> positions = new ArrayList<>();
  private final Set<Integer> highlightedModelLines = new HashSet<>();
  private Color lineBgColor;
  private LineBackgroundListener lineBackgroundListener;

  /** Constructor. Does not register listeners - call registerListeners() from UI thread. */
  public InlineHighlighter(ITextViewer viewer, StyledText text) {
    this.viewer = viewer;
    this.text = text;
    registerListeners();
  }

  /**
   * Register all SWT listeners.
   */
  public void registerListeners() {
    if (text == null || text.isDisposed()) {
      return;
    }
    SwtUtils.invokeOnDisplayThread(() -> {
      lineBackgroundListener = event -> {
        if (highlightedModelLines.isEmpty()) {
          return;
        }
        if (lineBgColor == null || lineBgColor.isDisposed()) {
          return;
        }
        if (viewer == null) {
          return;
        }
        int widgetOffset = event.lineOffset;
        int modelOffset = UiUtils.widgetOffset2ModelOffset(viewer, widgetOffset);
        if (modelOffset < 0) {
          return;
        }
        IDocument doc = viewer.getDocument();
        if (doc == null) {
          return;
        }
        try {
          int modelLine = doc.getLineOfOffset(modelOffset);
          if (highlightedModelLines.contains(modelLine)) {
            event.lineBackground = lineBgColor;
          }
        } catch (BadLocationException ex) {
          CopilotCore.LOGGER.error(ex);
        }
      };
      text.addLineBackgroundListener(lineBackgroundListener);

      // Add dispose listener to clean up resources
      text.addDisposeListener(e -> {
        if (lineBgColor != null && !lineBgColor.isDisposed()) {
          lineBgColor.dispose();
          lineBgColor = null;
        }
        if (lineBackgroundListener != null) {
          text.removeLineBackgroundListener(lineBackgroundListener);
          lineBackgroundListener = null;
        }
      });
    }, this.text);

  }

  /** Apply annotation + line highlight based on diff spans. */
  public void apply(RenderManager.DiffModel diffModel, int startOffset, int endOffset, Range lspRange) {
    clear();
    if (diffModel == null || viewer == null) {
      return;
    }
    if (!(viewer instanceof ISourceViewer sv)) {
      return;
    }
    IDocument doc = sv.getDocument();
    if (doc == null) {
      return;
    }

    // Determine diff type using DiffModel methods
    boolean pureDelete = diffModel.isPureDelete();
    boolean pureInsert = diffModel.isPureInsert();

    // For pure insert, startOffset == endOffset is valid (insertion point)
    // For delete/replace, require endOffset > startOffset
    if (startOffset < 0) {
      return;
    }
    if (!pureInsert && endOffset <= startOffset) {
      return;
    }

    IAnnotationModel model = sv.getAnnotationModel();
    if (model != null && !pureInsert) {
      // Pure insert: no annotation
      // Pure delete: show change and delete annotations for deleted text
      // Mixed: show change annotations for replaced text
      TextDiffCalculator.DualDiffResult res = diffModel.diffResult;
      if (res == null) {
        return; // No diff result available
      }

      // Batch add annotations to avoid multiple redraws
      for (TextDiffCalculator.DualDiffSpan span : res.spans) {
        if (span.origLength > 0) {
          // span.origStart is absolute position in diffModel.original (0-based)
          // Need to map to document position by adding startOffset
          int start = startOffset + span.origStart;
          int len = span.origLength;

          // Bounds check
          if (start < 0 || len <= 0) {
            continue;
          }
          if (start + len > doc.getLength()) {
            // Clamp length to document bounds
            len = Math.max(0, doc.getLength() - start);
            if (len == 0) {
              continue;
            }
          }

          // Add CHANGE annotation (background highlight)
          Annotation changeAnn = new Annotation(SUGGESTION_CHANGE, false,
              "Next Edit Suggestion: " + (pureDelete ? "deletion" : span.type));
          Position changePos = new Position(start, len);
          annotations.add(changeAnn);
          positions.add(changePos);

          // For pure delete, also add delete annotation
          if (pureDelete) {
            Annotation deleteAnn = new Annotation(SUGGESTION_DELETE, false,
                "Next Edit Suggestion: deletion");
            Position deletePos = new Position(start, len);
            annotations.add(deleteAnn);
            positions.add(deletePos);
          }
        }
      }

      // Add all annotations at once to minimize redraws
      if (model instanceof AnnotationModel annotationModel) {
        annotationModel.replaceAnnotations(new Annotation[0],
            annotations.stream().collect(Collectors.toMap(ann -> ann, ann -> positions.get(annotations.indexOf(ann)))));
      }
    }

    // Apply line background:
    // - Pure insert: show insert background
    // - Pure delete: no line background
    // - Mixed: show replace background
    if (pureInsert) {
      applyLineBackground(lspRange, doc, startOffset, endOffset, true);
    } else if (!pureDelete) {
      applyLineBackground(lspRange, doc, startOffset, endOffset, false);
    }
  }

  private void applyLineBackground(Range range, IDocument doc, int startOffset, int endOffset, boolean isInsert) {
    if (text == null || text.isDisposed()) {
      return;
    }
    if (range == null) {
      return;
    }
    if (lineBgColor != null && !lineBgColor.isDisposed()) {
      lineBgColor.dispose();
      lineBgColor = null;
    }
    if (isInsert) {
      lineBgColor = CssConstants.getNesInsertBackground(text.getDisplay());
    } else {
      lineBgColor = CssConstants.getNesReplaceBackground(text.getDisplay());
    }
    highlightedModelLines.clear();
    int startModelLine = range.getStart() != null ? range.getStart().getLine() : -1;
    int endModelLine = range.getEnd() != null ? range.getEnd().getLine() : startModelLine;

    if (startModelLine < 0) {
      try {
        startModelLine = doc.getLineOfOffset(startOffset);
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error(e);
        startModelLine = -1;
      }
    }
    if (endModelLine < 0 && startModelLine >= 0) {
      try {
        endModelLine = doc.getLineOfOffset(endOffset);
      } catch (BadLocationException e) {
        CopilotCore.LOGGER.error(e);
        endModelLine = startModelLine;
      }
    }
    if (startModelLine < 0) {
      return;
    }
    if (endModelLine < startModelLine) {
      endModelLine = startModelLine;
    }
    int endChar = range.getEnd() != null ? range.getEnd().getCharacter() : 0;
    if (endModelLine > startModelLine && endChar == 0) {
      endModelLine -= 1;
    }
    for (int ml = startModelLine; ml <= endModelLine; ml++) {
      highlightedModelLines.add(ml);
    }
    text.redraw();
  }

  /** Clear all annotations and highlights. */
  public void clear() {
    if (!annotations.isEmpty() && viewer instanceof ISourceViewer sv) {
      IAnnotationModel model = sv.getAnnotationModel();
      if (model != null) {
        // Batch remove annotations to avoid multiple redraws
        if (model instanceof AnnotationModel annotationModel) {
          annotationModel.replaceAnnotations(annotations.toArray(new Annotation[0]), Collections.emptyMap());
        }
      }
    }
    annotations.clear();
    positions.clear();
    highlightedModelLines.clear();
    if (text != null && !text.isDisposed()) {
      text.redraw();
    }
  }
}
