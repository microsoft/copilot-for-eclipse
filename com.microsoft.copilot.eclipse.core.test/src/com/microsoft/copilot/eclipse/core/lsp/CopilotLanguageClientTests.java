// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.IFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.FeatureFlags;
import com.microsoft.copilot.eclipse.core.chat.service.IChatServiceManager;
import com.microsoft.copilot.eclipse.core.chat.service.IReferencedFileService;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationCapabilities;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ConversationContextParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CurrentEditorContext;
import com.microsoft.copilot.eclipse.core.lsp.protocol.DidChangeFeatureFlagsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.FileStat;
import com.microsoft.copilot.eclipse.core.lsp.protocol.ReadFileResult;
import com.microsoft.copilot.eclipse.core.utils.FileUtils;

@ExtendWith(MockitoExtension.class)
class CopilotLanguageClientTests {

  private CopilotLanguageClient client;

  @Mock
  private CopilotCore plugin;

  @Mock
  private IChatServiceManager chatServiceManager;

  @Mock
  private IReferencedFileService fileService;

  @BeforeEach
  void setUp() {
    client = new CopilotLanguageClient();
  }

  @Test
  void testResolveCurrentEditorSkill() throws InterruptedException, ExecutionException {
    // Arrange
    ConversationContextParams params = new ConversationContextParams("", "",
        ConversationCapabilities.CURRENT_EDITOR_SKILL);
    IFile file = mock(IFile.class);
    String expectedUri = "file:///path/to/file.txt";

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class);
        MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {

      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getChatServiceManager()).thenReturn(chatServiceManager);
      when(chatServiceManager.getReferencedFileService()).thenReturn(fileService);
      when(fileService.getCurrentFile()).thenReturn(file);
      fileUtilsMock.when(() -> FileUtils.getResourceUri(file)).thenReturn(expectedUri);

      // Act
      CompletableFuture<Object[]> future = client.getConversationContext(params);
      Object[] result = future.get();

      // Assert
      assertNotNull(result);
      assertEquals(2, result.length);
      assertEquals(CurrentEditorContext.class, result[0].getClass());
      assertEquals(expectedUri, ((CurrentEditorContext) result[0]).getUri());
      assertNull(result[1]);
    }
  }

  @Test
  void testResolveCurrentEditorSkillWithVisibleEditorUri() throws InterruptedException, ExecutionException {
    // Arrange
    ConversationContextParams params = new ConversationContextParams("", "",
        ConversationCapabilities.CURRENT_EDITOR_SKILL);
    String expectedUri = "copilot-visible-editor://current/1";

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getChatServiceManager()).thenReturn(chatServiceManager);
      when(chatServiceManager.getReferencedFileService()).thenReturn(fileService);
      when(fileService.getCurrentFile()).thenReturn(null);
      when(fileService.getCurrentEditorUri()).thenReturn(expectedUri);

      // Act
      CompletableFuture<Object[]> future = client.getConversationContext(params);
      Object[] result = future.get();

      // Assert
      assertNotNull(result);
      assertEquals(2, result.length);
      assertEquals(CurrentEditorContext.class, result[0].getClass());
      assertEquals(expectedUri, ((CurrentEditorContext) result[0]).getUri());
      assertNull(result[1]);
    }
  }

  @Test
  void testResolveCurrentEditorSkillWithoutFileOrVisibleEditorUri() throws InterruptedException, ExecutionException {
    // Arrange
    ConversationContextParams params = new ConversationContextParams("", "",
        ConversationCapabilities.CURRENT_EDITOR_SKILL);

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getChatServiceManager()).thenReturn(chatServiceManager);
      when(chatServiceManager.getReferencedFileService()).thenReturn(fileService);
      when(fileService.getCurrentFile()).thenReturn(null);
      when(fileService.getCurrentEditorUri()).thenReturn(null);

      // Act
      CompletableFuture<Object[]> future = client.getConversationContext(params);
      Object[] result = future.get();

      // Assert
      assertNotNull(result);
      assertEquals(2, result.length);
      assertNull(result[0]);
      assertNull(result[1]);
    }
  }

  @Test
  void testReadFileFallsBackToCurrentEditorWhenFileNotFound() throws InterruptedException, ExecutionException {
    // Arrange
    String uri = "copilot-visible-editor://current/1";
    ReadFileResult currentEditorResult = new ReadFileResult("class Example {}", null);

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getChatServiceManager()).thenReturn(chatServiceManager);
      when(chatServiceManager.getReferencedFileService()).thenReturn(fileService);
      when(fileService.readCurrentEditor(uri)).thenReturn(currentEditorResult);

      // Act
      ReadFileResult result = client.readFile(uri).get();

      // Assert
      assertSame(currentEditorResult, result);
    }
  }

  @Test
  void testReadFilePreservesFileNotFoundWhenCurrentEditorFallbackMissing()
      throws InterruptedException, ExecutionException {
    // Arrange
    String uri = "copilot-visible-editor://current/1";

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getChatServiceManager()).thenReturn(chatServiceManager);
      when(chatServiceManager.getReferencedFileService()).thenReturn(fileService);
      when(fileService.readCurrentEditor(uri)).thenReturn(null);

      // Act
      ReadFileResult result = client.readFile(uri).get();

      // Assert
      assertEquals("file not found: " + uri, result.getText());
      assertNull(result.getStat());
    }
  }

  @Test
  void testReadFileDoesNotFallbackToCurrentEditorWhenFileFound()
      throws InterruptedException, ExecutionException {
    // Arrange
    String uri = "file:///path/to/file.txt";
    FileStat stat = new FileStat();
    stat.setSize(18);
    ReadFileResult fileResult = new ReadFileResult("class Example {}", stat);

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class);
        MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getChatServiceManager()).thenReturn(chatServiceManager);
      when(chatServiceManager.getReferencedFileService()).thenReturn(fileService);
      fileUtilsMock.when(() -> FileUtils.readFileWithStats(uri)).thenReturn(fileResult);

      // Act
      ReadFileResult result = client.readFile(uri).get();

      // Assert
      assertSame(fileResult, result);
      verify(fileService, never()).readCurrentEditor(uri);
    }
  }

  @Test
  void testOnDidChangeFeatureFlags() {
    // Arrange
    DidChangeFeatureFlagsParams params = new DidChangeFeatureFlagsParams();
    Map<String, String> featureFlags = new HashMap<>();
    featureFlags.put("agent_mode", "1");
    featureFlags.put("mcp", "0");
    params.setFeatureFlags(featureFlags);
    params.setByokEnabled(false);

    FeatureFlags mockFeatureFlags = mock(FeatureFlags.class);

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getFeatureFlags()).thenReturn(mockFeatureFlags);

      // Act
      client.onDidChangeFeatureFlags(params);

      // Assert
      verify(mockFeatureFlags).setAgentModeEnabled(true);
      verify(mockFeatureFlags).setMcpEnabled(false);
      verify(mockFeatureFlags).setByokEnabled(false);
    }
  }

  @Test
  void testOnDidChangeFeatureFlagsWithEmptyFeatureFlags() {
    // Arrange
    DidChangeFeatureFlagsParams params = new DidChangeFeatureFlagsParams();
    Map<String, String> featureFlags = new HashMap<>();
    params.setFeatureFlags(featureFlags);

    FeatureFlags mockFeatureFlags = mock(FeatureFlags.class);

    try (MockedStatic<CopilotCore> copilotCoreMock = Mockito.mockStatic(CopilotCore.class)) {
      copilotCoreMock.when(CopilotCore::getPlugin).thenReturn(plugin);
      when(plugin.getFeatureFlags()).thenReturn(mockFeatureFlags);

      // Act
      client.onDidChangeFeatureFlags(params);

      // Assert - should by default enable agent mode, MCP and editor preview
      verify(mockFeatureFlags).setAgentModeEnabled(true);
      verify(mockFeatureFlags).setMcpEnabled(true);
      verify(mockFeatureFlags).setByokEnabled(true);
    }
  }
}
