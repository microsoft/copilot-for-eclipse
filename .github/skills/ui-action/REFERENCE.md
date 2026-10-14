# SWTBot Probe Reference

## Runner behavior

`ProbeRunner` is a JUnit 4 test launched by Tycho under a real Eclipse workbench
with `useUIHarness=true` and `useUIThread=false`. It loads the JSON array from
`-Dprobe.script`, executes each `ProbeStep` through `StepExecutor`, writes result
artifacts, and fails the Maven build if any failed step has `failFast` enabled.

Before the bot starts, the runner pre-populates configuration-scope preferences
so Quick Start, What's New, Welcome, and "Terminal Support Unavailable" dialogs
do not block normal probes.

`screenshot` and failure screenshots capture the active workbench shell with
`java.awt.Robot`, then fall back to SWTBot's full-display capture if needed.
Screenshots are diagnostic artifacts only; JSON assertions drive pass/fail.

## Running probes

Default command from the repository root:

    # macOS / Linux
    ./mvnw clean verify -Dprobe.script=probe-scripts/<name>.json
    # Windows (PowerShell)
    .\mvnw.cmd clean verify -Dprobe.script=probe-scripts/<name>.json

The narrower module command is useful only after a green root build:

```bash
./mvnw -pl com.microsoft.copilot.eclipse.swtbot.test -am -Dprobe.script=probe-scripts/<name>.json verify
```

The module-only shortcut can fail dependency resolution or reuse stale bundles,
especially after source edits. If widget markers or other code changes appear to
be ignored, run the root `clean verify` command.

Platform notes:

- Linux headless: prefix the command with `xvfb-run -a`.
- macOS: keep `${swtbot.platformArgLine}` in
  `com.microsoft.copilot.eclipse.swtbot.test/pom.xml` so the `swtbot-osx`
  profile can inject `-XstartOnFirstThread`.
- macOS screenshots: the JVM needs Screen Recording permission. Blank or
  wallpaper-only PNGs usually mean permission is missing.
- Do not set `-Djava.awt.headless=true`; `Robot` cannot capture screenshots in
  headless AWT mode.
- Windows HiDPI: workbench shell bounds are DIP coordinates while `Robot`
  expects raw pixels, so captures can clip at scaling above 100%.

Windows stale cache recovery:

```powershell
Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository\com\microsoft\copilot\eclipse
```

## Results

Artifacts are written under
`com.microsoft.copilot.eclipse.swtbot.test/target/probe-results/`:

```text
results.json                  # pass/fail summary
workspace.log                 # sandbox Eclipse .metadata/.log
screenshots/
  <id>.png                    # from screenshot steps
  FAILED-stepNN-<action>.png  # auto-captured on step failure
ui-dumps/<id>.xml             # from dumpUi steps
```

Quick pass/fail checks:

```bash
jq '{passed, failed, failed_steps: [.steps[] | select(.status=="failed") | {index, action, message}]}' \
  com.microsoft.copilot.eclipse.swtbot.test/target/probe-results/results.json
```

```powershell
Get-Content com.microsoft.copilot.eclipse.swtbot.test/target/probe-results/results.json |
  ConvertFrom-Json | Select-Object passed, failed, durationMs
```

Maven exit code `0` and `results.json` `.failed == 0` means pass. Otherwise,
open the failed step message, `FAILED-stepNN-*.png`, nearest `ui-dumps/*.xml`,
and `workspace.log`. Many "widget not found" failures are downstream of a
Copilot language server startup or authentication failure.

## Authentication

The Copilot JS agent reads its GitHub token from the host's standard Copilot
store (`%USERPROFILE%\AppData\Local\github-copilot\apps.json` on Windows;
`~/.config/github-copilot/apps.json` elsewhere). The Tycho JVM inherits it.

- Signed-in host required for probes that exercise chat completion.
- To probe unauthenticated state, assert the "Sign in to GitHub" UI instead of
  polling internal status managers.
- CI-side auth bootstrap is out of scope for this skill.

## Actions

Set `"failFast": false` on a step to record a failure without aborting the probe.

| `action` | Required | Optional | Notes |
|---|---|---|---|
| `screenshot` | - | `id` | PNG of the active workbench window written to `screenshots/`. |
| `sleep` | - | `timeoutSec` default `1` | Plain `Thread.sleep`. Prefer waits over sleeps. |
| `waitForIdle` | - | - | Flushes the SWT event queue. |
| `pressKey` | `key` | `locator` | Supports `ENTER`, `RETURN`, `CR`, `ESC`, `ESCAPE`, `TAB`, `SPACE`, `BS`, `BACKSPACE`, `DELETE`. Uses SWTBot `pressShortcut`; with a text/styled-text locator it targets that widget, otherwise the active shell. |
| `showView` | `idRef` | - | Opens an Eclipse view via `IWorkbenchPage#showView`. |
| `closeView` | `idRef` | - | Hides the view if present. |
| `invokeCommand` | `idRef` | - | Runs a registered Eclipse command via `IHandlerService#executeCommand`. |
| `click` | `locator` | - | Focuses text/styled-text widgets; otherwise reflectively invokes `click()` on the matched widget. |
| `typeIn` | `locator`, `text` | - | Sets text on text/styled-text widgets. |
| `clearElement` | `locator` | - | Clears text/styled-text widgets. |
| `waitFor` | `locator` | `timeoutSec` default `30` | Polls until the locator resolves. |
| `waitForMethod` | `locator`, `method` | `timeoutSec` default `30`, `expectedValue` | Polls a no-arg getter on the located widget until it returns non-null/non-empty, or equals `expectedValue`. |
| `assertExists` | `locator` | `shouldExist` default `true` | Asserts presence or absence. |
| `dumpUi` | - | `id` | Writes shell widget hierarchy XML with class names and SWTBot widget IDs. |
| `newSession` | - | - | Copilot-specific shortcut for the `newChatSession` command. |

## Locators

Locators are JSON objects with a `by` discriminator. SWTBot is not XPath-based;
extend `Locator` and `StepExecutor` when the existing vocabulary is not enough.

| `by` | Other fields | What it finds |
|---|---|---|
| `viewId` | `id` | Eclipse view by ID (`bot.viewById`). |
| `label` | `text` | First label with the given text. |
| `button` | `text` | First button with the given text. |
| `buttonWithTooltip` | `tooltip` | First button whose tooltip matches; useful for icon-only buttons like Send. |
| `text` | `index` default `0` | Nth text field. |
| `styledText` | - | First `StyledText`. |
| `tree` | `labels` array | Tree path under the active tree. |
| `cssId` | `value` | Widget whose `CssConstants.CSS_ID_KEY` data equals `value`. |
| `cssClass` | `value` | Widget whose `CssConstants.CSS_CLASS_NAME_KEY` contains `value` as a whitespace-separated token. |
| `widgetId` | `value` | Widget tagged with `setData("org.eclipse.swtbot.widget.key", value)`. Preferred for widgets you own. |
| `widgetClass` | `value` | First widget whose `getClass().getSimpleName()` equals `value`. Use only when you cannot tag the widget. |

Stable IDs and signals currently used by probes:

- `widgetId`: `user-turn`, `copilot-turn`, `model-picker`.
- `cssClass`: `model-info-label` appears only after a Copilot turn has completed.
- `cssId`: `chat-container`, `chat-content-wrapper`, `chat-content-viewer`,
  `chat-action-bar-wrapper`, `chat-action-bar`, `chat-history-viewer`.

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `IllegalArgumentException: Missing required field: ...` | Step JSON is missing a field required by that action. Check the action table. |
| `AssertionError: waitFor timed out: locator ...` | Widget did not appear. Add `dumpUi` before the failing step, confirm class/id, or raise `timeoutSec`. |
| `assertExists failed: ... shouldExist=true` | Locator did not match. Confirm the `by` type matches the widget family. |
| `click not supported on <Type>` | Wrapper has no `click()` method. Use a more specific locator, such as `button` instead of `label`. |
| Tests skipped: "No probe script specified" | Pass `-Dprobe.script=probe-scripts/<name>.json`. |
| `model-info-label` wait times out | Usually authentication or language-server startup. Inspect `workspace.log`. |
| All screenshots are blank or tiny identical PNGs | `Robot` capture failed. On macOS, grant Screen Recording permission to the JVM and re-run. |
| Widget-id markers missing from `dumpUi` | Stale bundle cache or stale module jar. Run root `./mvnw clean verify`. |

## Extending the vocabulary

Probe support lives in `com.microsoft.copilot.eclipse.swtbot.test/src/.../probe/`:

- `ProbeStep.java` and `Locator.java`: JSON shape.
- `StepExecutor.java`: action dispatch, locator resolution, screenshots, dumps.
- `ProbeRunner.java`: runner loop, reporting, and workbench setup.

Add UI-level primitives that mimic user behavior. Do not add actions that call
private production methods, reach into OSGi services, or mutate app state behind
the UI; those hide real UX bugs and break on internal refactors.
