// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.lsp4j.FileChangeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult.ToolInvocationStatus;
import com.microsoft.copilot.eclipse.ui.CopilotUi;
import com.microsoft.copilot.eclipse.ui.chat.services.ChatServiceManager;

@ExtendWith(MockitoExtension.class)
class EditFileToolTest {

  @TempDir
  Path tempDir;

  @Mock
  private CopilotUi mockCopilotUi;
  @Mock
  private ChatServiceManager mockChatServiceManager;
  @Mock
  private FileToolService mockFileToolService;

  private MockedStatic<CopilotUi> mockedCopilotUi;

  private void setupMocks() {
    mockedCopilotUi = mockStatic(CopilotUi.class);
    mockedCopilotUi.when(CopilotUi::getPlugin).thenReturn(mockCopilotUi);
    when(mockCopilotUi.getChatServiceManager()).thenReturn(mockChatServiceManager);
    when(mockChatServiceManager.getFileToolService()).thenReturn(mockFileToolService);
  }

  @AfterEach
  void tearDown() {
    if (mockedCopilotUi != null) {
      mockedCopilotUi.close();
    }
    FileToolCacheAccessor.clearCaches();
  }

  @Test
  void testInvoke_withExternalLocalFilePath_editsFile() throws Exception {
    setupMocks();
    Path file = tempDir.resolve("target.txt");
    Files.writeString(file, "original");

    LanguageModelToolResult[] results = invokeEdit(file.toString(), "updated");

    assertSuccess(results, "updated");
    assertEquals("updated", Files.readString(file));
    verify(mockFileToolService).addChangedFile(ChangedFile.local(file), FileChangeType.Changed);
  }

  @Test
  void testInvoke_withExternalLocalFileUri_editsFile() throws Exception {
    setupMocks();
    Path file = tempDir.resolve("target.patch");
    Files.writeString(file, "old patch content");

    LanguageModelToolResult[] results = invokeEdit(file.toUri().toString(), "new patch content");

    assertSuccess(results, "new patch content");
    assertEquals("new patch content", Files.readString(file));
    verify(mockFileToolService).addChangedFile(ChangedFile.local(file), FileChangeType.Changed);
  }

  @Test
  void testOnUndoChange_withExternalLocalFile_restoresOriginalContent() throws Exception {
    setupMocks();
    Path file = tempDir.resolve("target-to-undo.txt");
    Files.writeString(file, "original");

    EditFileTool editFileTool = new EditFileTool();
    LanguageModelToolResult[] results = invokeEdit(editFileTool, file.toString(), "updated");
    assertSuccess(results, "updated");

    editFileTool.onUndoChange(ChangedFile.local(file));

    assertEquals("original", Files.readString(file));
  }

  @Test
  void testInvoke_createThenEditExternalLocalFile_preservesEmptyBaseline() throws Exception {
    setupMocks();
    Path file = tempDir.resolve("created-then-edited.txt");
    Path normalizedPath = file.toAbsolutePath().normalize();

    CreateFileTool createFileTool = new CreateFileTool();
    LanguageModelToolResult[] createResults = invokeCreate(createFileTool, file.toString(), "created content");
    assertSuccess(createResults, "File created at: " + normalizedPath);
    assertEquals("", FileToolCacheAccessor.getFileContentCache(normalizedPath));

    EditFileTool editFileTool = new EditFileTool();
    LanguageModelToolResult[] editResults = invokeEdit(editFileTool, file.toString(), "edited content");

    assertSuccess(editResults, "edited content");
    assertEquals("edited content", Files.readString(file));
    assertEquals("", FileToolCacheAccessor.getFileContentCache(normalizedPath));
  }

  @Test
  void testInvoke_withMissingExternalLocalFile_returnsError() throws Exception {
    LanguageModelToolResult[] results = invokeEdit(tempDir.resolve("missing.txt").toString(), "updated");

    assertNotNull(results);
    assertEquals(1, results.length);
    assertEquals(ToolInvocationStatus.error, results[0].getStatus());
  }

  private LanguageModelToolResult[] invokeEdit(String filePath, String code) throws Exception {
    return invokeEdit(new EditFileTool(), filePath, code);
  }

  private LanguageModelToolResult[] invokeEdit(EditFileTool editFileTool, String filePath, String code)
      throws Exception {
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", filePath);
    input.put("code", code);
    input.put("explanation", "test edit");

    return editFileTool.invoke(input, null).get();
  }

  private LanguageModelToolResult[] invokeCreate(CreateFileTool createFileTool, String filePath, String content)
      throws Exception {
    Map<String, Object> input = new HashMap<>();
    input.put("filePath", filePath);
    input.put("content", content);

    return createFileTool.invoke(input, null).get();
  }

  private void assertSuccess(LanguageModelToolResult[] results, String expectedContent) throws IOException {
    assertNotNull(results);
    assertEquals(1, results.length);
    assertEquals(ToolInvocationStatus.success, results[0].getStatus());
    assertEquals(expectedContent, results[0].getContent().get(0).getValue());
  }

  private static final class FileToolCacheAccessor extends EditFileTool {
    private static void clearCaches() {
      fileContentCache.clear();
    }

    private static String getFileContentCache(Path file) {
      return fileContentCache.get(ChangedFile.local(file));
    }
  }
}