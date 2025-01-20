package com.microsoft.copilot.eclipse.core.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

@ExtendWith(MockitoExtension.class)
class CompletionProviderTests {

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @Mock
  private AuthStatusManager mockStatusManager;

  @Mock
  private CompletionListener mockListener;

  @Test
  void testShouldNotifyListenersOnCompletion() throws OperationCanceledException, InterruptedException, URISyntaxException {
    when(mockStatusManager.getCopilotStatus()).thenReturn(CopilotStatusResult.OK);
    when(mockLsConnection.getCompletions(any()))
        .thenReturn(CompletableFuture.completedFuture(new CompletionResult(List.of(mock(CompletionItem.class)))));
    
    IFile mockFile = mock(IFile.class);
    when(mockFile.getLocation()).thenReturn(new Path("file://test,java"));
    CompletionProvider completionProvider = new CompletionProvider(mockLsConnection, mockStatusManager);
    completionProvider.addCompletionListener(mockListener);
    Position position = new Position(0, 0);
    completionProvider.triggerCompletion(mockFile, position, 1);
    IJobManager jobManager = Job.getJobManager();
    jobManager.join(CompletionProvider.COMPLETION_JOB_FAMILY, new NullProgressMonitor());
    verify(mockLsConnection, times(1)).getCompletions(any());
  }

  @Test
  void testShouldNotTriggerCompletionWhenNotSignedIn() {
    when(mockStatusManager.getCopilotStatus()).thenReturn(CopilotStatusResult.NOT_SIGNED_IN);
    
    IFile mockFile = mock(IFile.class);
    CompletionProvider completionProvider = new CompletionProvider(mockLsConnection, mockStatusManager);
    completionProvider.addCompletionListener(mockListener);
    Position position = new Position(0, 0);
    completionProvider.triggerCompletion(mockFile, position, 1);
    verify(mockLsConnection, never()).getCompletions(any());
  }

  @Test
  void testTriggerCompletionJobWithParams() throws InterruptedException, URISyntaxException {
    when(mockStatusManager.getCopilotStatus()).thenReturn(CopilotStatusResult.OK);
    CompletionItem mockCompletionItem = mock(CompletionItem.class);
    CompletionResult expectedResult = new CompletionResult(List.of(mockCompletionItem));
    CompletableFuture<CompletionResult> future = CompletableFuture.completedFuture(expectedResult);
    when(mockLsConnection.getCompletions(any())).thenReturn(future);
    CompletionProvider completionProvider = new CompletionProvider(mockLsConnection, mockStatusManager);
    IFile mockFile = mock(IFile.class);
    Path mockPath = mock(Path.class);
    when(mockFile.getLocation()).thenReturn(mockPath);
    File mockIoFile = mock(File.class);
    when(mockPath.toFile()).thenReturn(mockIoFile);
    File mockAbsoluteFile = mock(File.class);
    when(mockIoFile.getAbsoluteFile()).thenReturn(mockAbsoluteFile);
    URI mockUri = mock(URI.class);
    when(mockAbsoluteFile.toURI()).thenReturn(mockUri);
    when(mockUri.getPath()).thenReturn("/test.java");
    CompletionListener mockListener = mock(CompletionListener.class);
    completionProvider.addCompletionListener(mockListener);
    completionProvider.triggerCompletion(mockFile, new Position(0, 0), 1);
    IJobManager jobManager = Job.getJobManager();
    jobManager.join(CompletionProvider.COMPLETION_JOB_FAMILY, new NullProgressMonitor());

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockListener).onCompletionResolved(argumentCaptor.capture(), any());

    assertEquals("file:///test.java", argumentCaptor.getValue());
  }

}
