# Auto Context Compression

## Overview
Verifies the new **Auto Compress** feature that automatically compresses long
conversations to keep context usage within the model's limit. When enabled
(default), the language server sends `$/copilot/compressionStarted` and
`$/copilot/compressionCompleted` notifications; the chat view shows a
"Compacting conversation..." spinner below the latest Copilot turn during
compression and updates the context size donut once it completes.

Entry points:
- **Preferences** → *GitHub Copilot* → *Chat* → **Auto Compress** toggle.
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

### TC-001: Auto Compress preference is visible and defaults to enabled

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- Fresh Eclipse workspace (no prior override of the `autoCompress` preference).

#### Steps
1. Open **Window → Preferences → GitHub Copilot → Chat**.
2. Scroll to the bottom of the Chat preferences page.
3. Locate the **Auto Compress** checkbox and its description.
4. Verify the description text reads:
   *"Automatically compress conversation context when it gets too long."*
5. Verify and make sure the checkbox is **checked**.
6. Click **Apply and Close** without changing anything.

#### Expected Result
- The **Auto Compress** field editor is rendered with a clear label and the
  description note below it.
- The checkbox is checked by default (matches `Constants.AUTO_COMPRESS = true`).
- No errors are logged when applying the unchanged preference.

#### 📸 Key Screenshots
- [ ] **Preferences page** — Chat preferences showing the Auto Compress
  checkbox and description.

---

### TC-002: Compacting banner appears when compression starts

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- **Auto Compress** is enabled in preferences.
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

### TC-003: Compacting banner is dismissed and context donut updates on completion

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-002 has been executed and the "Compacting conversation..." banner is
  currently visible.

#### Steps
1. Wait for the server to finish compression (typically a few seconds).
2. Observe the latest Copilot turn after the
   `$/copilot/compressionCompleted` notification is received.
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

### TC-004: Disabling Auto Compress prevents automatic compression

**Type:** `Negative`
**Priority:** `P1`

#### Preconditions
- The Copilot Chat view is open with a fresh conversation.

#### Steps
1. Open **Window → Preferences → GitHub Copilot → Chat**.
2. Uncheck **Auto Compress** and click **Apply and Close**.
3. Drive the conversation toward and past the context window threshold using
   the same approach as TC-002 (large attachments, tool-heavy turns).
4. Continue sending messages until the donut clearly enters the warning state
   (≥90 % utilization).

#### Expected Result
- No "Compacting conversation..." banner appears at any point.
- The Context Size Donut stays in/around the warning state (it does **not**
  spontaneously drop because no compression occurs).
- The setting change is propagated to the language server (the
  `autoCompress` field is sent as `false`); no `$/copilot/compressionStarted`
  notification is received.

#### 📸 Key Screenshots
- [ ] **Donut in warning state, no banner** — high utilization with no
  compaction UI.

---

### TC-005: Cancelling a chat hides the compacting banner

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- **Auto Compress** is enabled.
- A conversation is set up so the next send will trigger compression
  (as in TC-002).

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
- [ ] **During compaction** — banner visible.
- [ ] **After cancel** — banner gone, send button reset, any buffered reply
  visible.

---

### TC-006: Compacting banner only updates the matching conversation

**Type:** `Edge Case`
**Priority:** `P2`

#### Preconditions
- **Auto Compress** is enabled.
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
  notification's `conversationId` does not match, so the UI ignores it.
- When you return to *Conversation A*, its state is consistent with the
  compression outcome (banner cleared if it completed in the meantime; new
  reply continues to stream if still in progress).
- No exceptions or stale spinners are left behind in either conversation.

#### 📸 Key Screenshots
- [ ] **Conversation B during A's compaction** — no banner shown.

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Preferences page with Auto Compress checkbox + description.
- [ ] `TC-002` Compacting banner under latest Copilot turn.
- [ ] `TC-003` Copilot turn after compaction completes (banner gone).
- [ ] `TC-003` Context Size Donut after compaction (reduced usage).
- [ ] `TC-003` Context Window popup after compaction.
- [ ] `TC-004` Donut in warning state with Auto Compress disabled and no banner.
- [ ] `TC-005` Compacting banner before cancel.
- [ ] `TC-005` State after cancel — banner gone, send button reset, buffered
  reply visible.
- [ ] `TC-006` Conversation B during Conversation A's compaction (no banner).
