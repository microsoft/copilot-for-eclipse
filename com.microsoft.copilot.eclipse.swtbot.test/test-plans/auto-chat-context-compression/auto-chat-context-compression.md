# Support Automatic Chat Context Compression

## Overview
Verifies the **Auto Compress** feature that automatically compresses long conversations to keep context usage within the model's limit. Auto Compress is always enabled (no user-facing toggle). While compression is in progress the chat view shows a "Compacting conversation..." spinner below the latest Copilot turn, and the Context Size Donut updates once compression completes.

---

## Test Cases

### TC-001: Compacting banner appears when compression starts

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and authenticated.
- A model with a finite context window selected (e.g. Claude Sonnet 4.6 or GPT-4.1).
- The Copilot Chat view is open with a new conversation.

#### Steps
1. Start a conversation and drive context usage toward the model limit — attach several large files and/or run multiple tool-heavy turns until the **Context Size Donut** approaches its warning threshold (≥90%).
2. Continue sending messages until the conversation exceeds the threshold and the server initiates automatic compression.
3. Observe the latest Copilot turn while the server processes the request.

#### Expected Result
- A banner appears **below the latest Copilot turn** containing an animated spinner and the text **"Compacting conversation..."**.
- The chat view layout refreshes so the banner is fully visible (not clipped).
- No error dialogs are shown.

#### 📸 Key Screenshots
- [ ] **Compacting banner** — spinner + "Compacting conversation..." text rendered below the latest Copilot turn.

---

### TC-002: Compacting banner is dismissed and Context Size Donut updates on completion

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-001 has been executed and the "Compacting conversation..." banner is currently visible.

#### Steps
1. Wait for the server to finish compression (typically a few seconds).
2. Observe the latest Copilot turn after compression completes.
3. Hover the **Context Size Donut** in the chat view control bar to open the Context Window popup.

#### Expected Result
- The "Compacting conversation..." banner is removed from the Copilot turn.
- The chat view relayouts cleanly (no leftover blank space, no clipping).
- The Context Size Donut updates to reflect the new, smaller token usage (the ring's filled portion shrinks).
- The **Context Window** popup shows the post-compression token breakdown consistent with the new total.
- The subsequent reply continues to stream normally on top of the freshly compressed history.

#### 📸 Key Screenshots
- [ ] **After completion** — Copilot turn without the banner.
- [ ] **Donut after compression** — Context Size Donut showing reduced usage.
- [ ] **Context Window popup** — Token breakdown after compression.

---

### TC-003: Cancelling a chat turn hides the compacting banner

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- A conversation is set up so the next send will trigger compression (as in TC-001).

#### Steps
1. Send the message that triggers compression and wait for the "Compacting conversation..." banner to appear.
2. While the banner is showing, click the **Cancel** (stop) button in the chat input action bar.

#### Expected Result
- The send button is restored from its stop/cancel state back to its normal send state.
- The "Compacting conversation..." banner is removed from the latest Copilot turn.
- Any buffered reply text that arrived just before cancellation is rendered (no missing trailing line).
- The chat view relayouts cleanly so the flushed reply is fully visible.
- The user can immediately send a new message in the same conversation.

#### 📸 Key Screenshots
- [ ] **After cancel** — banner gone, send button reset, any buffered reply visible.

---

### TC-004: Compacting banner is scoped to the active conversation only

**Type:** `Edge Case`
**Priority:** `P2`

#### Preconditions
- Two conversations exist in chat history: *Conversation A* (about to trigger compression) and *Conversation B* (short, well under the context limit).

#### Steps
1. In *Conversation A*, send a message that triggers compression and wait for the "Compacting conversation..." banner to appear.
2. Without waiting for completion, open chat history and switch to *Conversation B*.
3. Inspect *Conversation B* for any compaction banner.
4. Switch back to *Conversation A*.

#### Expected Result
- *Conversation B* never shows a "Compacting conversation..." banner — compaction status is scoped to *Conversation A* only.
- When returning to *Conversation A*, its state is consistent with the compression outcome (banner cleared if it completed; reply still streaming if still in progress).
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
- [ ] `TC-003` State after cancel — banner gone, send button reset, buffered reply visible.
- [ ] `TC-004` Conversation B during Conversation A's compaction (no banner).
