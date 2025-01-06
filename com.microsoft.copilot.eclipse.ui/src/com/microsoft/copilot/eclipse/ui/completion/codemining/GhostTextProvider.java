package com.microsoft.copilot.eclipse.ui.completion.codemining;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;

/**
 * A provider for ghost text.
 */
public class GhostTextProvider extends AbstractCodeMiningProvider {

  @Override
  public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
      IProgressMonitor monitor) {
    return CompletableFuture.completedFuture(getCodeMinings());
  }

  @Nullable
  private List<ICodeMining> getCodeMinings() {
    ITextEditor belongingEditor = this.getAdapter(ITextEditor.class);
    if (belongingEditor == null) {
      return Collections.emptyList();
    }
    CopilotUi copilotUi = CopilotUi.getPlugin();
    if (copilotUi == null) {
      return Collections.emptyList();
    }
    EditorsManager editorsManager = copilotUi.getEditorsManager();
    if (editorsManager == null) {
      return Collections.emptyList();
    }
    CompletionManager manager = editorsManager.getCompletionManagerFor(belongingEditor);

    if (manager == null) {
      return Collections.emptyList();
    }

    return manager.getCodeMinings();
  }

}
