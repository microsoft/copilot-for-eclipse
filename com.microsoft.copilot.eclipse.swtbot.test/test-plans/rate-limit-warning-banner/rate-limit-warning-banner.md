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

## 2. Banner Content

### TC-002: Banner contains "Get more info" link and Dismiss button

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

### TC-003: Dismiss button closes the banner

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
