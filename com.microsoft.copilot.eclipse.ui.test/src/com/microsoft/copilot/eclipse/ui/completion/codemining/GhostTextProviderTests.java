// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.completion.codemining;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.format.FormatOptionProvider;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.completion.CompletionManager;
import com.microsoft.copilot.eclipse.ui.completion.EditorsManager;

@ExtendWith(MockitoExtension.class)
class GhostTextProviderTests {

  @Test
  void testGetCodeMinings() throws BadLocationException {
    CopilotCore copilotCore = mock(CopilotCore.class);
    FormatOptionProvider formatOptionProvider = mock(FormatOptionProvider.class);
    when(copilotCore.getFormatOptionProvider()).thenReturn(formatOptionProvider);
    when(formatOptionProvider.useSpace(any())).thenReturn(false);
    when(formatOptionProvider.getTabSize(any())).thenReturn(2);

    IAdaptable adapter = mock(IAdaptable.class);
    ITextEditor textEditor = mock(ITextEditor.class);
    when(adapter.getAdapter(any())).thenReturn(textEditor);
    IFileEditorInput fileEditorInput = mock(IFileEditorInput.class);
    when(textEditor.getEditorInput()).thenReturn(fileEditorInput);
    IFile file = mock(IFile.class);
    when(fileEditorInput.getFile()).thenReturn(file);
    CopilotUi copilotUi = mock(CopilotUi.class);
    EditorsManager editorsManager = mock(EditorsManager.class);
    when(copilotUi.getEditorsManager()).thenReturn(editorsManager);

    CompletionManager completionManager = mock(CompletionManager.class);
    when(editorsManager.getCompletionManagerFor(textEditor)).thenReturn(completionManager);

    List<ICodeMining> codeMinings = List.of(new BlockGhostText(new Position(0, 0), null, "\t\tfoo\n\tbar"));
    when(completionManager.getCodeMinings()).thenReturn(codeMinings);

    try (MockedStatic<CopilotUi> mockedStaticUi = mockStatic(CopilotUi.class);
        MockedStatic<CopilotCore> mockedStaticCore = mockStatic(CopilotCore.class)) {
      mockedStaticUi.when(CopilotUi::getPlugin).thenReturn(copilotUi);
      mockedStaticCore.when(CopilotCore::getPlugin).thenReturn(copilotCore);

      GhostTextProvider ghostTextProvider = new GhostTextProvider();
      ghostTextProvider.setContext(adapter);
      List<? extends ICodeMining> codeMiningsResult = ghostTextProvider.provideCodeMinings(null, null).join();
      assertEquals(1, codeMiningsResult.size());
      assertEquals("    foo\n  bar", codeMiningsResult.get(0).getLabel());
    }
  }
}
