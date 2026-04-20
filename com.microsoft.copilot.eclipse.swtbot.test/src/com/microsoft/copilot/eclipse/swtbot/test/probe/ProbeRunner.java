// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.swtbot.test.probe;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.copilot.eclipse.core.Constants;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Executes a JSON probe script against the running Eclipse workbench.
 *
 * <p>The script path is read from the {@code probe.script} system property (relative to
 * the bundle root, or absolute). If unset the test is skipped so ordinary {@code mvn
 * verify} runs are unaffected.</p>
 *
 * <p>Output is written to {@code target/probe-results/}:</p>
 * <ul>
 *   <li>{@code results.json} — structured pass/fail report per step.</li>
 *   <li>{@code screenshots/} — PNG captures; failing steps auto-produce
 *       {@code FAILED-step&lt;N&gt;-&lt;action&gt;.png}.</li>
 *   <li>{@code ui-dumps/} — XML widget-tree dumps from {@code dumpUi} steps.</li>
 * </ul>
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class ProbeRunner {
  private static final String SYSPROP = "probe.script";
  private static final Path RESULTS_DIR = Paths.get("target", "probe-results");
  private static final String UI_BUNDLE_ID = "com.microsoft.copilot.eclipse.ui";

  private SWTWorkbenchBot bot;

  @BeforeClass
  public static void assumeScript() {
    String v = System.getProperty(SYSPROP);
    Assume.assumeTrue(
        "No probe script specified via -D" + SYSPROP + "=<path>; skipping probe runner.",
        v != null && !v.isEmpty() && !"null".equals(v) && !("${" + SYSPROP + "}").equals(v));
    suppressNuisancePreferences();
  }

  /**
   * Pre-populates the Copilot UI plugin's configuration-scope preferences so the
   * workbench-startup logic in {@code CopilotUi#showHintIfNecessary} and
   * {@code MissingTerminalDependenciesDialog#showIfNotSuppressed} skip their
   * "first run" dialogs (Quick Start, What's New, Terminal Support Unavailable).
   * Runs before the workbench bot exists; best-effort — any failure is logged
   * and the probe continues.
   */
  private static void suppressNuisancePreferences() {
    try {
      IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(UI_BUNDLE_ID);
      prefs.putInt(Constants.COPILOT_QUICK_START_VERSION, Constants.CURRENT_COPILOT_QUICK_START_VERSION);
      prefs.putBoolean(Constants.AUTO_SHOW_WHAT_IS_NEW, false);
      prefs.put(Constants.LAST_USED_COPILOT_PLUGIN_VERSION, currentUiBundleMajorMinor());
      prefs.putBoolean(Constants.SUPPRESS_TERMINAL_DEPENDENCY_DIALOG, true);
      prefs.flush();
    } catch (BackingStoreException | RuntimeException e) {
      System.err.println("[ProbeRunner] Failed to preset nuisance-dialog preferences: " + e);
    }
  }

  private static String currentUiBundleMajorMinor() {
    Bundle b = Platform.getBundle(UI_BUNDLE_ID);
    if (b == null) {
      return "0.0";
    }
    Version v = b.getVersion();
    return v.getMajor() + "." + v.getMinor();
  }

  @Before
  public void setUp() {
    // SWTBot defaults to JPEG; pin to PNG so the .png filenames we produce are truthful.
    SWTBotPreferences.SCREENSHOT_FORMAT = "png";
    bot = new SWTWorkbenchBot();
    try {
      bot.viewByTitle("Welcome").close();
    } catch (Exception ignored) {
      // welcome view absent — fine
    }
  }

  @Test
  public void runProbe() throws Exception {
    Path scriptPath = resolveScriptPath();
    List<ProbeStep> steps = loadScript(scriptPath);

    Path screenshotsDir = RESULTS_DIR.resolve("screenshots");
    Path uiDumpsDir = RESULTS_DIR.resolve("ui-dumps");
    StepExecutor executor = new StepExecutor(bot, screenshotsDir, uiDumpsDir);

    ResultsWriter report = new ResultsWriter();
    report.script = scriptPath.toString();

    long t0 = System.currentTimeMillis();
    boolean hardFailure = false;

    for (int i = 0; i < steps.size(); i++) {
      ProbeStep step = steps.get(i);
      StepResult result = new StepResult();
      result.index = i;
      result.action = step.action;
      result.id = step.id;
      long stepStart = System.currentTimeMillis();
      try {
        executor.execute(step, result);
        result.status = "passed";
        report.passed++;
      } catch (Throwable t) {
        result.status = "failed";
        result.message = t.getClass().getSimpleName() + ": " + t.getMessage();
        executor.screenshotFailure(i, step.action, step.id);
        report.failed++;
        if (step.isFailFast()) {
          hardFailure = true;
        }
      } finally {
        result.durationMs = System.currentTimeMillis() - stepStart;
        report.steps.add(result);
      }
      if (hardFailure) {
        break;
      }
    }
    report.durationMs = System.currentTimeMillis() - t0;
    report.write(RESULTS_DIR.resolve("results.json"));
    copyWorkspaceLog();

    if (report.failed > 0) {
      throw new AssertionError(
          "Probe failed: " + report.failed + " of " + steps.size()
              + " steps failed. See " + RESULTS_DIR.resolve("results.json"));
    }
  }

  /**
   * Copies the sandbox workbench's {@code .metadata/.log} next to the results so the
   * Eclipse platform log is easy to inspect alongside screenshots and step messages.
   * Absent or unreadable logs are ignored — the probe should never fail because of
   * diagnostic artefact plumbing.
   */
  private void copyWorkspaceLog() {
    try {
      Path src = Platform.getLogFileLocation().toFile().toPath();
      if (!Files.isRegularFile(src)) {
        return;
      }
      Path dst = RESULTS_DIR.resolve("workspace.log");
      Files.createDirectories(RESULTS_DIR);
      Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception ignored) {
      // best effort
    }
  }

  private Path resolveScriptPath() {
    String raw = System.getProperty(SYSPROP);
    Path p = Paths.get(raw);
    if (!p.isAbsolute()) {
      p = Paths.get("").toAbsolutePath().resolve(raw);
    }
    return p;
  }

  private List<ProbeStep> loadScript(Path path) throws Exception {
    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("Probe script not found: " + path);
    }
    try (Reader r = Files.newBufferedReader(path)) {
      Gson gson = new Gson();
      Type listType = new TypeToken<List<ProbeStep>>() { }.getType();
      List<ProbeStep> steps = gson.fromJson(r, listType);
      return steps == null ? new ArrayList<>() : steps;
    }
  }
}
