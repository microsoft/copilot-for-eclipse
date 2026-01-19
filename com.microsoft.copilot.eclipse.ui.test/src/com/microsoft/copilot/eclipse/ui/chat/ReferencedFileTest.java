package com.microsoft.copilot.eclipse.ui.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.protocol.ChatMode;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotModel;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatFontService;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;
import com.microsoft.copilot.eclipse.ui.chat.services.McpConfigService;
import com.microsoft.copilot.eclipse.ui.chat.services.ModelService;
import com.microsoft.copilot.eclipse.ui.chat.services.ReferencedFileService;
import com.microsoft.copilot.eclipse.ui.chat.services.UserPreferenceService;
import com.microsoft.copilot.eclipse.ui.utils.SwtUtils;

@ExtendWith(MockitoExtension.class)
class ReferencedFileTest {

  @Mock
  private UserPreferenceService mockUserPreferenceService;
  @Mock
  private ModelService mockModelService;
  @Mock
  private ChatServiceManager mockChatServiceManager;
  @Mock
  private ReferencedFileService mockReferencedFileService;
  @Mock
  private IFile mockImageFile;
  @Mock
  private IFile mockTextFile;
  @Mock
  private CopilotUi mockCopilotUi;
  @Mock
  private CopilotModel mockModel;
  @Mock
  private McpConfigService mockMcpConfigService;
  @Mock
  private ChatFontService mockChatFontService;

  private Shell shell;
  private ActionBar actionBar;
  private MockedStatic<CopilotUi> mockedCopilotUi;

  @BeforeEach
  void setUp() {
    SwtUtils.invokeOnDisplayThread(() -> {
      setupSwtComponents();
      setupMockFiles();
      setupMockServices();
    });
  }

  @AfterEach
  void tearDown() {
    SwtUtils.invokeOnDisplayThread(() -> {
      if (actionBar != null && !actionBar.isDisposed()) {
        actionBar.dispose();
      }
      if (shell != null && !shell.isDisposed()) {
        shell.dispose();
      }
    });
    if (mockedCopilotUi != null) {
      mockedCopilotUi.close();
    }
  }

  private void setupSwtComponents() {
    shell = new Shell(Display.getDefault());
  }

  private void setupMockFiles() {
    when(mockImageFile.getName()).thenReturn("test-image.png");
    when(mockImageFile.getFileExtension()).thenReturn("png");

    when(mockTextFile.getName()).thenReturn("test-file.txt");
    when(mockTextFile.getFileExtension()).thenReturn("txt");
  }

  private void setupMockServices() {
    mockedCopilotUi = mockStatic(CopilotUi.class);
    mockedCopilotUi.when(CopilotUi::getPlugin).thenReturn(mockCopilotUi);

    when(mockModel.getModelName()).thenReturn("test-model");
    when(mockModelService.getActiveModel()).thenReturn(mockModel);
    when(mockChatServiceManager.getUserPreferenceService()).thenReturn(mockUserPreferenceService);
    when(mockChatServiceManager.getModelService()).thenReturn(mockModelService);
    when(mockChatServiceManager.getReferencedFileService()).thenReturn(mockReferencedFileService);
    when(mockChatServiceManager.getMcpConfigService()).thenReturn(mockMcpConfigService);
    when(mockChatServiceManager.getChatFontService()).thenReturn(mockChatFontService);
    when(mockUserPreferenceService.getActiveChatMode()).thenReturn(ChatMode.Ask);
    when(mockCopilotUi.getChatServiceManager()).thenReturn(mockChatServiceManager);
    actionBar = spy(new ActionBar(shell, SWT.NONE, mockChatServiceManager));
  }

  /**
   * Test ActionBar creates ReferencedFiles with correct strikeThrough state based on model vision support.
   */
  @Test
  void testStrikeThroughBehavior() {
    SwtUtils.invokeOnDisplayThread(() -> {
      List<IFile> testFiles = Arrays.asList(mockImageFile, mockTextFile);

      callUpdateReferencedFilesInternal(actionBar, testFiles, true);

      ReferencedFile imageWithVisionWidget = getReferencedFileWidgetByReflection(actionBar, mockImageFile);
      assertNotNull(imageWithVisionWidget, "Image widget should be created");
      assertFalse(imageWithVisionWidget.isFileUnSupported(),
          "Image file should not have strikethrough when vision is supported");

      ReferencedFile textWithVisionWidget = getReferencedFileWidgetByReflection(actionBar, mockTextFile);
      assertNotNull(textWithVisionWidget, "Text widget should be created");
      assertFalse(textWithVisionWidget.isFileUnSupported(),
          "Text file should not have strikethrough when vision is supported");

      callUpdateReferencedFilesInternal(actionBar, testFiles, false);

      ReferencedFile imageWithoutVisionWidget = getReferencedFileWidgetByReflection(actionBar, mockImageFile);
      assertNotNull(imageWithoutVisionWidget, "Image widget should be created");
      assertTrue(imageWithoutVisionWidget.isFileUnSupported(),
          "Image file should have strikethrough when vision is not supported");

      ReferencedFile textWithoutVisionWidget = getReferencedFileWidgetByReflection(actionBar, mockTextFile);
      assertNotNull(textWithoutVisionWidget, "Text widget should be created");
      assertFalse(textWithoutVisionWidget.isFileUnSupported(),
          "Text file should not have strikethrough regardless of vision support");
    });
  }

  /**
   * Helper method to call private updateReferencedFilesInternal method using reflection.
   */
  private void callUpdateReferencedFilesInternal(ActionBar actionBar, List<IFile> files, boolean supportVision) {
    try {
      Method method = ActionBar.class.getDeclaredMethod("updateReferencedFilesInternal", List.class, boolean.class);
      method.setAccessible(true);
      method.invoke(actionBar, files, supportVision);
    } catch (Exception e) {
      throw new RuntimeException("Failed to call updateReferencedFilesInternal", e);
    }
  }

  private ReferencedFile getReferencedFileWidgetByReflection(ActionBar actionBar, IFile file) {
    try {
      Field cmpFileRefField = ActionBar.class.getDeclaredField("cmpFileRef");
      cmpFileRefField.setAccessible(true);
      Composite cmpFileRef = (Composite) cmpFileRefField.get(actionBar);

      for (Control child : cmpFileRef.getChildren()) {
        if (child instanceof ReferencedFile) {
          ReferencedFile refFile = (ReferencedFile) child;
          if (file.equals(refFile.getFile())) {
            return refFile;
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to get ReferencedFile widget by reflection", e);
    }
    return null;
  }
}
