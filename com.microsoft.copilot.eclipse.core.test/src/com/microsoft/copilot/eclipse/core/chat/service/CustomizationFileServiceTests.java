// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CustomizationFileInfo;

class CustomizationFileServiceTests {

  @TempDir
  Path tempDir;

  @Test
  void testRefreshAllAsync_ignoresResultOfSupersededRefresh() {
    CopilotLanguageServerConnection lsConnection = mock(CopilotLanguageServerConnection.class);
    CompletableFuture<CustomizationFileInfo[]> olderRefresh = new CompletableFuture<>();
    CompletableFuture<CustomizationFileInfo[]> newerRefresh = new CompletableFuture<>();
    when(lsConnection.listCustomInstructions(any())).thenReturn(olderRefresh, newerRefresh);
    when(lsConnection.listCustomSkills(any())).thenReturn(emptyResult());
    when(lsConnection.listCustomPrompts(any())).thenReturn(emptyResult());
    when(lsConnection.listCustomAgents(any())).thenReturn(emptyResult());
    Path staleFile = tempDir.resolve("stale.instructions.md");
    Path currentFile = tempDir.resolve("current.instructions.md");

    CustomizationFileService service = new CustomizationFileService(lsConnection);
    try {
      service.refreshAllAsync();
      verify(lsConnection, timeout(5000).times(1)).listCustomInstructions(any());
      service.refreshAllAsync();
      verify(lsConnection, timeout(5000).times(2)).listCustomInstructions(any());

      newerRefresh.complete(new CustomizationFileInfo[] { toFileInfo(currentFile) });
      olderRefresh.complete(new CustomizationFileInfo[] { toFileInfo(staleFile) });

      assertEquals(Set.of(currentFile.toAbsolutePath().normalize()), service.getCustomizationFiles());
    } finally {
      service.dispose();
    }
  }

  private static CompletableFuture<CustomizationFileInfo[]> emptyResult() {
    return CompletableFuture.completedFuture(new CustomizationFileInfo[0]);
  }

  private static CustomizationFileInfo toFileInfo(Path file) {
    return new CustomizationFileInfo(null, file.getFileName().toString(), file.toUri().toString(), "local");
  }
}
