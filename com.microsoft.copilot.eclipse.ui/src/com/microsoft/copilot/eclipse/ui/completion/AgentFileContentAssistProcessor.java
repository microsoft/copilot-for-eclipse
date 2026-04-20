// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ModelService;

/**
 * Content assist processor for .agent.md files.
 * Provides auto-completion for the model: field in YAML frontmatter.
 */
public class AgentFileContentAssistProcessor implements IContentAssistProcessor {

  @Override
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    try {
      IDocument document = viewer.getDocument();
      int line = document.getLineOfOffset(offset);
      int lineStartOffset = document.getLineOffset(line);
      String lineText = document.get(lineStartOffset, offset - lineStartOffset);

      // Check if we're on a line that starts with "model:"
      if (lineText.trim().startsWith("model:")) {
        return createModelCompletionProposals(document, offset, lineText);
      }

    } catch (BadLocationException e) {
      CopilotCore.LOGGER.error("Error computing completion proposals", e);
    }

    return new ICompletionProposal[0];
  }

  /**
   * Create completion proposals for model names.
   */
  private ICompletionProposal[] createModelCompletionProposals(IDocument document, int offset, String lineText) {
    List<ICompletionProposal> proposals = new ArrayList<>();

    try {
      // Get available models from ModelService
      CopilotUi copilotUi = CopilotUi.getPlugin();
      if (copilotUi == null) {
        return new ICompletionProposal[0];
      }

      ModelService modelService = copilotUi.getChatServiceManager().getModelService();
      if (modelService == null) {
        return new ICompletionProposal[0];
      }

      Map<String, CopilotModel> models = modelService.getModels();

      if (models == null || models.isEmpty()) {
        return new ICompletionProposal[0];
      }

      // Extract the current value after "model:"
      String modelPrefix = "";
      int modelKeyStart = lineText.indexOf("model:");
      if (modelKeyStart >= 0) {
        String afterModel = lineText.substring(modelKeyStart + "model:".length()).trim();
        modelPrefix = afterModel;
      }

      // Calculate replacement offset and length
      int replaceOffset = offset - modelPrefix.length();
      int replaceLength = modelPrefix.length();

      // Create proposals for each model
      for (Map.Entry<String, CopilotModel> entry : models.entrySet()) {
        CopilotModel model = entry.getValue();
        String modelId = model.getId();
        String displayName = model.getModelName() != null ? model.getModelName() : modelId;
        String provider = model.getProviderName() != null && !model.getProviderName().isEmpty()
            ? model.getProviderName() : "copilot";

        // Filter based on prefix
        if (modelPrefix.isEmpty() || modelId.toLowerCase().contains(modelPrefix.toLowerCase())
            || displayName.toLowerCase().contains(modelPrefix.toLowerCase())) {

          // Format: model_name (provider)
          String displayString = displayName + " (" + provider + ")";
          String replacementText = displayName + " (" + provider + ")";
          String additionalInfo = buildModelInfo(model);

          ICompletionProposal proposal = new CompletionProposal(
              replacementText,            // replacement string
              replaceOffset,              // replacement offset
              replaceLength,              // replacement length
              replacementText.length(),   // cursor position after insertion
              null,                       // image
              displayString,              // display string
              null,                       // context information
              additionalInfo              // additional proposal info
          );

          proposals.add(proposal);
        }
      }
    } catch (Exception e) {
      CopilotCore.LOGGER.error("Error creating model completion proposals", e);
    }

    return proposals.toArray(new ICompletionProposal[0]);
  }

  /**
   * Build additional information string for a model.
   */
  private String buildModelInfo(CopilotModel model) {
    StringBuilder info = new StringBuilder();

    if (model.getModelName() != null) {
      info.append(model.getModelName()).append("\n\n");
    }

    info.append("ID: ").append(model.getId()).append("\n");

    if (model.getModelFamily() != null) {
      info.append("Family: ").append(model.getModelFamily()).append("\n");
    }

    if (model.getProviderName() != null) {
      info.append("Provider: ").append(model.getProviderName()).append("\n");
    }

    List<String> capabilities = new ArrayList<>();
    if (model.getCapabilities() != null
        && model.getCapabilities().supports() != null
        && model.getCapabilities().supports().vision()) {
      capabilities.add("Vision");
    }

    if (!capabilities.isEmpty()) {
      info.append("Capabilities: ").append(String.join(", ", capabilities)).append("\n");
    }

    return info.toString().trim();
  }

  @Override
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
    return new IContextInformation[0];
  }

  @Override
  public char[] getCompletionProposalAutoActivationCharacters() {
    return new char[] { ':', ' ' };
  }

  @Override
  public char[] getContextInformationAutoActivationCharacters() {
    return new char[0];
  }

  @Override
  public IContextInformationValidator getContextInformationValidator() {
    return null;
  }

  @Override
  public String getErrorMessage() {
    return null;
  }
}
