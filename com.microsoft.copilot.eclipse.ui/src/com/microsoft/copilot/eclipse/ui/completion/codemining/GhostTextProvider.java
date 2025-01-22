package com.microsoft.copilot.eclipse.ui.completion.codemining;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.format.FormatOptionProvider;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;

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
    CompletionManager manager = editorsManager.getCompletionManagerFor(belongingEditor);
    if (manager == null) {
      return Collections.emptyList();
    }

    List<ICodeMining> codeMinings = manager.getCodeMinings();
    if (codeMinings == null || codeMinings.isEmpty()) {
      return Collections.emptyList();
    }
    IFile file = getFileFromEditor(belongingEditor);
    if (file == null) {
      return codeMinings;
    }

    boolean useSpace = formatOptionProvider.useSpace(file);
    if (useSpace) {
      return codeMinings;
    }

    int tabSize = formatOptionProvider.getTabSize(file);
    for (ICodeMining codeMining : codeMinings) {
      if (codeMining instanceof BlockGhostText blockGhostText) {
        // replace the beginning tabs with spaces, this is because the code mining API does not support tabs
        // rendering, so we need to replace the tabs with spaces to correctly render the indentation. See:
        // LineHeaderCodeMining.draw() method.
        String text = blockGhostText.getLabel();
        String replacedText = replaceTabsWithSpaces(text, tabSize);
        blockGhostText.setLabel(replacedText);
      }
    }
    return codeMinings;
  }

  private IFile getFileFromEditor(ITextEditor editor) {
    IEditorInput input = editor.getEditorInput();
    if (input instanceof IFileEditorInput fileInput) {
      return fileInput.getFile();
    }
    return null;
  }

  private String replaceTabsWithSpaces(String input, int tabSize) {
    String[] lines = input.split("\n");
    StringBuilder result = new StringBuilder();

    for (String line : lines) {
      result.append(replaceLeadingTabs(line, tabSize)).append("\n");
    }

    // Remove the last newline character
    if (result.length() > 0) {
      result.setLength(result.length() - 1);
    }

    return result.toString();
  }

  private static String replaceLeadingTabs(String line, int tabSize) {
    int tabCount = countLeadingTabs(line);
    String spaces = " ".repeat(tabSize).repeat(tabCount);
    return spaces + line.substring(tabCount);
  }

  private static int countLeadingTabs(String line) {
    int count = 0;
    while (count < line.length() && line.charAt(count) == '\t') {
      count++;
    }
    return count;
  }

}
