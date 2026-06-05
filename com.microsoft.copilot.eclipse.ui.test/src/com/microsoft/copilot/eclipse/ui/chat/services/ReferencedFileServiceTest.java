// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
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
  private IEditorReference remainingEditorReference;

  @Mock
  private IWorkbenchPartReference closedPartReference;

  @Mock
  private IEditorPart closedEditor;

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

      ReferencedFileService service = new ReferencedFileService();
      try {
        IPartListener2 listener = getRegisteredPartListener();
        when(closedPartReference.getPart(false)).thenReturn(closedEditor);
        uiUtils.when(() -> UiUtils.getFileFromEditorPart(closedEditor)).thenReturn(currentFile);
        uiUtils.when(UiUtils::getActivePage).thenReturn(activePage);
        when(activePage.getEditorReferences()).thenReturn(new IEditorReference[] { remainingEditorReference });

        setCurrentFile(service, currentFile);
        assertSame(currentFile, service.getCurrentFile());

        listener.partClosed(closedPartReference);

        assertNull(service.getCurrentFile());
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

  private static void setCurrentFile(ReferencedFileService service, IFile file) throws Exception {
    Object currentFileObservable = getField(service, "currentFileObservable");
    runOnDisplayThread(() -> invokeSetValue(currentFileObservable, file));
  }

  private static Object getField(Object target, String fieldName) throws NoSuchFieldException,
      IllegalAccessException {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
  }

  private static void runOnDisplayThread(Runnable runnable) {
    if (Display.getCurrent() != null) {
      runnable.run();
      return;
    }
    Display.getDefault().syncExec(runnable);
  }

  private static void invokeSetValue(Object observable, Object value) {
    try {
      Method setValue = observable.getClass().getMethod("setValue", Object.class);
      setValue.invoke(observable, value);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Failed to set observable value", e);
    }
  }
}
