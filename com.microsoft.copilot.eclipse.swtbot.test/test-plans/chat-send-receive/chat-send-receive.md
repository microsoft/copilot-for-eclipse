# Chat: Send a Prompt and Receive a Response

## Overview
Tests the core "send a chat prompt and render the completed response" flow in
the GitHub Copilot Chat view. This is the single most load-bearing interaction
for the Copilot for Eclipse plugin: the view must open, its input must accept
text, the chat model must be resolved, the **Send** button must dispatch the
prompt as a user turn, and the language server must stream back a Copilot turn
that completes end-to-end.

The observable signals (user-turn rendered, `model-info-label` appearing on a
completed Copilot turn) are UI-level outputs of the full stack: workbench view
lifecycle → chat UI → model picker → LSP request/response → streaming
renderer. A failure anywhere in that pipeline breaks the test.

Entry points exercised:
- **Window → Show View → Copilot Chat** (equivalent to the probe's
  `showView` of view id `com.microsoft.copilot.eclipse.ui.chat.ChatView`).

Not exercised in TC-001 (separate scenarios):
- The chat-mode dropdown (Ask / Edit / Agent).
- The model picker's explicit model selection — the probe trusts whatever
  the picker resolves to as its default.
- Attachment / context management.
- Cancelling an in-flight response.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- **A signed-in Copilot account on the host machine.** The Copilot JS agent
  reads its GitHub token from the OS-standard Copilot store
  (`~/.config/github-copilot/apps.json` on macOS/Linux,
  `%USERPROFILE%\AppData\Local\github-copilot\apps.json` on Windows). Sign in
  via any Copilot client (Eclipse plugin, VS Code, or `gh auth login`) before
  running the probe. Without auth the language server cannot complete a turn
  and the `model-info-label` wait will time out.
- Network access to `api.githubcopilot.com` (and the GitHub auth host if a
  token refresh is needed).
- No modal dialogs queued on workbench startup. The probe runner
  pre-populates preferences to suppress Quick Start, What's New, Welcome, and
  "Terminal Support Unavailable" pop-ups; authoring additional tests on top
  of this scenario should keep that contract.

---

## 1. Happy-path send and receive

### TC-001: Send "hello" and verify a Copilot turn completes

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Eclipse workbench is open.
- The user is signed in to Copilot (see Prerequisites).
- No previous chat conversation is open in the Chat view (a fresh probe
  sandbox satisfies this automatically; if running manually, start a new
  session).

#### Steps
1. Wait for the workbench to settle (`waitForIdle`).
2. Open the **Copilot Chat** view (`Window → Show View → Other… → Copilot →
   Copilot Chat`, or use the probe's `showView` of
   `com.microsoft.copilot.eclipse.ui.chat.ChatView`).
3. Verify the Chat view is visible and its input area has rendered (the
   `StyledText` chat input is present and editable).
4. Click into the chat input to focus it.
5. Clear any existing text and type the prompt `hello`.
6. Wait for the model picker to resolve an active model — i.e. the picker
   tagged `widget.setData("org.eclipse.swtbot.widget.key", "model-picker")`
   reports a non-null `getSelectedItemId()`. This is the signal that auth
   + the asynchronous `listModels` response from the language server have
   both completed. Sending before this point produces an `activeModel is
   null` NPE in the onSend path — that's a separate regression to guard
   against, not the happy path tested here.
7. Locate and click the **Send** button (icon-only button with tooltip
   `Send`).
8. Wait for a user turn to render in the chat viewer (a widget tagged
   `user-turn`, i.e. a `UserTurnWidget` instance). This proves the Send
   button dispatched the prompt into the conversation state and the view
   re-rendered.
9. Wait up to ~120 s for a Copilot turn to complete. The reliable signal is
   a widget with CSS class `model-info-label`, which is only attached to a
   Copilot turn's footer **after** streaming finishes. (A `copilot-turn`
   widget appears earlier, while streaming is in progress; asserting on it
   alone doesn't prove the turn completed successfully.)
10. Verify a `copilot-turn` widget exists and at least one
    `model-info-label` is present in the view hierarchy.

#### Expected Result
- The Chat view opens and accepts keyboard input.
- The Send button dispatches the prompt; a user turn renders with the exact
  text submitted.
- Within ~120 seconds, a Copilot turn appears and completes streaming — its
  footer renders a `model-info-label` showing which model served the
  response.
- No error dialog is shown. `workspace.log` contains no
  `!ENTRY com.microsoft.copilot.eclipse` entries at `ERROR` level and no
  NPEs originating in `CopilotLanguageServer.*` / the Chat UI.

#### 📸 Key Screenshots
- [ ] **Chat view open** — empty conversation, input focused.
- [ ] **Prompt typed** — input shows `hello`, Send button visible.
- [ ] **User turn rendered** — the user turn appears in the transcript
  immediately after Send.
- [ ] **Agent response completed** — the Copilot turn is present and its
  `model-info-label` footer is visible (streaming is done).

#### Notes on failure modes
- `model-picker.getSelectedItemId()` never becomes non-null → auth or LS
  startup failure. Check `workspace.log` for LS launch errors and
  `~/.config/github-copilot/apps.json` for a valid token.
- User turn renders but `model-info-label` never appears → LS connected but
  the turn didn't complete (rate limit, network, server-side error, or the
  streaming renderer getting stuck). Inspect `workspace.log` for the JSON-RPC
  stream and LSP4E warnings.
- Widget-id locators (`model-picker`, `user-turn`, `copilot-turn`) don't
  resolve → stale Tycho cache for the UI bundle; re-run root `clean verify`
  to rebuild `setData` markers (see `.github/skills/ui-action/SKILL.md`).
