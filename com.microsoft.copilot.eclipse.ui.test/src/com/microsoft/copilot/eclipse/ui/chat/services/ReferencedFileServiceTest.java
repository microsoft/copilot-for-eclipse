// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.ui.utils.UiUtils;

@ExtendWith(MockitoExtension.class)
class ReferencedFileServiceTest {

  @Mock
  private IWorkbench workbench;

  @Mock
  private IWorkbenchWindow window;

  @Mock
  private IPartService partService;

  @Mock
  private IWorkbenchPage activePage;

  @Mock
  private IWorkbenchPage otherPage;

  @Mock
  private IEditorReference closedEditorReference;

  @Mock
  private IEditorReference remainingEditorReference;

  @Mock
  private IEditorPart closedEditor;

  @Mock
  private IFileEditorInput closedEditorInput;

  @Mock
  private IEditorInput nonFileEditorInput;

  @Mock
  private IFile currentFile;

  @Test
  void partClosed_WhenClosedEditorIsCurrentFileAndEditorReferencesRemain_ShouldClearCurrentFile()
      throws Exception {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class);
        MockedStatic<UiUtils> uiUtils = mockStatic(UiUtils.class)) {
      platformUi.when(PlatformUI::getWorkbench).thenReturn(workbench);
      when(workbench.getWorkbenchWindows()).thenReturn(new IWorkbenchWindow[] { window });
      when(window.getPartService()).thenReturn(partService);

      TestReferencedFileService service = new TestReferencedFileService();
      try {
        IPartListener2 listener = getRegisteredPartListener();
        when(closedEditorReference.getEditorInput()).thenReturn(nonFileEditorInput);
        when(closedEditorReference.getPart(false)).thenReturn(closedEditor);
        uiUtils.when(() -> UiUtils.getFileFromEditorPart(closedEditor)).thenReturn(currentFile);
        when(window.getPages()).thenReturn(new IWorkbenchPage[] { activePage });
        when(activePage.getEditorReferences()).thenReturn(new IEditorReference[] { remainingEditorReference });

        service.setCurrentFile(currentFile);
        assertSame(currentFile, service.getCurrentFile());

        listener.partClosed(closedEditorReference);

        assertNull(service.getCurrentFile());
      } finally {
        service.dispose();
      }
    }
  }

  @Test
  void partClosed_WhenEditorPartIsDisposedButReferenceInputIsCurrentFile_ShouldClearCurrentFile()
      throws Exception {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class)) {
      platformUi.when(PlatformUI::getWorkbench).thenReturn(workbench);
      when(workbench.getWorkbenchWindows()).thenReturn(new IWorkbenchWindow[] { window });
      when(window.getPartService()).thenReturn(partService);

      TestReferencedFileService service = new TestReferencedFileService();
      try {
        IPartListener2 listener = getRegisteredPartListener();
        when(closedEditorReference.getEditorInput()).thenReturn(closedEditorInput);
        when(closedEditorInput.getFile()).thenReturn(currentFile);
        when(window.getPages()).thenReturn(new IWorkbenchPage[] { activePage });
        when(activePage.getEditorReferences()).thenReturn(new IEditorReference[] { remainingEditorReference });

        service.setCurrentFile(currentFile);
        assertSame(currentFile, service.getCurrentFile());

        listener.partClosed(closedEditorReference);

        assertNull(service.getCurrentFile());
      } finally {
        service.dispose();
      }
    }
  }

  @Test
  void partClosed_WhenActivePageHasNoEditorsButAnotherPageHasEditors_ShouldKeepCurrentFile()
      throws Exception {
    try (MockedStatic<PlatformUI> platformUi = mockStatic(PlatformUI.class);
        MockedStatic<UiUtils> uiUtils = mockStatic(UiUtils.class)) {
      platformUi.when(PlatformUI::getWorkbench).thenReturn(workbench);
      when(workbench.getWorkbenchWindows()).thenReturn(new IWorkbenchWindow[] { window });
      when(window.getPartService()).thenReturn(partService);

      TestReferencedFileService service = new TestReferencedFileService();
      try {
        IPartListener2 listener = getRegisteredPartListener();
        when(closedEditorReference.getEditorInput()).thenReturn(nonFileEditorInput);
        when(closedEditorReference.getPart(false)).thenReturn(closedEditor);
        uiUtils.when(() -> UiUtils.getFileFromEditorPart(closedEditor)).thenReturn(null);
        when(window.getPages()).thenReturn(new IWorkbenchPage[] { activePage, otherPage });
        when(activePage.getEditorReferences()).thenReturn(new IEditorReference[0]);
        when(otherPage.getEditorReferences()).thenReturn(new IEditorReference[] { remainingEditorReference });

        service.setCurrentFile(currentFile);
        assertSame(currentFile, service.getCurrentFile());

        listener.partClosed(closedEditorReference);

        assertSame(currentFile, service.getCurrentFile());
      } finally {
        service.dispose();
      }
    }
  }

  private IPartListener2 getRegisteredPartListener() {
    ArgumentCaptor<IPartListener2> listenerCaptor = ArgumentCaptor.forClass(IPartListener2.class);
    verify(partService).addPartListener(listenerCaptor.capture());
    return listenerCaptor.getValue();
  }

  private static class TestReferencedFileService extends ReferencedFileService {
    void setCurrentFile(IFile file) {
      setCurrentFileForTest(file);
    }
  }
}
