package com.microsoft.copilot.eclipse.core.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.completion.CompletionProvider.CompletionJob;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionDocument;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;

@ExtendWith(MockitoExtension.class)
class CompletionJobTests {

  private CopilotLanguageServerConnection mockLsConnection;
  private CompletionJob completionJob;

  @BeforeEach
  public void setUp() {
    mockLsConnection = mock(CopilotLanguageServerConnection.class);
    completionJob = mock(CompletionProvider.class).new CompletionJob(mockLsConnection);
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
  void testShouldTimeoutWhenCompletionTakesTooLong() throws Exception {
    when(mockLsConnection.getCompletions(any())).thenAnswer(invocation -> {
      TimeUnit.SECONDS.sleep(6); // completion will timeout after 5 seconds
      return new CompletableFuture<>();
    });

    CompletionJob job = new CompletionProvider(mockLsConnection, null).new CompletionJob(mockLsConnection);
    Position position = new Position(0, 0);
    CompletionDocument completionDoc = new CompletionDocument("file://test.java", position);
    completionDoc.setVersion(1);
    completionDoc.setInsertSpaces(true);
    completionDoc.setTabSize(4);
    job.setCompletionParams(new CompletionParams(completionDoc));
    job.schedule();

    IJobManager jobManager = Job.getJobManager();
    jobManager.join(CompletionProvider.COMPLETION_JOB_FAMILY, new NullProgressMonitor());

    assertEquals(Status.CANCEL_STATUS, job.getResult());

  }

}
