# Subagent: Execution, Persistence, and Session Switching

## Overview
Tests that subagent execution (`run_subagent` tool call) is correctly
displayed during live execution, persisted to conversation history, and
properly restored when switching between conversations via chat history.

The observable signals include the `SubagentMessageBlock` card (border +
agent name header), individual tool-call status labels inside the card,
and the main agent's summary text appearing after the subagent block.
A failure in persistence, progress-event routing, or restoration logic
breaks these signals.

Entry points exercised:
- Agent mode with a prompt that triggers `run_subagent`.
- Chat history panel: switching to a different conversation and back.
- Cancel (stop) button during subagent execution.
- Cancel button on a subagent tool-confirmation dialog.
- `conversation/destroy` LSP call on session switch.

Not exercised:
- Subagent tool-call confirmation dialogs.
- Subagent error recovery / retry.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- **A signed-in Copilot account.**
- Network access to `api.githubcopilot.com`.
- Agent mode selected in the chat mode picker.
- At least one custom agent available (e.g., the built-in CVE Remediator
  or a workspace `.github/agents/*.md` agent).

---

## 1. Live subagent execution

### TC-001: Subagent executes and renders nested card

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Chat view is open in Agent mode.
- No previous conversation is active (fresh session).

#### Steps
1. Type a prompt that triggers subagent execution (e.g., `invoke a subagent`
   or `scan for CVEs`).
2. Click **Send**.
3. Wait for the main agent to invoke `run_subagent` — a bordered
   `SubagentMessageBlock` card should appear with the agent name header
   (e.g., "CVE Remediator: Scan dependencies for CVEs").
4. Wait for the subagent to finish executing — tool-call status labels
   (checkmark icons) should appear inside the card.
5. Wait for the main agent to produce its summary response after the
   subagent block.
6. The send button returns to the send state.

#### Expected Result
- A `SubagentMessageBlock` card renders with agent name and description.
- Subagent tool calls (e.g., "Searched for files matching query",
  "Ran MCP tool") appear inside the card with status icons.
- The main agent's summary text appears below the subagent block.
- No orphaned tool-call labels outside the card.

#### Key Screenshots
- [ ] **Subagent running** — card header visible, tool calls streaming in.
- [ ] **Subagent completed** — all tool calls show checkmark, main agent
  summary visible below.

---

## 2. Persistence and restoration

### TC-002: Subagent content restored after session switch (completed)

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-001 completed successfully (subagent fully finished).

#### Steps
1. Open chat history (clock icon in top banner).
2. Select a different conversation or click "New Chat".
3. Open chat history again.
4. Select the conversation from TC-001.

#### Expected Result
- The subagent card header is visible, positioned between the
  `run_subagent` tool-call status and the main agent's summary.
- Subagent tool calls are displayed inside the card (not as flat
  labels under the main turn).
- The main agent's summary text appears after the subagent section.

#### Key Screenshots
- [ ] **Restored conversation** — subagent card with tool calls visible
  inside, main agent summary below.

---

### TC-003: Subagent content restored after cancel mid-execution

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- A new conversation in Agent mode.

#### Steps
1. Send a prompt that triggers subagent execution.
2. While the subagent is running (tool calls streaming), click **Cancel**
   (stop button).
3. Open chat history, switch to another conversation, then switch back.

#### Expected Result
- The subagent card header is visible with partial content.
- Tool calls executed before cancel are displayed inside the card.
- The send button is in the send state.

#### Key Screenshots
- [ ] **After cancel** — partial subagent card visible, send button reset.
- [ ] **After restore** — same partial content restored correctly.

---

## 3. Progress event isolation

### TC-004: Subagent progress does not leak when switching mid-execution

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- A new conversation in Agent mode.

#### Steps
1. Send a prompt that triggers subagent execution.
2. While the subagent is still running, open chat history and switch to
   a different conversation.
3. Observe the newly opened conversation.

#### Expected Result
- The newly opened conversation does not show any subagent output
  (tool calls, card headers, or text) from the previous conversation.
- The send button is in the send state (not stuck on cancel).
- No error messages or orphaned widgets appear.
- `workspace.log` should show `conversation/destroy` being sent for
  the previous conversation.

#### Key Screenshots
- [ ] **Switched conversation** — clean UI, no leaked subagent output.

#### Notes on failure modes
- Subagent output appearing in the wrong conversation →
  `isProgressForCurrentConversation()` check failed; verify
  `subagentConversationId` is being set on first subagent event and
  cleared on cancel/switch.
- Send button stuck on cancel → `onCancel()` did not call
  `actionBar.resetSendButton()`.

---

## 4. Multiple subagents in one turn

### TC-005: Two subagents in a single turn restore correctly

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- A new conversation in Agent mode.

#### Steps
1. Send a prompt that triggers multiple subagent invocations
   (e.g., `invoke two different subagents` or a prompt where the agent
   decides to call `run_subagent` twice).
2. Wait for both subagents to complete.
3. Switch to another conversation and back.

#### Expected Result
- Each subagent has its own card header with the correct agent name.
- Tool calls for each subagent appear under their respective cards.
- After restoration, both subagent cards are correctly positioned and
  filled with their respective tool calls.
- The main agent's summary (if any) appears after the last subagent block.

#### Key Screenshots
- [ ] **Live execution** — two distinct subagent cards visible.
- [ ] **After restore** — both cards restored with correct content.

#### Notes on failure modes
- Content from subagent A appearing in subagent B's card →
  `subagentToolCallId` association is incorrect; check that
  `lastRunSubagentToolCallId` is updated for each `run_subagent`
  tool call.

---

## 5. Non-subagent conversation unaffected

### TC-006: Conversation with no subagent restores cleanly

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- A new conversation in Agent mode.

#### Steps
1. Send a message that produces a normal (non-subagent) agent response.
2. Wait for the response to complete.
3. Switch to another conversation via chat history, then switch back.

#### Expected Result
- The restored conversation looks identical to before the session switch.
- No duplicate or extra assistant messages appear.
- No orphaned subagent card headers or tool-call labels are shown.

#### Key Screenshots
- [ ] **After restore** — conversation unchanged, no extra messages.

---

## 6. Tool-confirmation dialog cancel layout

### TC-007: Subagent panel collapses after confirmation dialog is cancelled

**Type:** `Regression`
**Priority:** `P1`

#### Preconditions
- The Chat view is open in Agent mode.
- A new conversation (fresh session).
- The selected agent triggers a tool that requires confirmation
  (e.g., a command that needs to run in the terminal).

#### Steps
1. Send a prompt that triggers subagent execution where the subagent
   wants to run a command (e.g., `scan for CVEs`, or
   `run ls in the terminal`).
2. Wait for the subagent's tool **confirmation dialog** to appear inside
   the subagent card (Allow / Skip buttons).
3. Click **Skip** on the confirmation dialog.
4. Observe the subagent card area after the dialog disposes.

#### Expected Result
- The subagent panel resizes/collapses to fit its remaining content.
- **No empty/blank gap** is left at the bottom of the subagent area where
  the confirmation dialog used to be (the bug in #169).
- A skip label/message is shown where appropriate, and the scroller
  minimum height is recomputed in a single layout pass.
- The send button returns to the send state.

#### Key Screenshots
- [ ] **Confirmation dialog visible** — Allow / Skip buttons shown
  inside the subagent card.
- [ ] **After skip** — subagent panel collapsed with no empty area below.

#### Notes on failure modes
- Empty area left below the subagent card → `cancelConfirmation()` did not
  trigger `requestRefreshScrollerLayout()`; the `ScrolledComposite`
  `setMinHeight()` was never recomputed after the dialog disposal.
- Verify the Allow path is unaffected: clicking **Allow** on a
  separate invocation should still lay out correctly (no regression).
