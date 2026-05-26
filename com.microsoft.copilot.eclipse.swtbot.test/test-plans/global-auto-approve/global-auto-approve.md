# Global Auto-Approve

## Overview

Tests the Global Auto-Approve (YOLO) feature: enabling/disabling it via the
preference page and verifying that all tool confirmations are bypassed when
active.

Entry points exercised:
- **Preferences → GitHub Copilot → Tool Auto Approve → Global Auto-Approve** —
  the "Automatically approve ALL tool invocations" checkbox with its
  confirmation dialog.
- **Agent Mode chat** — any tool call (terminal, file operation, MCP) to
  observe confirmation bypass.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- A signed-in Copilot account on the host machine.
- Network access to `api.githubcopilot.com`.
- **Agent Mode** selected in the chat mode dropdown.
- Global Auto-Approve is **disabled** at the start of each scenario.

---

## 1. Enable Global Auto-Approve — confirmation dialog required

### TC-001: Enable Global Auto-Approve → confirmation dialog required
→ all tools skip confirmation

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Open **Preferences → Tool Auto Approve → Global Auto-Approve** section.
2. Click the **"Automatically approve ALL tool invocations"** checkbox.
3. Observe that a **confirmation dialog immediately appears** asking the user
   to confirm this dangerous setting.
4. Verify the dialog title and message warn about the risk.
5. Click **Cancel** — verify the checkbox remains **unchecked**.
6. Click the checkbox again, then click **OK** in the confirmation dialog.
7. Verify the checkbox is now **checked**.
8. Click **"Apply and Close"**.
9. In Agent Mode, send a prompt that would normally trigger a confirmation
   (e.g., an MCP tool call or a terminal command).
10. Observe that **no confirmation dialog appears** — all tools auto-approve.

#### Expected Result
- Enabling YOLO mode requires an explicit confirmation dialog.
- Cancelling the dialog keeps the checkbox unchecked.
- When enabled, all tool confirmations (terminal, file operations, MCP)
  are bypassed.

#### 📸 Key Screenshots
- [ ] Confirmation dialog when enabling YOLO mode.
- [ ] Checkbox unchecked after Cancel.
- [ ] Checkbox checked after OK.
- [ ] Agent Mode: tool runs without any confirmation dialog.

---

## 2. Disable Global Auto-Approve — no confirmation needed

### TC-002: Disable Global Auto-Approve → no dialog → tools require
confirmation again

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- Global Auto-Approve is **enabled**.

#### Steps
1. Open **Preferences → Tool Auto Approve → Global Auto-Approve** section.
2. Click the **"Automatically approve ALL tool invocations"** checkbox to
   uncheck it.
3. Observe that **no confirmation dialog appears** — turning it off is safe
   and does not require confirmation.
4. Verify the checkbox is now **unchecked**.
5. Click **"Apply and Close"**.
6. In Agent Mode, trigger any tool.
7. Observe that the **confirmation dialog appears** — YOLO mode is off.

#### Expected Result
- Disabling YOLO mode does not require a confirmation dialog.
- Tools require confirmation again after disabling.

#### 📸 Key Screenshots
- [ ] Checkbox unchecked without any dialog.
- [ ] Tool shows confirmation dialog again.

---

## 3. Global Auto-Approve overrides all tool categories

### TC-003: Global Auto-Approve bypasses terminal deny rules, MCP
rules, and file operation rules

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- Global Auto-Approve is **enabled**.
- Terminal has a custom **Deny** rule for `curl`.

#### Steps
1. In Agent Mode, trigger a `curl` terminal command (normally blocked by the
   deny rule).
2. Observe **auto-approved** — YOLO mode bypasses the deny rule.
3. Trigger an MCP tool call with no prior MCP approval.
4. Observe **auto-approved** — YOLO mode bypasses MCP confirmation.
5. Trigger a file operation on a file not in the attached context.
6. Observe **auto-approved** — YOLO mode bypasses file operation confirmation.

#### Expected Result
- Global Auto-Approve bypasses ALL tool categories regardless of individual
  rules or approval lists.

#### 📸 Key Screenshots
- [ ] `curl` auto-approved despite deny rule.
- [ ] MCP tool auto-approved without prior approval.
- [ ] File operation auto-approved.
