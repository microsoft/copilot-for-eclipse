package com.microsoft.copilot.eclipse.ui.completion;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionDocument;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Handle completion for an ITextEditor.
 */
public class CompletionHandler implements ITextListener, CaretListener, IJobChangeListener {

  private CopilotLanguageServerConnection lsConnection;
  private ITextEditor editor;
  private ITextViewer textViewer;
  private IDocument document;
  private URI documentUri;
  private volatile int documentVersion;

  private CompletionJob completionJob;
  private CompletionData completionData;
  private CompletionRendering completionRendering;

  /**
   * Creates a new completion handler.
   */
  public CompletionHandler(CopilotLanguageServerConnection lsConnection, ITextEditor editor) {
    this.lsConnection = lsConnection;
    this.completionJob = new CompletionJob(lsConnection);
    this.completionData = new CompletionData();
    this.completionJob.addJobChangeListener(this);
    this.editor = editor;
    this.textViewer = (ITextViewer) this.editor.getAdapter(ITextOperationTarget.class);
    if (textViewer == null) {
      // TODO: log & send telemetry
      return;
    }
    this.completionRendering = new CompletionRendering(this.textViewer, this.completionData);
    this.document = LSPEclipseUtils.getDocument(editor);
    this.documentUri = UiUtils.getUriFromTextEditor(editor);
    try {
      lsConnection.connectDocument(this.document);
    } catch (IOException e) {
      // TODO: log & send telemetry
      return;
    }
    this.documentVersion = -1;
    registerListeners();
  }

  void registerListeners() {
    // if the text viewer is null, we will not register listeners.
    // the side effect is that the completion will not be triggered for this editor.
    if (this.textViewer == null) {
      // TODO: log & send telemetry
      return;
    }

    SwtUtils.invokeOnDisplayThread(() -> {
      this.textViewer.getTextWidget().addCaretListener(this);
      this.textViewer.addTextListener(this);
    });
  }

  void triggerCompletion() {
    int caretOffset = UiUtils.getCaretOffset(this.editor);
    this.completionData.setTriggerOffset(caretOffset);
    CompletionParams completionParam = null;
    try {
      completionParam = this.createCompletionParams(caretOffset);
    } catch (BadLocationException e) {
      // TODO: log & send telemetry
      return;
    }
    this.completionJob.cancel();
    this.completionJob.setCompletionParams(completionParam);
    this.completionJob.schedule();
  }

  CompletionParams createCompletionParams(int offset) throws BadLocationException {
    String uriString = this.documentUri.toASCIIString();
    Position position = LSPEclipseUtils.toPosition(offset, this.document);
    CompletionDocument completionDoc = new CompletionDocument(uriString, position);
    completionDoc.setVersion(this.documentVersion);
    // following format options are hard-coded, because eclipse support applying the format options
    // automatically when drawing text into the editor, so don't need to set the actual values here.
    completionDoc.setInsertSpaces(true);
    completionDoc.setTabSize(4);
    return new CompletionParams(completionDoc);
  }

  void clearCompletion() {
    if (this.completionData.getSize() > 0) {
      // if the completion data is not empty, clear it and trigger a redraw.
      this.completionData.setItems(List.of(CompletionData.EMPTY_ITEM));
      this.completionRendering.redraw();
    }
  }

  @Override
  public void caretMoved(CaretEvent event) {
    // it's guaranteed that the document change event comes earlier than caret
    // change event. See org.eclipse.swt.custom.StyledText#modifyContent()
    int currentVersion = this.lsConnection.getDocumentVersion(this.documentUri);

    // initialize the document version and return. This avoids the ghost text
    // being rendered when user opens the editor and just clicks in it.
    if (this.documentVersion < 0) {
      this.documentVersion = currentVersion;
      return;
    }
    if (currentVersion == this.documentVersion) {
      // if the caret position is changed without document version change, we should remove the ghost text.
      clearCompletion();
    } else {
      this.documentVersion = currentVersion;
      triggerCompletion();
    }

  }

  @Override
  public void textChanged(TextEvent event) {
    // this event comes earlier than caret change event. So we should check if the typed characters
    // are the same as the ghost. Then determine whether a re-redering or a new completion
    // request is needed.
    // TODO: check changed text
  }

  /**
   * Disposes the resources of this completion handler.
   */
  public void dispose() {
    this.completionJob.cancel();
    this.completionJob.removeJobChangeListener(this);
    lsConnection.disconnectDocument(this.documentUri);
    if (this.textViewer != null) {
      SwtUtils.invokeOnDisplayThread(() -> {
        this.textViewer.getTextWidget().removeCaretListener(this);
        this.textViewer.removeTextListener(this);
        this.completionRendering.dispose();
      });
    }

  }

  @Override
  public void aboutToRun(IJobChangeEvent event) {
    // do nothing
  }

  @Override
  public void awake(IJobChangeEvent event) {
    // do nothing
  }

  @Override
  public void done(IJobChangeEvent event) {
    IStatus jobStatus = this.completionJob.getResult();
    if (jobStatus != null && !jobStatus.isOK()) {
      return;
      // TODO: log & send telemetry
    }
    CompletionResult result = this.completionJob.getCompletionResult();
    if (result == null || result.getCompletions() == null || result.getCompletions().isEmpty()) {
      return;
    }
    // ignore the result if the document version is out-dated.
    if (this.documentVersion != this.completionData.getDocumentVersion()) {
      return;
    }

    this.completionData.setItems(result.getCompletions());
    this.completionRendering.redraw();
  }

  @Override
  public void running(IJobChangeEvent event) {
    // do nothing
  }

  @Override
  public void scheduled(IJobChangeEvent event) {
    // do nothing
  }

  @Override
  public void sleeping(IJobChangeEvent event) {
    // do nothing
  }

}
