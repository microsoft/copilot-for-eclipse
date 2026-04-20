// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion.codemining;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.texteditor.ITextEditor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.ui.handlers.ConfigureToolsCommandHandler;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

/**
 * Code mining provider that displays a clickable "Configure Tools" link next to the tools: line in .agent.md files.
 */
public class ToolConfigurationCodeMiningProvider extends AbstractCodeMiningProvider {

  private static final Pattern TOOLS_LINE_PATTERN = Pattern.compile("^\\s*tools:\\s*(.*)$", Pattern.MULTILINE);

  @Override
  public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer,
      IProgressMonitor monitor) {
    return CompletableFuture.supplyAsync(() -> {
      IDocument document = viewer.getDocument();
      if (document == null) {
        return Collections.emptyList();
      }

      // Check if this is an .agent.md file
      if (!isAgentFile()) {
        return Collections.emptyList();
      }

      List<ICodeMining> minings = new ArrayList<>();
      String content = document.get();

      // Find the tools: line in the YAML frontmatter
      Matcher matcher = TOOLS_LINE_PATTERN.matcher(content);
      if (matcher.find()) {
        try {
          int lineNumber = document.getLineOfOffset(matcher.start());
          minings.add(new ToolConfigurationCodeMining(lineNumber, document, this, viewer));
        } catch (BadLocationException e) {
          CopilotCore.LOGGER.error("Failed to create tool configuration code mining", e);
        }
      }

      return minings;
    });
  }

  /**
   * Check if the current editor is editing an .agent.md file.
   */
  private boolean isAgentFile() {
    ITextEditor editor = getAdapter(ITextEditor.class);
    if (editor == null) {
      return false;
    }

    IFile file = UiUtils.getFileFromTextEditor(editor);
    return UiUtils.isAgentFile(file);
  }

  /**
   * Code mining that displays "Configure Tools..." link.
   */
  private static class ToolConfigurationCodeMining extends LineHeaderCodeMining {

    private final ITextViewer viewer;

    public ToolConfigurationCodeMining(int lineNumber, IDocument document,
        ToolConfigurationCodeMiningProvider provider, ITextViewer viewer)
        throws BadLocationException {
      super(lineNumber, document, provider);
      this.viewer = viewer;
      setLabel("Configure Tools...");
    }

    @Override
    protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isResolved() {
      return true;
    }

    @Override
    public Consumer<MouseEvent> getAction() {
      return new Consumer<MouseEvent>() {
        @Override
        public void accept(MouseEvent e) {
          // Get the editor from the provider
          if (getProvider() instanceof ToolConfigurationCodeMiningProvider) {
            ToolConfigurationCodeMiningProvider tcProvider = (ToolConfigurationCodeMiningProvider) getProvider();
            ITextEditor editor = tcProvider.getAdapter(ITextEditor.class);

            if (editor != null) {
              IFile file = UiUtils.getFileFromTextEditor(editor);
              if (file != null) {
                // Open the tool configuration dialog
                ConfigureToolsCommandHandler.openToolConfigurationDialog(editor.getSite().getShell(), file);
              }
            }
          }
        }
      };
    }
  }
}
