package com.microsoft.copilot.eclipse.core.completion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionItem;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;

@ExtendWith(MockitoExtension.class)
class CompletionProviderTests {

  @Mock
  private CopilotLanguageServerConnection mockLsConnection;

  @Mock
  private CompletionListener mockListener;

  @Test
  void testShouldNotifyListenersOnCompletion() throws OperationCanceledException, InterruptedException {
    when(mockLsConnection.getCompletions(any()))
        .thenReturn(CompletableFuture.completedFuture(new CompletionResult(List.of(mock(CompletionItem.class)))));

    CompletionProvider completionProvider = new CompletionProvider(mockLsConnection);
    completionProvider.addCompletionListener(mockListener);
    Position position = new Position(0, 0);
    completionProvider.triggerCompletion("file://test.java", position, 1);
    IJobManager jobManager = Job.getJobManager();
    jobManager.join(CompletionJob.COMPLETION_JOB_FAMILY, new NullProgressMonitor());
    verify(mockLsConnection, times(1)).getCompletions(any());
  }

}
