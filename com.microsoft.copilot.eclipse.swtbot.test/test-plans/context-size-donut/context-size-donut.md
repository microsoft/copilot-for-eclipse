# Context Size Donut and Popup

## Overview
Tests the context size donut chart widget and its hover popup in the GitHub Copilot
for Eclipse chat view. A `ContextSizeDonut` widget is rendered in the chat view's
`ActionBar` control bar. It displays a ring that fills proportionally based on the
token utilization percentage received via the LSP `ContextSizeInfo` payload. At
≥90 % utilization the ring switches to a warning color. Hovering over the donut
opens a `ContextWindowPopup` that shows a breakdown of token usage by category.

Entry points:
- **Copilot Chat view** → bottom control bar (the donut appears after the first
  response is received that includes `ContextSizeInfo`).
- Hover the donut to open the **Context Window** popup.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
- A valid GitHub Copilot subscription is active (authentication completed).
- The Copilot Chat view is open and visible in the workbench.

---

## 1. Donut Widget Visibility

### TC-001: Donut appears after first response

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The Copilot Chat view is open.
- No previous conversation is loaded (fresh session or new conversation).

#### Steps
1. Open the Copilot Chat view.
2. Confirm the donut widget is **not** visible in the bottom control bar (no
   `ContextSizeInfo` has been received yet).
3. Send a short chat message (e.g. "Hello") and wait for a response.
4. Inspect the bottom control bar of the chat view.

#### Expected Result
- Before the first response the donut is not visible (or shows an empty/zero state).
- After the first response arrives the donut widget appears in the control bar.
- No error dialog or exception is logged.

#### 📸 Key Screenshots
- [ ] **Before response** — chat view control bar with no donut.
- [ ] **After response** — chat view control bar showing the donut widget.

---

## 2. Donut Fill Proportion

### TC-002: Donut ring reflects token utilization

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- A conversation exists with at least one response (donut is visible).

#### Steps
1. Send a short message; note the approximate fill level of the donut ring.
2. Send a message that attaches a large file or pastes a long code block to
   increase context size; wait for the response.
3. Compare the donut fill level after the larger-context response.

#### Expected Result
- The donut ring fill increases visibly when more tokens are consumed.
- The fill level is proportional to the reported utilization percentage (0–100 %).

#### 📸 Key Screenshots
- [ ] **Low utilization** — donut with small fill.
- [ ] **Higher utilization** — donut with larger fill.

---

## 3. Context Window Popup

### TC-003: Hovering the donut opens the Context Window popup

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The donut widget is visible (at least one response received).

#### Steps
1. Hover the mouse cursor over the donut widget in the control bar.
2. Wait for the popup to appear.
3. Inspect the popup header.
4. Inspect the popup body for: total token count, utilization percentage, progress
   bar, and per-category rows.
5. Move the mouse away from the donut.

#### Expected Result
- The popup opens with the header **"Context Window"**.
- Total usage is displayed in the format `X / Y tokens`.
- A utilization percentage is shown.
- A progress bar reflects the utilization level.
- Per-category rows are shown: **System Instructions**, **Tool Definitions**,
  **Messages**, **Attached Files**, **Tool Results**.
- Moving the mouse away closes the popup.

#### 📸 Key Screenshots
- [ ] **Popup open** — the Context Window popup showing all fields.

---

### TC-004: Popup shows non-zero Attached Files when a file is attached

**Type:** `Happy Path`
**Priority:** `P2`

#### Preconditions
- The chat view is open.

#### Steps
1. Send a message with at least one file attached as context.
2. Wait for the response.
3. Hover over the donut to open the popup.
4. Locate the **Attached Files** row.

#### Expected Result
- The **Attached Files** row displays a non-zero token count.

---

### TC-005: Popup shows non-zero Tool Definitions and Tool Results in agent mode

**Type:** `Happy Path`
**Priority:** `P2`

#### Preconditions
- Agent mode is enabled in the chat view.

#### Steps
1. Send a message that triggers at least one tool call in agent mode.
2. Wait for the response.
3. Hover over the donut to open the popup.
4. Locate the **Tool Definitions** and **Tool Results** rows.

#### Expected Result
- **Tool Definitions** shows a non-zero token count.
- **Tool Results** shows a non-zero token count.

---

## 4. Warning Color at High Utilization

### TC-006: Donut ring changes to warning color at ≥90 % utilization

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- A way to produce or simulate ≥90 % token utilization (e.g. very large context,
  or by injecting a mock `ContextSizeInfo` value via a debug breakpoint).

#### Steps
1. Bring the token utilization to ≥90 % (send large context or inject mock data).
2. Observe the donut ring color.

#### Expected Result
- Below 90 % the ring is rendered in the normal (non-warning) color.
- At or above 90 % the ring switches to a warning/red color.

#### 📸 Key Screenshots
- [ ] **Warning state** — donut showing warning color at high utilization.

---

## 5. Edge Cases

### TC-007: Donut resets when a new conversation is started

**Type:** `Edge Case`
**Priority:** `P2`

#### Steps
1. Start a conversation and let the donut show some utilization.
2. Start a new conversation (clear or new session).
3. Observe the donut state before any new response.

#### Expected Result
- The donut resets (hidden or zero state) for the new conversation until a
  response with `ContextSizeInfo` is received.

---

### TC-008: Chat view resized to narrow width

**Type:** `Edge Case`
**Priority:** `P2`

#### Steps
1. Make the chat view narrow by dragging its edge.
2. Send a message and wait for the donut to appear.
3. Hover over the donut to open the popup.

#### Expected Result
- The donut and control bar do not overflow or clip incorrectly.
- The popup appears positioned near the donut without being cut off.

---

### TC-009: Rapid message sending does not cause stale data

**Type:** `Edge Case`
**Priority:** `P2`

#### Steps
1. Send several messages in quick succession.
2. After all responses arrive, hover over the donut.

#### Expected Result
- The donut and popup reflect the most recent `ContextSizeInfo` without
  stale data, race conditions, or visual glitches.
