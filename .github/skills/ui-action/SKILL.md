---
name: ui-action
description: Authors and validates SWTBot JSON probe scripts for GitHub Copilot for Eclipse UI flows against a real Eclipse workbench. Use when creating or updating probe-scripts, converting test plans to UI probes, validating end-to-end Eclipse UI behavior, or troubleshooting ProbeRunner failures.
---

# SWTBot Probe Scripts

Use this skill to create or update JSON probes for `com.microsoft.copilot.eclipse.swtbot.test`.
Probes drive the workbench through SWTBot actions and run as Tycho tests; no Java is compiled per test case.

## Quick start

1. Create one focused JSON script at
   `com.microsoft.copilot.eclipse.swtbot.test/probe-scripts/<plan-slug>-<tc-id>.json`.
2. Start by settling the workbench and opening the view under test:

   ```json
   [
     { "action": "waitForIdle" },
     { "action": "screenshot", "id": "01-startup" },
     { "action": "showView", "idRef": "com.microsoft.copilot.eclipse.ui.chat.ChatView" },
     { "action": "waitForIdle" },
     { "action": "screenshot", "id": "02-chat-open" }
   ]
   ```

3. Add user-level steps only, such as `click`, `typeIn`, `clearElement`, `pressKey`,
   `invokeCommand`, `waitFor`, `waitForMethod`, `assertExists`, `screenshot`, or
   `dumpUi`. Prefer `widgetId` locators for widgets the plugin owns.
4. Run from the repository root:

   ```bash
   ./mvnw clean verify -Dprobe.script=probe-scripts/<name>.json
   ```

   Use root `clean verify` as the default. Tycho can reuse stale bundle jars
   when only the SWTBot module is run, making source edits appear ignored. Use
   `xvfb-run -a` on Linux headless.
5. Read `com.microsoft.copilot.eclipse.swtbot.test/target/probe-results/results.json`,
   `workspace.log`, `screenshots/`, and `ui-dumps/`.

If `-Dprobe.script` is unset, `ProbeRunner` skips itself so ordinary `./mvnw verify` runs are unaffected.

## Authoring workflow

- Convert one scenario/test case into one probe. Split unrelated behaviors so each failure screenshot has one meaning.
- Drive the UI the way a user does. `invokeCommand` is allowed for registered Eclipse command IDs; do not call
  OSGi services, private methods, or test-only shortcuts.
- Pair screenshots with machine-checked assertions. Screenshots never fail a step; `assertExists`, `waitFor`, and
  `waitForMethod` decide pass/fail.
- Wait for asynchronous UI state before acting. For chat, wait for the `model-picker` selected model before Send,
  then wait for `user-turn` and `model-info-label`.
- When a locator fails, insert `dumpUi` before the failure and inspect `ui-dumps/*.xml` for widget class names and
  SWTBot widget IDs.
- For unauthenticated chat probes, assert the "Sign in to GitHub" UI. Do not poll internal auth managers.

## Chat send skeleton

```json
[
  { "action": "waitForIdle" },
  { "action": "showView", "idRef": "com.microsoft.copilot.eclipse.ui.chat.ChatView" },
  { "action": "waitFor", "locator": { "by": "styledText" }, "timeoutSec": 30 },
  { "action": "clearElement", "locator": { "by": "styledText" } },
  { "action": "typeIn", "locator": { "by": "styledText" }, "text": "hello" },
  { "action": "waitForMethod", "locator": { "by": "widgetId", "value": "model-picker" },
    "method": "getSelectedItemId", "timeoutSec": 60 },
  { "action": "click", "locator": { "by": "buttonWithTooltip", "tooltip": "Send" } },
  { "action": "waitFor", "locator": { "by": "widgetId", "value": "user-turn" }, "timeoutSec": 30 },
  { "action": "waitFor", "locator": { "by": "cssClass", "value": "model-info-label" }, "timeoutSec": 120 },
  { "action": "assertExists", "locator": { "by": "widgetId", "value": "copilot-turn" } },
  { "action": "screenshot", "id": "agent-response" }
]
```

## Pass/fail judgment

A probe passes only when Maven exits `0` and `results.json` has `"failed": 0`.
On failure, inspect the failed step message, `FAILED-stepNN-*.png`, nearest `ui-dumps/*.xml`, and `workspace.log`.

## Extending probes

The vocabulary lives in `com.microsoft.copilot.eclipse.swtbot.test/src/.../probe/`.
Add UI-level primitives in `ProbeStep`, `Locator`, and `StepExecutor`, then update [REFERENCE.md](REFERENCE.md).

See [REFERENCE.md](REFERENCE.md) for action/locator tables, platform notes, result queries, troubleshooting, and current stable widget IDs.
