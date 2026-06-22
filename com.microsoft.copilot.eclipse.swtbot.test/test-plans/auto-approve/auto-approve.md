# Tool Auto Approve

## Overview
Verifies the end-to-end "Tool Auto Approve" feature introduced in PR #255: the
unified **Preferences → GitHub Copilot → Tool Auto Approve** page (Terminal,
File Operations, MCP, and Global sections), the org-policy / feature-flag
gating that can hide it, and the shared confirmation-dialog flow that routes
Agent Mode tool calls through `ConfirmationService` and the per-category
handlers. Deep per-category cases live in the sibling plans
(`terminal-auto-approve`, `file-operation-auto-approve`, `mcp-auto-approve`,
`global-auto-approve`); this plan covers the integration surface that ties them
together.

---

## Test Cases

### TC-001: Tool Auto Approve preference page renders all four sections

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Eclipse with the GitHub Copilot plugin installed and activated.
- Signed-in Copilot account whose org policy permits auto-approval (the
  feature flag `isAutoApprovalEnabled()` resolves to `true`).

#### Steps
1. Open **Window → Preferences**.
2. Expand the **GitHub Copilot** category and select **Tool Auto Approve**.
3. Verify the page shows four labelled group sections, top to bottom:
   - **Terminal Auto Approve** — with a rule table (Command / Auto Approve
     columns), **Add...**, **Remove**, **Reset to Defaults** buttons, and the
     "Auto approve commands not covered by rules" checkbox.
   - **File Operations Auto Approve** — with a rule table (Pattern /
     Description columns), the same Add/Remove/Reset buttons, and the
     "Auto approve file operations not covered by rules" checkbox.
   - **MCP Auto Approve** — with the "Trust MCP tool annotations" checkbox
     and the "MCP Server and Tool Approval" area.
   - **Global Auto Approve** — with the "Auto approve all tool calls"
     checkbox.
4. Scroll the page to confirm all sections are reachable.

#### Expected Result
- All four sections render in order with their tables, buttons, and
  checkboxes.
- The page is vertically scrollable and stays within the Preferences shell
  bounds.

#### 📸 Key Screenshots
- [ ] **Page top** — Terminal + File Operations sections visible.
- [ ] **Page bottom** — MCP + Global sections visible after scrolling.

---

### TC-002: Org policy gating hides the rules and shows an info message

**Type:** `Negative`
**Priority:** `P0`

#### Preconditions
- Signed-in Copilot account whose org policy **disables** auto-approval
  (feature flag `isAutoApprovalEnabled()` resolves to `false`).

#### Steps
1. Open **Window → Preferences → GitHub Copilot → Tool Auto Approve**.
2. Observe the page contents.

#### Expected Result
- No rule tables, checkboxes, or buttons are shown.
- A single info message is displayed: *"Tool auto-approval rules are disabled
  by your organization's administrator. Please contact your organization's
  administrator for more information."* next to an info icon.

#### 📸 Key Screenshots
- [ ] **Disabled page** — info message shown instead of the rule sections.

---

### TC-003: Terminal default deny rule triggers the confirmation dialog

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Terminal default rules are active (not modified).
- "Auto approve commands not covered by rules" is **unchecked**.
- **Agent Mode** selected in the chat mode dropdown.

#### Steps
1. Open the **Copilot Chat** view and select **Agent** mode.
2. Wait for the model picker to resolve.
3. Send the prompt: `please run curl https://example.com`.
4. Wait for the agent to invoke the `run_in_terminal` tool with a `curl`
   command.
5. Observe the in-chat confirmation dialog.

#### Expected Result
- The confirmation dialog appears (curl is a default deny rule).
- It shows the **"Run command in terminal"** title, the command text in a
  scrollable panel, a blue **"Allow Once ▾"** split-dropdown button, and a
  **"Skip"** button.
- Clicking **Skip** dismisses the tool call without running the command.

#### 📸 Key Screenshots
- [ ] **Confirmation dialog** — split-dropdown and Skip button visible.

---

### TC-004: Allow rule auto-approves a matching tool call

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- **Agent Mode** selected.

#### Steps
1. Open **Tool Auto Approve** preferences.
2. In **Terminal Auto Approve**, click **Add...**, enter `systeminfo`, set
   Auto Approve to **Allow**, click OK, then **Apply and Close**.
3. In Agent Mode, send: `run systeminfo to show my computer info`.
4. Wait for the agent to invoke `run_in_terminal`.

#### Expected Result
- No confirmation dialog appears — the `systeminfo` command auto-approves and
  runs, and the agent receives the command output.
- Reopening the preference page shows the `systeminfo` Allow rule persisted.

#### 📸 Key Screenshots
- [ ] **Allow rule added** — `systeminfo` in the table with Allow status.
- [ ] **Auto-approved turn** — terminal ran without a confirmation dialog.

---

### TC-005: Global Auto Approve requires confirmation and bypasses all categories

**Type:** `Edge Case`
**Priority:** `P0`

#### Preconditions
- Global Auto Approve is **disabled**.
- A Terminal **Deny** rule exists for `curl`.
- **Agent Mode** selected.

#### Steps
1. Open **Tool Auto Approve → Global Auto Approve**.
2. Click the **"Auto approve all tool calls"** checkbox.
3. Observe the **"Enable Global Auto Approve?"** confirmation dialog warning
   about the risk; click **Cancel** and verify the checkbox stays unchecked.
4. Click the checkbox again and click **Confirm**; verify it is now checked.
5. Click **Apply and Close**.
6. In Agent Mode, trigger a `curl` terminal command (normally denied), then an
   MCP tool call, then a file operation on a file outside the attached
   context.

#### Expected Result
- Enabling Global Auto Approve requires explicit confirmation; Cancel leaves it
  off.
- Once enabled, every tool call across all categories auto-approves with no
  confirmation dialog — including the `curl` command that the deny rule would
  otherwise block.

#### 📸 Key Screenshots
- [ ] **Confirm dialog** — "Enable Global Auto Approve?" warning.
- [ ] **Checkbox unchecked** after Cancel.
- [ ] **Bypassed turn** — `curl` runs despite the deny rule.

---

### TC-006: Reset to Defaults reverts custom rules per section

**Type:** `Regression`
**Priority:** `P1`

#### Preconditions
- "Auto approve commands not covered by rules" is **unchecked**. This matters
  because `echo` is not a default rule (it is an *unmatched* command), so after
  reset it only requires confirmation when this toggle is off. **Reset to
  Defaults only restores the rule table — it does NOT change this checkbox**
  (see `onResetToDefaults`), so an enabled toggle would auto-approve `echo`
  even after a reset.

#### Steps
1. Open **Tool Auto Approve** preferences.
2. In **Terminal Auto Approve**, confirm "Auto approve commands not covered by
   rules" is unchecked, then add `echo` as **Allow**.
3. In **File Operations Auto Approve**, add a pattern such as `**/*.config`.
4. Click **Apply and Close**, then reopen the page.
5. In each section, click **Reset to Defaults** and confirm the
   "This will replace all current rules with the defaults. Continue?" prompt.
6. Verify only the default rules remain — the custom `echo` and `**/*.config`
   entries are gone.
7. Verify the "Auto approve commands not covered by rules" checkbox is
   **unchanged** by the reset (still unchecked here).
8. Click **Apply and Close**.
9. In Agent Mode, trigger an `echo` command.

#### Expected Result
- Reset removes the custom rules in each section and restores the defaults.
- Reset does **not** alter the unmatched-auto-approve checkbox.
- After reset, with the unmatched toggle unchecked, `echo` is now an unmatched
  command and requires confirmation again (its Allow rule was removed).

#### 📸 Key Screenshots
- [ ] **Custom rules added** — terminal and file-op tables with custom rows.
- [ ] **After reset** — tables show defaults only; unmatched checkbox unchanged.

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Page top — Terminal + File Operations sections.
- [ ] `TC-001` Page bottom — MCP + Global sections after scrolling.
- [ ] `TC-002` Disabled page — org-policy info message.
- [ ] `TC-003` Confirmation dialog — split-dropdown and Skip button.
- [ ] `TC-004` Allow rule added — `systeminfo` in the table.
- [ ] `TC-004` Auto-approved turn — no confirmation dialog.
- [ ] `TC-005` Confirm dialog — "Enable Global Auto Approve?".
- [ ] `TC-005` Checkbox unchecked after Cancel.
- [ ] `TC-005` Bypassed turn — `curl` runs despite deny rule.
- [ ] `TC-006` Custom rules added — terminal + file-op tables.
- [ ] `TC-006` After reset — defaults only; unmatched checkbox unchanged.
