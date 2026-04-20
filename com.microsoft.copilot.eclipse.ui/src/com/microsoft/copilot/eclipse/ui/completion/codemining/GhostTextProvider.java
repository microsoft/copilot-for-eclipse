// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion.codemining;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMining;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.format.FormatOptionProvider;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.BaseCompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;
import com.microsoft.copilot.eclipse.ui.utils.CompletionUtils;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * A provider for ghost text.
 */
public class GhostTextProvider extends AbstractCodeMiningProvider {

  private FormatOptionProvider formatOptionProvider;

  /**
   * Creates a new GhostTextProvider.
   */
  public GhostTextProvider() {
    this.formatOptionProvider = CopilotCore.getPlugin().getFormatOptionProvider();
  }

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
    BaseCompletionManager manager = editorsManager.getCompletionManagerFor(belongingEditor);
    if (manager == null) {
      return Collections.emptyList();
    }

    List<ICodeMining> codeMinings = manager.getCodeMinings();
    if (codeMinings == null || codeMinings.isEmpty()) {
      return Collections.emptyList();
    }
    IFile file = UiUtils.getFileFromTextEditor(belongingEditor);
    if (file == null) {
      return codeMinings;
    }

    boolean useSpace = formatOptionProvider.useSpace(file);
    if (useSpace) {
      return codeMinings;
    }

    int tabSize = formatOptionProvider.getTabSize(file);
    for (ICodeMining codeMining : codeMinings) {
      // replace the beginning tabs with spaces, this is because the code mining API does not support tabs
      // rendering, so we need to replace the tabs with spaces to correctly render the indentation. See:
      // AbstractCodeMining.draw() method.
      if (codeMining instanceof AbstractCodeMining cm) {
        String text = cm.getLabel();
        String replacedText = CompletionUtils.replaceTabsWithSpaces(text, tabSize);
        cm.setLabel(replacedText);
      }
    }
    return codeMinings;
  }

}
