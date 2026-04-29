---
name: ui-action
description: Write or update a SWTBot JSON probe script (e.g. from a test plan), then validate it locally by running the `ProbeRunner` Tycho test against the Copilot for Eclipse plugin. Use this whenever you need end-to-end UI validation against a real Eclipse workbench.
---

# Authoring and Validating SWTBot Probe Scripts (Eclipse)

Use this skill to write or update a JSON probe script and validate it by
invoking the `com.microsoft.copilot.eclipse.swtbot.test` Tycho bundle locally.
Probe scripts are JSON, so no Java is compiled per test case.

## Quick start

1. Drop a probe at `com.microsoft.copilot.eclipse.swtbot.test/probe-scripts/<plan-slug>-<tc-id>.json`
   (e.g. `chat-send-receive-001.json`). Start every probe with this preamble that
   settles the workbench and opens the Copilot Chat view:

   ```json
   [
     { "action": "waitForIdle" },
     { "action": "screenshot", "id": "01-startup" },
     { "action": "showView", "idRef": "com.microsoft.copilot.eclipse.ui.chat.ChatView" },
     { "action": "waitForIdle" },
     { "action": "screenshot", "id": "02-chat-open" }
   ]
   ```

2. Run it (cross-platform command; use `./mvnw` on macOS/Linux):

   ```powershell
   ./mvnw clean verify -Dprobe.script=probe-scripts/<name>.json
   ```

   Root `clean verify` is the recommended default. Tycho prefers each
   bundle's freshly-packaged jar in `<module>/target/` and silently falls
   back to the stale Maven cache if the jar is missing — making `setData`
   markers and other source edits appear to be ignored.

   The narrower `./mvnw -pl com.microsoft.copilot.eclipse.swtbot.test -am
   -Dprobe.script=... verify` shortcut is **experimental**: in practice
   it can fail dependency resolution (observed: `Missing requirement:
   com.microsoft.copilot.eclipse.core 0.0.0`, with Tycho choosing the
   wrong `osgi.arch` on aarch64). Use only as a follow-up, after a green
   root `clean verify`.

3. Read results under `com.microsoft.copilot.eclipse.swtbot.test/target/probe-results/`:

   ```
   results.json                  # pass/fail summary
   workspace.log                 # sandbox Eclipse .metadata/.log
   screenshots/
     <id>.png                    # from `screenshot` steps
     FAILED-stepNN-<action>.png  # auto-captured on step failure
   ui-dumps/<id>.xml             # from `dumpUi` steps
   ```

If `-Dprobe.script` is unset the test is skipped, so ordinary `./mvnw verify`
runs are unaffected.

> **Linux headless:** prefix with `xvfb-run -a`. **macOS:** the swtbot test
> pom carries a `swtbot-osx` profile that injects `-XstartOnFirstThread`
> into the test JVM via the `swtbot.platformArgLine` placeholder
> interpolated into `<argLine>`. If you ever edit
> `com.microsoft.copilot.eclipse.swtbot.test/pom.xml`, **keep the
> `${swtbot.platformArgLine}` token in `<argLine>`** — replacing it with a
> plain literal silently drops the macOS arg and the workbench fails with
> `SWTException: Invalid thread access`. **Stale cache recovery:**
> `Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository\com\microsoft\copilot\eclipse`
> then re-run `clean verify`.

## How the runner works

`ProbeRunner` is a JUnit 4 test Tycho launches under a real Eclipse workbench
(`useUIHarness=true`, `useUIThread=false`). It:

1. Loads the JSON probe pointed at by `-Dprobe.script`.
2. Walks each step via `SWTWorkbenchBot`, wrapping every step in a uniform
   try/catch so one failure doesn't crash the run.
3. Writes `results.json`, screenshots (PNG — `SWTBotPreferences.SCREENSHOT_FORMAT`
   is pinned), and widget-tree dumps.
4. Fails the Maven build if any `failFast` step failed.

Before the bot starts, the runner pre-populates configuration-scope
preferences so Quick Start, What's New, Welcome, and "Terminal Support
Unavailable" dialogs don't pop during a probe — probes see a clean workbench.

`screenshot` and `dumpUi` capture from the **on-screen workbench shell**:
`StepExecutor#captureWorkbenchShell` resolves the active workbench
`Shell`'s bounds, then snapshots those screen pixels with
`java.awt.Robot.createScreenCapture` (with SWTBot's full-display
`bot.captureScreenshot` as a final fallback). SWT's own GC-based capture
(`new GC(display).copyArea(...)` and `Shell#print(GC)`) returns blank
images on macOS Cocoa for workbench shells — Robot is the workaround.
Caveats:

- **macOS:** the JVM running the tests needs **Screen Recording**
  permission (System Settings → Privacy & Security → Screen Recording).
  Without it Robot returns blank or wallpaper-only frames.
- **Headless AWT:** `-Djava.awt.headless=true` makes `new Robot()` throw,
  and the SWTBot fallback is also blank on Cocoa. Don't set it.
- **Windows HiDPI:** `Shell#getBounds()` returns DIP coordinates; Robot
  expects raw screen pixels. At display scaling > 100% the captured
  region is undersized / clipped. (At 100% — typical CI runners — it's
  a pixel-perfect match.)

## Authoring rules

### Drive the UI, don't reach into services

Probes **must** interact with the workbench the way a user does — SWTBot
clicks, typing, menu navigation, keyboard shortcuts, or `invokeCommand`
against a registered Eclipse command id (the same `IHandlerService#executeCommand`
path that menus and key bindings trigger — useful when an entry point is
buried in a popup like the Copilot status-bar menu, which is awkward to
drive with SWTBot). Do not add actions that reflectively invoke private
methods, call OSGi services to flip state, or read private fields; those
hide real UX bugs and break on every internal refactor. If a scenario needs
a new primitive (e.g. a chat-mode dropdown picker), extend the
action/locator vocabulary in `StepExecutor` with a UI-level primitive,
not a service shortcut.

### Tag widgets for `widgetId` locators

SWTBot's blessed identification convention is
`widget.setData("org.eclipse.swtbot.widget.key", "<stable-id>")` at widget
construction, located from tests with the `widgetId` locator. Zero runtime
cost, far more robust than class-name or creation-order lookup. Prefer
`widgetId` over `widgetClass` whenever you control the widget's construction;
use `widgetClass` only for platform / third-party widgets you can't tag.

Current tags: `user-turn` (`UserTurnWidget`), `copilot-turn`
(`CopilotTurnWidget`), `model-picker` (chat-model `DropdownButton`).

### Screenshots are artifacts; assertions catch regressions

`screenshot` never fails a step. Validation has two layers:

- **Structural** (machine-checked): `assertExists`, `waitFor`, specific
  locators. These failures land in `results.json` and drive the build exit
  code — the agent judges pass/fail from here.
- **Visual** (agent-checked): after the run, open the PNGs under
  `screenshots/` with your vision tool and describe what you see. No
  baseline-image comparison in JSON (cross-platform font rendering makes
  pixel diffs flaky).

Pair screenshots with assertions around every non-trivial interaction:

```json
{ "action": "screenshot", "id": "before-send" },
{ "action": "click", "locator": { "by": "buttonWithTooltip", "tooltip": "Send" } },
{ "action": "waitForIdle" },
{ "action": "assertExists", "locator": { "by": "widgetId", "value": "user-turn" } },
{ "action": "screenshot", "id": "after-send" }
```

When a locator is wrong, insert a `dumpUi` step and inspect
`ui-dumps/*.xml` to find the real widget class / id.

### Authentication prerequisite

The Copilot JS agent reads its GitHub token from the host's standard Copilot
store (`%USERPROFILE%\AppData\Local\github-copilot\apps.json` on Windows;
`~/.config/github-copilot/apps.json` elsewhere) — the Tycho JVM inherits it.

- **Signed-in host required** for any probe that exercises chat. Sign in via
  any Copilot client (Eclipse plugin, VS Code, `gh auth login`).
- **Probing unauthed state:** assert on the "Sign in to GitHub" button that
  replaces the chat input; don't poll internal status managers.
- CI-side auth bootstrap is out of scope for this skill.

## Reading results & interpreting failures

Maven exit code `0` **and** `results.json` `.failed == 0` → overall pass.
Otherwise open the failed step's `message`, its `FAILED-stepNN-*.png`, the
nearest `ui-dumps/*.xml`, **and** `workspace.log` — many "widget not found"
failures are downstream of a Copilot LS that never started or authenticated.
Look for `!ENTRY com.microsoft.copilot.eclipse` entries and NPEs on
`CopilotLanguageServer.*`.

| Symptom | What's wrong / fix |
|---|---|
| `IllegalArgumentException: Missing required field: …` | Step JSON missing a field required by that action (see action table). |
| `AssertionError: waitFor timed out: locator …` | Widget not appearing. Add a `dumpUi` before the failing step, check class/id, or raise `timeoutSec`. |
| `assertExists failed: … shouldExist=true` | Locator didn't match. Confirm `by` matches the widget family. |
| `click not supported on <Type>` | Wrapper has no `click()`; use the right `by` (e.g. `button`, not `label`). |
| Tests skipped: "No probe script specified" | Pass `-Dprobe.script=probe-scripts/<name>.json`. |
| `model-info-label` wait times out | Usually auth; open `workspace.log`. |
| All screenshots are blank / identical ~4KB PNGs across every step | Robot capture failed. macOS: grant Screen Recording permission to the JVM and re-run. Verify by file size — real workbench frames are 50KB+; a uniform 4KB family means the on-screen capture path didn't run or returned empty pixels. |
| Widget-id markers missing from `dumpUi` | Stale Maven cache — run full `clean verify` (see Quick start). |

Quick pass/fail check:

```powershell
Get-Content com.microsoft.copilot.eclipse.swtbot.test/target/probe-results/results.json |
  ConvertFrom-Json | Select-Object passed, failed, durationMs
```

```bash
jq '{passed, failed, failed_steps: [.steps[] | select(.status=="failed") | {index, action, message}]}' \
  com.microsoft.copilot.eclipse.swtbot.test/target/probe-results/results.json
```

## Action reference

Set `"failFast": false` on a step to record a failure without aborting the probe.

| `action` | Required | Optional | Notes |
|---|---|---|---|
| `screenshot` | — | `id` | PNG of the active workbench window written to `screenshots/`. |
| `sleep` | — | `timeoutSec` (default 1) | Plain `Thread.sleep`. |
| `waitForIdle` | — | — | Flushes the SWT event queue. |
| `pressKey` | `key` | `locator` | Accepts `ENTER`/`CR`, `ESC`/`ESCAPE`, `TAB`, `SPACE`, `BS`/`BACKSPACE`, `DELETE`. Without `locator`, presses on the active shell; with a `locator` targeting a text / styledText widget, sends the key directly (needed for chat input ENTER-to-send). |
| `showView` | `idRef` (Eclipse view id) | — | Opens via `IWorkbenchPage#showView`. |
| `closeView` | `idRef` | — | Hides the view if present. |
| `invokeCommand` | `idRef` (command id) | — | Runs via `IHandlerService#executeCommand`. |
| `click` | `locator` | — | Reflective `click()` on matched widget. |
| `typeIn` | `locator`, `text` | — | Sets text programmatically via `setText`. Does **not** fire key events — use `typeKeys` when key listeners (e.g. content-assist) must trigger. |
| `typeKeys` | `locator`, `text` | — | Types text character-by-character via keyboard simulation. Fires real key events, triggering listeners like content-assist auto-activation. Use for `/` in the chat input. |
| `clearElement` | `locator` | — | Sets text to empty. |
| `waitFor` | `locator` | `timeoutSec` (default 30) | Polls until locator resolves. |
| `waitForMethod` | `locator`, `method` | `timeoutSec` (default 30), `expectedValue` | Polls until a no-arg getter on the located widget returns non-null (and non-empty for `String`), or — if `expectedValue` is set — until `toString()` equals it. Invoked reflectively via the class hierarchy. Use for UI-exposed state not reachable via finders, e.g. `DropdownButton.getSelectedItemId()`. |
| `assertExists` | `locator` | `shouldExist` (default true) | Asserts presence / absence. |
| `assertContentAssist` | `text` | `shouldExist` (default true) | Asserts that the visible content-assist popup contains (or does not contain) a proposal whose text includes `text`. Type `/` and `sleep` before calling to let the popup render. |
| `dumpUi` | — | `id` | Writes widget hierarchy XML. |
| `newSession` | — | — | Copilot-specific: triggers `newChatSession` command. |
| `createProject` | — | `text` (default `"probe-project"`) | Creates and opens an Eclipse project in the workspace. No-op if it already exists. The Tycho test workbench starts empty — call this before any `createWorkspaceFile` / `deleteWorkspaceFile` step. |
| `createWorkspaceFile` | `path` | `content` | Creates a file (and parent folders) in the first open workspace project. |
| `deleteWorkspaceFile` | `path` | — | Deletes a file from the first open workspace project. |
| `openPreferencePage` | `pageId` | — | Opens the Preferences dialog directly to the given page id. |
| `waitForJobFamily` | `jobFamily` | `timeoutSec` (default 30) | Waits for all Eclipse Jobs in the given family to finish. |

## Locator reference

Locators are JSON objects with a `by` discriminator. SWTBot is **not
XPath-based** — stick to this vocabulary; extend `Locator` / `StepExecutor`
if you need a new finder.

| `by` | Other fields | What it finds |
|---|---|---|
| `viewId` | `id` | Eclipse view by id (`bot.viewById`). |
| `label` | `text` | First label with the given text. |
| `button` | `text` | First button with the given text. |
| `buttonWithTooltip` | `tooltip` | First button whose tooltip matches (use for icon-only buttons like **Send**). |
| `text` | `index` (default 0) | Nth text field. |
| `styledText` | — | First StyledText (editors / chat input). |
| `tree` | `labels` (array) | Tree path under the active tree. |
| `cssId` | `value` | Widget whose `CssConstants.CSS_ID_KEY` equals `value` (e.g. `chat-content-viewer`, `chat-action-bar`). Walks the full shell tree. |
| `cssClass` | `value` | Widget whose `CssConstants.CSS_CLASS_NAME_KEY` contains `value` as a token. `model-info-label` is only set on a **completed** Copilot turn, so waiting on it is a reliable "response received" signal. |
| `widgetId` | `value` | Widget tagged with `setData("org.eclipse.swtbot.widget.key", value)`. **Preferred** identifier for widgets you own. |
| `widgetClass` | `value` | First widget whose `getClass().getSimpleName()` equals `value` (walks the full shell tree). Fallback for widgets you can't tag. |

## Canonical example: send a prompt and verify a response

```json
{ "action": "clearElement", "locator": { "by": "styledText" } },
{ "action": "typeIn",       "locator": { "by": "styledText" }, "text": "your prompt" },
{ "action": "screenshot",   "id": "typed" },

{ "action": "waitForMethod",
  "locator": { "by": "widgetId", "value": "model-picker" },
  "method": "getSelectedItemId",
  "timeoutSec": 60 },

{ "action": "click",        "locator": { "by": "buttonWithTooltip", "tooltip": "Send" } },

{ "action": "waitFor",      "locator": { "by": "widgetId", "value": "user-turn" },
  "timeoutSec": 30 },
{ "action": "assertExists", "locator": { "by": "widgetId", "value": "user-turn" } },

{ "action": "waitFor",      "locator": { "by": "cssClass", "value": "model-info-label" },
  "timeoutSec": 120 },
{ "action": "screenshot",   "id": "agent-response" },
{ "action": "assertExists", "locator": { "by": "widgetId", "value": "copilot-turn" } }
```

Key signals:

- `model-picker.getSelectedItemId()` non-null — the workbench has resolved an
  active chat model. Sending before this NPEs in `onSendInternal` with
  "activeModel is null" because auth + model fetch complete asynchronously.
- `user-turn` — the user turn has rendered (Send button dispatched the prompt).
- `model-info-label` — only appears once a Copilot turn has **completed**
  (rendered in the turn's footer at end of streaming); the reliable "response
  fully received" signal.
- `copilot-turn` — the assistant turn container; good secondary assertion.

Without a signed-in host the language server can't complete a turn, so
`model-info-label` never appears and the step times out — itself a useful
diagnostic (check `workspace.log`).

## Extending the vocabulary

Everything the probe understands lives in
`com.microsoft.copilot.eclipse.swtbot.test/src/.../probe/`:

- `ProbeStep.java` / `Locator.java` — JSON shape.
- `StepExecutor.java` — action dispatch + locator resolution.
- `ProbeRunner.java` — runner loop, reporting, screenshots.

Add a case to the `switch` in `StepExecutor#execute` (or `resolve`) and the new
action is usable from every probe.

### Keep one probe focused

Each JSON script represents one test case. Split unrelated behaviours into
separate probes so a single `FAILED-step…` screenshot tells you what broke.
