# Rate Limit Warning Banner

## Overview
Tests the rate limit warning banner in the GitHub Copilot for Eclipse chat view.
When the Copilot language server emits a `$/copilot/rateLimitWarning` LSP
notification, a `StaticBanner` widget is displayed above the action bar input
area. The banner shows the server-provided human-readable message, a
"Get more info" hyperlink to `https://aka.ms/github-copilot-rate-limit-error`,
and a "Dismiss" button. The banner is wired via an OSGi event topic
(`TOPIC_RATE_LIMIT_WARNING`) from `CopilotLanguageClient` → `ChatView` →
`ActionBar`. Navigating chat history hides/shows the banner appropriately.

Entry points:
- Triggered automatically by a `$/copilot/rateLimitWarning` LSP notification.
- Dismissed manually via the "×" button on the banner.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
- A GitHub account signed in with a Copilot subscription that has measurable
  usage quota (so that rate limit notifications can be triggered or simulated).
- A way to trigger or mock a `$/copilot/rateLimitWarning` LSP notification —
  options include:
  - Exhausting the quota for the account.
  - Injecting the notification via a debug breakpoint in `CopilotLanguageClient`.
  - Using a test harness / mock language server.
- The Copilot Chat view is open and visible in the workbench.

---

## 1. Banner Appearance

### TC-001: Banner appears in non-handoff mode on rate limit warning

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The Copilot Chat view is open in a **non-handoff** (standard) chat mode.

#### Steps
1. Trigger a `$/copilot/rateLimitWarning` notification (type: "weekly" or
   "session") from the language server while in standard chat mode.
2. Observe the area above the chat input field in the Action Bar.

#### Expected Result
- A warning banner appears above the action bar input area.
- The banner text matches the `message` field from the LSP notification.
- No error dialog or exception is logged.

#### 📸 Key Screenshots
- [ ] **Banner visible** — chat view showing the rate limit warning banner.

---

### TC-002: Banner appears in handoff mode on rate limit warning

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The Copilot Chat view is open in **agent/handoff** mode.

#### Steps
1. Trigger a `$/copilot/rateLimitWarning` notification while in agent/handoff
   mode.
2. Observe the area above the chat input field in the Action Bar.

#### Expected Result
- The warning banner appears correctly above the action bar input area, identical
  to non-handoff mode.

---

## 2. Banner Content

### TC-003: Banner contains "Get more info" link and Dismiss button

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The rate limit warning banner is currently visible in the chat view.

#### Steps
1. Locate the **"Get more info"** link in the banner.
2. Click the link.
3. Locate the **"×"** (Dismiss) button in the banner.

#### Expected Result
- The "Get more info" link opens `https://aka.ms/github-copilot-rate-limit-error`
  in the system default browser (or Eclipse's internal browser).
- The "×" button is visible and interactive.

#### 📸 Key Screenshots
- [ ] **Banner with link and dismiss button** — close-up of the banner widget.

---

### TC-004: Dismiss button closes the banner

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The rate limit warning banner is currently visible.

#### Steps
1. Click the **"×"** (Dismiss) button on the banner.
2. Observe the chat view layout.

#### Expected Result
- The banner is removed from the chat view immediately.
- The chat input area expands to fill the space previously occupied by the banner.
- No exceptions or layout glitches occur.

#### 📸 Key Screenshots
- [ ] **After dismiss** — chat view with banner removed.

---

## 3. History Navigation

### TC-005: Banner hides and shows when navigating chat history

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- A rate limit warning banner is visible for the current chat conversation.
- At least one other chat history entry exists.

#### Steps
1. Note the current conversation — the banner is visible.
2. Navigate to a **different** chat history entry.
3. Observe the banner state.
4. Navigate **back** to the original conversation.
5. Observe the banner state.

#### Expected Result
- After switching away, the banner is **hidden** for the other history entry.
- After switching back, the banner **reappears** for the original conversation.
- No errors or layout issues occur during navigation.

---

## 4. Edge Cases

### TC-006: Multiple rapid notifications — only one banner shown

**Type:** `Edge Case`
**Priority:** `P2`

#### Steps
1. Trigger two or more `$/copilot/rateLimitWarning` notifications in quick
   succession.
2. Observe the chat view.

#### Expected Result
- Only one banner is displayed at a time (previous banner is replaced/disposed
  before the new one is shown).

---

### TC-007: New warning after dismiss creates a fresh banner

**Type:** `Edge Case`
**Priority:** `P2`

#### Steps
1. Dismiss the existing rate limit banner.
2. Trigger another `$/copilot/rateLimitWarning` notification.
3. Observe the chat view.

#### Expected Result
- A new banner appears with the content of the new notification.
- No stale state from the dismissed banner is visible.

---

### TC-008: Rate limit warning received while chat view is not focused

**Type:** `Edge Case`
**Priority:** `P2`

#### Steps
1. Switch focus to another Eclipse view or editor.
2. Trigger a `$/copilot/rateLimitWarning` notification.
3. Switch back to the Copilot Chat view.

#### Expected Result
- The banner is displayed when the chat view regains focus/visibility.

---

### TC-009: Very long message string wraps gracefully

**Type:** `Edge Case`
**Priority:** `P3`

#### Steps
1. Trigger a `$/copilot/rateLimitWarning` notification whose `message` field
   contains an unusually long string (e.g. 500+ characters).
2. Observe the banner layout.

#### Expected Result
- The banner text wraps within the banner boundaries.
- The chat view layout is not broken (no overflow, clipping, or overlapping
  widgets).
