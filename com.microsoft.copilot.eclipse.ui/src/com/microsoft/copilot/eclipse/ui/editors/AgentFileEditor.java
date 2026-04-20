// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.editors;

import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.ui.editors.text.TextEditor;

import com.microsoft.copilot.eclipse.ui.completion.AgentFileContentAssistProcessor;
import com.microsoft.copilot.eclipse.ui.i18n.Messages;

/**
 * Text editor for .agent.md files with model auto-completion support.
 */
public class AgentFileEditor extends TextEditor {

  /**
   * Constructs a new AgentFileEditor with custom source viewer configuration.
   */
  public AgentFileEditor() {
    super();
    setSourceViewerConfiguration(new AgentFileSourceViewerConfiguration());
  }

  /**
   * Source viewer configuration for .agent.md files.
   * Provides content assist for the model: field in YAML frontmatter.
   */
  private static class AgentFileSourceViewerConfiguration extends SourceViewerConfiguration {

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
      ContentAssistant assistant = new ContentAssistant();

      // Set our custom processor for all content types
      AgentFileContentAssistProcessor processor = new AgentFileContentAssistProcessor();
      assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
      assistant.setContentAssistProcessor(processor, "__dftl_partition_content_type");

      // Configure the assistant with Eclipse-typical settings
      assistant.enableAutoActivation(true);
      assistant.setAutoActivationDelay(0);
      assistant.enableColoredLabels(true);
      assistant.setShowEmptyList(true); // Eclipse shows empty list with message
      assistant.setStatusLineVisible(true); // Eclipse shows status messages
      assistant.setStatusMessage(Messages.agentFileEditor_contentAssist_statusMessage);
      assistant.setEmptyMessage(Messages.agentFileEditor_contentAssist_emptyMessage);
      assistant.setRepeatedInvocationMode(true); // Allow Ctrl+Space repeated invocation
      assistant.setRepeatedInvocationTrigger(
          KeySequence.getInstance(KeyStroke.getInstance(SWT.CTRL, ' ')));
      assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);

      return assistant;
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
      // Return null to disable syntax highlighting that might interfere
      // Let the underlying editor handle basic text presentation
      return super.getPresentationReconciler(sourceViewer);
    }

    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
      return new String[] {
          IDocument.DEFAULT_CONTENT_TYPE
      };
    }
  }
}
