// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.nes;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.NextEditSuggestionsResult.CopilotInlineEdit;

@ExtendWith(MockitoExtension.class)
class NextEditSuggestionProviderTests {

  @Mock
  CopilotLanguageServerConnection mockLsConnection;

  NextEditSuggestionProvider provider;

  @BeforeEach
  void setUp() {
    provider = new NextEditSuggestionProvider(mockLsConnection);
  }

  @Test
  void testShouldNotifyListenerWhenSuggestionReceived() throws InterruptedException {
    IFile mockFile = mock(IFile.class);
    when(mockFile.getLocation()).thenReturn(new Path("/test/file.java"));
    Position position = new Position(10, 5);
    when(mockLsConnection.getDocumentVersion(any(URI.class))).thenReturn(1);
    CopilotInlineEdit mockEdit = mock(CopilotInlineEdit.class);
    NextEditSuggestionsResult result = new NextEditSuggestionsResult();
    result.setEdits(List.of(mockEdit));
    when(mockLsConnection.getNextEditSuggestions(any(NextEditSuggestionsParams.class)))
        .thenReturn(CompletableFuture.completedFuture(result));
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<CopilotInlineEdit> receivedEdit = new AtomicReference<>();

    NextEditSuggestionListener listener = (file, edit) -> {
      receivedEdit.set(edit);
      latch.countDown();
    };
    provider.addListener(listener);
    // Act
    provider.fetchSuggestion(mockFile, position);
    // Assert
    latch.await(1, TimeUnit.SECONDS);
    assertNotNull(receivedEdit.get());
  }
}
