package com.microsoft.copilot.eclipse.ui.completion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

@ExtendWith(MockitoExtension.class)
class EditorLifecycleListenerTests {

	@Mock
	private IWorkbenchPartReference partRef;

	@Mock
	private IWorkbenchPart part;

	@Mock
	private IEditorPart editorPart;

	@Mock
	private ITextEditor textEditor;

	@Mock
	private CopilotLanguageServerConnection languageServer;

	@Mock
	private EditorsManager manager;

	@Mock
	private IFile mockFile;

	@Mock
	private IDocument mockDocument;

	private EditorLifecycleListener listener;

	@BeforeEach
	void setUp() throws Exception {
		when(partRef.getPart(anyBoolean())).thenReturn(part);
		when(part.getAdapter(IEditorPart.class)).thenReturn(editorPart);
		when(editorPart.getAdapter(ITextEditor.class)).thenReturn(textEditor);
		listener = new EditorLifecycleListener(languageServer, manager);
	}

	@Test
	void testOpenEditorPartMultipleTimes() throws IOException {
		when(languageServer.connectDocument(any(), any())).thenReturn(new CompletableFuture<LanguageServerWrapper>());
		// Setup: Mock UiUtils.getFileFromEditorPart to return a file
		try (var mockedUiUtils = Mockito.mockStatic(UiUtils.class);
				var lspMockedLSPEclipseUtils = Mockito.mockStatic(LSPEclipseUtils.class)) {
			mockedUiUtils.when(() -> UiUtils.getFileFromEditorPart(editorPart)).thenReturn(mockFile);

			// Mock document retrieval for the textEditor
			lspMockedLSPEclipseUtils.when(() -> LSPEclipseUtils.getDocument(textEditor)).thenReturn(mockDocument);
			lspMockedLSPEclipseUtils.when(() -> LSPEclipseUtils.toUri(mockFile))
					.thenReturn(java.net.URI.create("file:///test.java"));
			lspMockedLSPEclipseUtils.when(() -> LSPEclipseUtils.getFile(mockDocument)).thenReturn(mockFile);

			// Act: Activate the same editor part multiple times
			listener.partActivated(partRef);
			listener.partActivated(partRef);
			listener.partActivated(partRef);

			// Assert: Verify that connectDocument is called only once
			// This is because EditorLifecycleListener maintains a set of editors
			// and only adds each editor once
			verify(languageServer, times(1)).connectDocument(any(IDocument.class), any(IFile.class));
		}
	}
}
