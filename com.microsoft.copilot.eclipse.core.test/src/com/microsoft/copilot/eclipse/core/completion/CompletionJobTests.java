// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.completion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.AuthStatusManager;
import com.microsoft.copilot.eclipse.core.completion.CompletionProvider.CompletionJob;
import com.microsoft.copilot.eclipse.core.lsp.CopilotLanguageServerConnection;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionDocument;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionParams;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CompletionResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.CopilotStatusResult;

@ExtendWith(MockitoExtension.class)
class CompletionJobTests {

  @Mock
  private IResource mockResource;

  private CopilotLanguageServerConnection mockLsConnection;
  private CompletionJob completionJob;

  @BeforeEach
  void setUp() {
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
    when(mockResource.exists()).thenReturn(true);
    when(mockLsConnection.getCompletions(any())).thenAnswer(invocation -> {
      TimeUnit.SECONDS.sleep(6); // completion will timeout after 5 seconds
      return new CompletableFuture<>();
    });

    Position position = new Position(0, 0);
    CompletionDocument completionDoc = new CompletionDocument("file://test.java", position);
    completionDoc.setVersion(1);
    CompletionJob job = new CompletionProvider(mockLsConnection, null).new CompletionJob(mockLsConnection);
    completionDoc.setInsertSpaces(true);
    completionDoc.setTabSize(4);
    job.setCompletionParams(new CompletionParams(completionDoc));
    job.setFile(mockResource);
    job.setEnableCompletion(true); // Enable completion to trigger the timeout logic
    job.schedule();

    IJobManager jobManager = Job.getJobManager();
    jobManager.join(CompletionProvider.COMPLETION_JOB_FAMILY, new NullProgressMonitor());

    assertEquals(Status.CANCEL_STATUS, job.getResult());

  }

  @Test
  void testTriggerCompletionJobWhenCopilotIsSignedOutNotUsingEclipse() throws InterruptedException {
    CopilotStatusResult expectedResult = new CopilotStatusResult();
    expectedResult.setStatus(CopilotStatusResult.ERROR);
    AuthStatusManager authStatusManager = mock(AuthStatusManager.class);
    when(authStatusManager.setCopilotStatus(CopilotStatusResult.ERROR)).thenReturn(expectedResult);
    when(authStatusManager.getCopilotStatus()).thenReturn(CopilotStatusResult.ERROR);
    CompletableFuture<CompletionResult> future = new CompletableFuture<>();
    future.completeExceptionally(new ExecutionException("Not signed in", new Throwable()));
    when(mockLsConnection.getCompletions(any())).thenReturn(future);
    when(mockResource.exists()).thenReturn(true);

    Position position = new Position(0, 0);
    CompletionDocument completionDoc = new CompletionDocument("file://test.java", position);
    completionDoc.setVersion(1);
    CompletionJob job = new CompletionProvider(mockLsConnection, authStatusManager).new CompletionJob(mockLsConnection);
    completionDoc.setInsertSpaces(true);
    completionDoc.setTabSize(4);
    job.setCompletionParams(new CompletionParams(completionDoc));
    job.setFile(mockResource);
    job.setEnableCompletion(true);
    job.schedule();

    IJobManager jobManager = Job.getJobManager();
    jobManager.join(CompletionProvider.COMPLETION_JOB_FAMILY, new NullProgressMonitor());

    assertEquals(IStatus.OK, job.getResult().getSeverity());
    assertEquals(CopilotStatusResult.ERROR, authStatusManager.getCopilotStatus());
  }

  @Test
  void testWhenFileDoesNotExist() throws InterruptedException {
    when(mockResource.exists()).thenReturn(false);
    Position position = new Position(0, 0);
    CompletionDocument completionDoc = new CompletionDocument("file://test.java", position);
    completionDoc.setVersion(1);
    CompletionJob job = new CompletionProvider(mockLsConnection, null).new CompletionJob(mockLsConnection);
    completionDoc.setInsertSpaces(true);
    completionDoc.setTabSize(4);
    job.setCompletionParams(new CompletionParams(completionDoc));
    job.setFile(mockResource);
    job.setEnableCompletion(true);
    job.schedule();

    IJobManager jobManager = Job.getJobManager();
    jobManager.join(CompletionProvider.COMPLETION_JOB_FAMILY, new NullProgressMonitor());

    assertEquals(Status.CANCEL_STATUS, job.getResult());
  }

}
