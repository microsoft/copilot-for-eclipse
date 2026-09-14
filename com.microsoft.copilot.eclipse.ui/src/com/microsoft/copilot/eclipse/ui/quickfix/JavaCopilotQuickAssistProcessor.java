// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.quickfix;

import java.util.function.BooleanSupplier;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

/**
 * Provides a GitHub Copilot quick fix for problem markers in the Java editor.
 */
public class JavaCopilotQuickAssistProcessor implements IQuickAssistProcessor {
  private final BooleanSupplier isCopilotAvailable;

  /**
   * Creates a Java quick assist processor.
   */
  public JavaCopilotQuickAssistProcessor() {
    this(QuickFixProcessorSupport::isCopilotAvailable);
  }

  JavaCopilotQuickAssistProcessor(BooleanSupplier isCopilotAvailable) {
    this.isCopilotAvailable = isCopilotAvailable;
  }

  @Override
  public boolean hasAssists(IInvocationContext context) throws CoreException {
    if (!isCopilotAvailable.getAsBoolean() || context == null) {
      return false;
    }

    ICompilationUnit compilationUnit = context.getCompilationUnit();
    IResource resource = compilationUnit == null ? null : compilationUnit.getResource();
    if (!(resource instanceof IFile file)) {
      return false;
    }

    IDocument document = new Document(compilationUnit.getBuffer().getContents());
    return QuickFixProcessorSupport.hasOverlappingProblemMarker(file, document, context.getSelectionOffset(),
        context.getSelectionLength());
  }

  @Override
  public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
      throws CoreException {
    IJavaCompletionProposal proposal = createProposal(context);
    return proposal == null ? null : new IJavaCompletionProposal[] { proposal };
  }

  private IJavaCompletionProposal createProposal(IInvocationContext context) throws CoreException {
    if (!isCopilotAvailable.getAsBoolean() || context == null) {
      return null;
    }

    ICompilationUnit compilationUnit = context.getCompilationUnit();
    IResource resource = compilationUnit == null ? null : compilationUnit.getResource();
    if (!(resource instanceof IFile file)) {
      return null;
    }

    IDocument document = new Document(compilationUnit.getBuffer().getContents());
    QuickFixProcessorSupport.ProblemContext problemContext = QuickFixProcessorSupport.findProblemContext(file,
        document, context.getSelectionOffset(),
        context.getSelectionLength());
    if (problemContext.messages().isEmpty()) {
      return null;
    }
    return new JavaCopilotQuickFixProposal(QuickFixProcessorSupport.buildPrompt(problemContext.messages()),
        problemContext.selectionOffset(), problemContext.selectionLength());
  }

  private static final class JavaCopilotQuickFixProposal extends CopilotQuickFixProposal
      implements IJavaCompletionProposal {

    private JavaCopilotQuickFixProposal(String prompt, int selectionOffset, int selectionLength) {
      super(prompt, selectionOffset, selectionLength);
    }

    @Override
    public int getRelevance() {
      return 0;
    }
  }
}
