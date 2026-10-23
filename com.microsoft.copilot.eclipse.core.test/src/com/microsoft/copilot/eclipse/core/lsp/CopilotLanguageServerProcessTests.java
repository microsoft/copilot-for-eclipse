// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4e.LanguageServerWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CopilotLanguageServerProcessTests {

  private static final String SLEEPER_SOURCE = """
      public class Sleeper {
        public static void main(String[] args) throws Exception {
          if (args.length > 0) {
            new ProcessBuilder(args).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
          }
          Thread.sleep(120_000);
        }
      }
      """;

  @TempDir
  Path tempDir;

  private Process server;
  private ProcessHandle serverChild;

  @AfterEach
  void killLeftovers() {
    if (serverChild != null) {
      serverChild.destroyForcibly();
    }
    if (server != null) {
      server.destroyForcibly();
    }
  }

  @Test
  void testStop_serverIgnoresShutdown_terminatesServerProcessTree() throws Exception {
    Path sleeper = tempDir.resolve("Sleeper.java");
    Files.writeString(sleeper, SLEEPER_SOURCE);
    String java = ProcessHandle.current().info().command().orElseThrow();
    // The "server" spawns a child, like the CLS spawns MCP servers.
    server = new ProcessBuilder(List.of(java, sleeper.toString(), java, sleeper.toString()))
        .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
    serverChild = awaitChild(server);

    // A mocked wrapper does nothing on stop(), like LSP4E when the JVM exits before its async close runs.
    LanguageServerWrapper wrapper = mock(LanguageServerWrapper.class);
    when(wrapper.getProcessHandle()).thenReturn(server.toHandle());

    new CopilotLanguageServerConnection(wrapper).stop();

    server.onExit().get(10, TimeUnit.SECONDS);
    assertFalse(server.isAlive(), "language server must be terminated");
    serverChild.onExit().get(10, TimeUnit.SECONDS);
    assertFalse(serverChild.isAlive(), "language server child must be terminated");
  }

  private static ProcessHandle awaitChild(Process process) throws IOException, InterruptedException {
    long deadline = System.currentTimeMillis() + 60_000;
    while (System.currentTimeMillis() < deadline) {
      Optional<ProcessHandle> child = process.children().findFirst();
      if (child.isPresent()) {
        return child.get();
      }
      assertTrue(process.isAlive(), "test server process died unexpectedly");
      Thread.sleep(100);
    }
    throw new IOException("Test server process did not start a child process");
  }
}
