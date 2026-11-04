// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.lsp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
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

  // Launched as single-file source in a separate JVM: the OSGi test runtime offers no plain classpath to
  // start a compiled test class with.
  private static final String SLEEPER_SOURCE = """
      import java.nio.file.Files;
      import java.nio.file.Path;
      import java.util.Arrays;

      public class Sleeper {
        public static void main(String[] args) throws Exception {
          int first = 0;
          if (args.length > 0 && args[0].startsWith("--await=")) {
            // Spawn the child only once the trigger file exists
            Path trigger = Path.of(args[0].substring("--await=".length()));
            while (!Files.exists(trigger)) {
              Thread.sleep(50);
            }
            first = 1;
          }
          if (args.length > first) {
            new ProcessBuilder(Arrays.copyOfRange(args, first, args.length))
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
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

  @Test
  void testStop_childSpawnedWhileStopping_terminatesChild() throws Exception {
    Path sleeper = tempDir.resolve("Sleeper.java");
    Files.writeString(sleeper, SLEEPER_SOURCE);
    Path trigger = tempDir.resolve("spawn-child");
    String java = ProcessHandle.current().info().command().orElseThrow();
    server = new ProcessBuilder(List.of(java, sleeper.toString(), "--await=" + trigger, java, sleeper.toString()))
        .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
    LanguageServerWrapper wrapper = mock(LanguageServerWrapper.class);
    when(wrapper.getProcessHandle()).thenReturn(server.toHandle());
    // stop() captures the process tree first, so the child spawned here is new to it
    doAnswer(invocation -> {
      Files.createFile(trigger);
      serverChild = awaitChild(server);
      return null;
    }).when(wrapper).stop();

    new CopilotLanguageServerConnection(wrapper).stop();

    serverChild.onExit().get(10, TimeUnit.SECONDS);
    assertFalse(serverChild.isAlive(), "child spawned while stopping must be terminated");
  }

  private static ProcessHandle awaitChild(Process process) throws IOException, InterruptedException {
    long deadline = System.currentTimeMillis() + 60_000;
    while (System.currentTimeMillis() < deadline) {
      Optional<ProcessHandle> child = process.children().filter(CopilotLanguageServerProcessTests::isJava)
          .findFirst();
      if (child.isPresent()) {
        return child.get();
      }
      assertTrue(process.isAlive(), "test server process died unexpectedly");
      Thread.sleep(100);
    }
    throw new IOException("Test server process did not start a child process");
  }

  // Skips helper processes such as conhost.exe on Windows
  private static boolean isJava(ProcessHandle process) {
    return process.info().command().map(command -> Path.of(command).getFileName().toString().startsWith("java"))
        .orElse(false);
  }
}
