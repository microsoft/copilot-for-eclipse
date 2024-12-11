package com.microsoft.copilot.eclipse.ui.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionDocument;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;

@ExtendWith(MockitoExtension.class)
class CompletionJobTests {

  private CopilotLanguageServerConnection mockLsConnection;
  private CompletionJob completionJob;

  @BeforeEach
  public void setUp() {
    mockLsConnection = mock(CopilotLanguageServerConnection.class);
    completionJob = new CompletionJob(mockLsConnection);
  }

  @Test
  void testTriggerCompletionJobWithoutParams() throws InterruptedException {
    completionJob.schedule();
    completionJob.join();

    IStatus status = completionJob.getResult();

    assertEquals(IStatus.ERROR, status.getSeverity());
  }

  @Test
  void testCancelCompletionJob() throws InterruptedException {
    completionJob.schedule(500L);
    completionJob.cancel();
    completionJob.join();

    IStatus status = completionJob.getResult();

    if (status != null) {
      assertEquals(IStatus.CANCEL, status.getSeverity());
    }
  }

  @Test
  void testTriggerCompletionJobWithParams() throws InterruptedException {
    CompletionDocument document = mock(CompletionDocument.class);
    CompletionParams params = new CompletionParams(document);
    completionJob.setCompletionParams(params);

    CompletionResult expectedResult = new CompletionResult(new ArrayList<>());
    CompletableFuture<CompletionResult> future = CompletableFuture.completedFuture(expectedResult);
    when(mockLsConnection.getCompletions(params)).thenReturn(future);
    completionJob.schedule();
    completionJob.join();

    IStatus status = completionJob.getResult();
    assertEquals(IStatus.OK, status.getSeverity());
    assertEquals(expectedResult, completionJob.getCompletionResult());
  }
}
