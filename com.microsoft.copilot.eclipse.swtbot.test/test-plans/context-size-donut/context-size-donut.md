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

## 2. Context Window Popup

### TC-002: Hovering the donut opens the Context Window popup

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

## 3. Edge Cases

### TC-003: Donut resets when a new conversation is started

**Type:** `Edge Case`
**Priority:** `P2`

#### Steps
1. Start a conversation and let the donut show some utilization.
2. Start a new conversation (clear or new session).
3. Observe the donut state before any new response.

#### Expected Result
- The donut resets (hidden or zero state) for the new conversation until a
  response with `ContextSizeInfo` is received.
