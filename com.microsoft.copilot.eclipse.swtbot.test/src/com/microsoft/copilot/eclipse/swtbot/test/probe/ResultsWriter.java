/*******************************************************************************
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 *******************************************************************************/
package com.microsoft.copilot.eclipse.swtbot.test.probe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the {@code results.json} report.
 *
 * <p>Shape is intentionally compatible with the JetBrains AgentProbe runner so agents
 * can reuse the same parsing heuristics.</p>
 */
public final class ResultsWriter {
  public String script;
  public int passed;
  public int failed;
  public long durationMs;
  public List<StepResult> steps = new ArrayList<>();

  public void write(Path path) throws IOException {
    Files.createDirectories(path.getParent());
    Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    try (Writer w = Files.newBufferedWriter(path)) {
      gson.toJson(this, w);
    }
  }
}
