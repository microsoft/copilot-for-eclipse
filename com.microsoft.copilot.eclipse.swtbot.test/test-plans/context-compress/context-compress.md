# Auto Context Compression

## Overview
Verifies the **Auto Compress** feature that automatically compresses long
conversations to keep context usage within the model's limit. Auto Compress
is always enabled (no user-facing preference). While compression is in
progress, the chat view shows a "Compacting conversation..." spinner below
the latest Copilot turn, and the context size donut updates once it
completes.

Entry points:
- **Copilot Chat view** → latest Copilot turn (spinner banner appears here).
- **Copilot Chat view** → control bar **Context Size Donut** (updates after
  compression completes).

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed (built from
  the branch containing the staged Auto Compress changes).
- A valid GitHub Copilot subscription is active (authentication completed).
- A model that supports a finite context window is selected (so the donut and
  compression can be exercised — e.g. Claude Sonnet 4.6 or GPT-4.1).
- The Copilot Chat view is open and visible.

---

## Test Cases

### TC-001: Compacting banner appears when compression starts

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Copilot Chat view is open with a new conversation.

#### Steps
1. Start a conversation and drive the context usage toward the model limit —
   for example, attach several large files and/or run multiple tool-heavy
   turns until the **Context Size Donut** approaches its warning threshold
   (≥90 %).
2. Continue sending messages until the conversation goes over the threshold
   so the server initiates automatic compression.
3. Observe the latest Copilot turn while the server processes the request.

#### Expected Result
- A small banner appears **below the latest Copilot turn** containing:
  - An animated spinner.
  - The status text **"Compacting conversation..."**.
- The chat view layout refreshes so the banner is fully visible (not clipped).
- No error dialogs are shown.

#### 📸 Key Screenshots
- [ ] **Compacting banner** — spinner + "Compacting conversation..." text
  rendered under the latest Copilot turn.

---

### TC-002: Compacting banner is dismissed and context donut updates on completion

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-001 has been executed and the "Compacting conversation..." banner is
  currently visible.

#### Steps
1. Wait for the server to finish compression (typically a few seconds).
2. Observe the latest Copilot turn after compression completes.
3. Hover the **Context Size Donut** in the chat view control bar.

#### Expected Result
- The "Compacting conversation..." banner is removed from the Copilot turn.
- The chat view scroller relayouts cleanly (no leftover blank space, no
  clipping).
- The Context Size Donut updates to reflect the new, smaller token usage
  (the ring's filled portion shrinks).
- The **Context Window** popup shows the post-compression token breakdown
  consistent with the new total.
- The subsequent reply continues to stream normally on top of the freshly
  compressed history.

#### 📸 Key Screenshots
- [ ] **After completion** — Copilot turn without the banner.
- [ ] **Donut after compression** — Context Size Donut showing reduced usage.
- [ ] **Context Window popup** — Token breakdown after compression.

---

### TC-003: Cancelling a chat hides the compacting banner

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- A conversation is set up so the next send will trigger compression
  (as in TC-001).

#### Steps
1. Send the message that triggers compression and wait for the
   "Compacting conversation..." banner to appear.
2. While the banner is showing, click the **Cancel** (stop) button in the
   chat input action bar.

#### Expected Result
- The send button is restored from its stop/cancel state back to its normal
  send state.
- The "Compacting conversation..." banner is removed from the latest Copilot
  turn.
- Any buffered reply text that arrived just before cancellation is rendered
  (no missing trailing line).
- The chat view relayouts cleanly so the flushed reply is fully visible.
- The user can immediately send a new message in the same conversation.

#### 📸 Key Screenshots
- [ ] **After cancel** — banner gone, send button reset, any buffered reply
  visible.

---

### TC-004: Compacting banner only updates the matching conversation

**Type:** `Edge Case`
**Priority:** `P2`

#### Preconditions
- Two conversations exist in chat history: *Conversation A* (about to
  trigger compression) and *Conversation B* (short, well under the limit).

#### Steps
1. In *Conversation A*, send a message that triggers compression and wait
   for the "Compacting conversation..." banner to appear.
2. Without waiting for completion, open chat history and switch to
   *Conversation B*.
3. Inspect *Conversation B* for any compaction banner.
4. Switch back to *Conversation A*.

#### Expected Result
- *Conversation B* never shows a "Compacting conversation..." banner — the
  compaction status is scoped to *Conversation A* only.
- When you return to *Conversation A*, its state is consistent with the
  compression outcome (banner cleared if it completed in the meantime; new
  reply continues to stream if still in progress).
- No errors or stale spinners are left behind in either conversation.

#### 📸 Key Screenshots
- [ ] **Conversation B during A's compaction** — no banner shown.

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Compacting banner under latest Copilot turn.
- [ ] `TC-002` Copilot turn after compaction completes (banner gone).
- [ ] `TC-002` Context Size Donut after compaction (reduced usage).
- [ ] `TC-002` Context Window popup with post-compaction token breakdown.
- [ ] `TC-003` State after cancel — banner gone, send button reset, buffered
  reply visible.
- [ ] `TC-004` Conversation B during Conversation A's compaction (no banner).
