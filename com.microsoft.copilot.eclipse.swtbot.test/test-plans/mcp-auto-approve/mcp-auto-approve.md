# MCP Auto-Approve

## Overview

Tests the MCP tool auto-approve feature end-to-end: configuring rules in the
preference page, then triggering Agent Mode tool calls and observing whether
the confirmation dialog appears or the tool runs automatically.

Each test case exercises the full stack: preference store →
`McpConfirmationHandler` → dialog (or auto-approve) → tool execution. This
mirrors the real user workflow: tweak settings, chat with Copilot via an MCP
tool, observe behavior.

Entry points exercised:
- **Preferences → GitHub Copilot → Tool Auto Approve → MCP Configuration** —
  the "Trust MCP tool annotations" checkbox and the server/tool tree.
- **Agent Mode chat** — sending prompts that trigger MCP tool calls.
- **Confirmation dialog** — the split-dropdown button with session/global
  allow actions for tool and server scope.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- A signed-in Copilot account on the host machine.
- Network access to `api.githubcopilot.com`.
- **At least one MCP server configured** in the Copilot MCP settings, with
  at least one tool available. Note the server name and a tool name for use
  in the prompts below.
- **Agent Mode** selected in the chat mode dropdown.
- All MCP auto-approve preferences at their defaults before each scenario:
  no globally approved servers/tools, "Trust MCP tool annotations" unchecked.

---

## 1. Default behavior: confirmation dialog appears for MCP tools

### TC-001: MCP tool call with no rules → confirmation dialog

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- No global approved servers or tools.
- "Trust MCP tool annotations" is **unchecked**.

#### Steps
1. Open **Preferences → GitHub Copilot → Tool Auto Approve**.
2. Navigate to the **MCP Configuration** section.
3. Verify the server/tool tree shows no tools checked.
4. Confirm "Trust MCP tool annotations" is unchecked.
5. Close preferences.
6. Open the **Copilot Chat** view and select **Agent** mode.
7. Send a prompt that triggers the known MCP tool (e.g., `use <toolName> to
   <action>`).
8. Wait for the Copilot turn — the agent should invoke the MCP tool.
9. Observe the **confirmation dialog** that appears in the chat panel.
10. Verify the dialog shows:
    - Bold title: `Run '<toolName>' tool from '<serverName>' MCP server`.
    - A description of the MCP tool call.
    - A blue **"Allow Once ▾"** split-dropdown button and a **"Skip"** button.
11. Click the dropdown arrow on "Allow Once ▾".
12. Verify the dropdown contains:
    - "Allow '<toolName>' in this Session"
    - "Always Allow '<toolName>'"
    - "Allow tools from '<serverName>' in this Session"
    - "Always Allow tools from '<serverName>'"
13. Click **"Skip"**.
14. Verify the tool was **NOT** executed.

#### Expected Result
- Confirmation dialog appears for MCP tools with no auto-approve rules.
- Dropdown shows tool-level and server-level scoped actions.
- Skipping prevents execution.

#### 📸 Key Screenshots
- [ ] MCP preference section with no checked tools.
- [ ] Confirmation dialog with dropdown expanded showing all actions.
- [ ] Agent turn after skip — no tool output.

---

## 2. Session allow for a specific tool

### TC-002: "Allow tool in Session" → same tool auto-approves → new
conversation resets

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- No global approved servers or tools.
- "Trust MCP tool annotations" is **unchecked**.

#### Steps
1. In Agent Mode, send a prompt that triggers the MCP tool.
2. Confirmation dialog appears.
3. Click the dropdown arrow and select **"Allow '<toolName>' in this
   Session"**.
4. The tool executes.
5. In the **same conversation**, send another prompt that triggers the same
   tool.
6. Observe that **no confirmation dialog appears** — the tool is
   session-approved.
7. Start a **new conversation** (click "New Chat" or equivalent).
8. Send a prompt that triggers the same MCP tool.
9. Observe that the **confirmation dialog appears again** — session approvals
   do not carry over.

#### Expected Result
- Session approval auto-approves the same tool within the conversation.
- New conversation resets session approvals.

#### 📸 Key Screenshots
- [ ] First dialog: selecting "Allow '<toolName>' in this Session".
- [ ] Second invocation (same conversation): auto-approved, no dialog.
- [ ] New conversation: dialog reappears.

---

## 3. Session allow for an entire server

### TC-003: "Allow all tools from server in Session" → all tools from
that server auto-approve

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- No global approved servers or tools.
- "Trust MCP tool annotations" is **unchecked**.

#### Steps
1. In Agent Mode, send a prompt that triggers any tool from the target
   MCP server.
2. Confirmation dialog appears.
3. Click the dropdown and select **"Allow all tools from '<serverName>'
   in this Session"**.
4. The tool executes.
5. In the **same conversation**, send prompts that trigger **different tools**
   from the same server.
6. Observe that **none of them** show a confirmation dialog.
7. Start a **new conversation**.
8. Trigger any tool from the same server.
9. Observe that the **confirmation dialog appears again**.

#### Expected Result
- Server-level session approval covers all tools from that server.
- New conversation resets the session approval.

#### 📸 Key Screenshots
- [ ] Dropdown: selecting "Allow tools from '<serverName>' in this Session".
- [ ] Second tool from same server: auto-approved.
- [ ] New conversation: dialog reappears.

---

## 4. "Always Allow" for a specific tool — global persistence

### TC-004: "Always Allow '<toolName>'" → persists across conversations →
visible in preferences tree

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- No global approved servers or tools.
- "Trust MCP tool annotations" is **unchecked**.

#### Steps
1. In Agent Mode, trigger the MCP tool.
2. Confirmation dialog appears.
3. Click the dropdown and select **"Always Allow '<toolName>'"**.
4. The tool executes.
5. Open **Preferences → Tool Auto Approve → MCP Configuration**.
6. Verify the specific tool is **checked** in the server/tool tree.
7. Close preferences.
8. Start a **new conversation**.
9. Send a prompt that triggers the same MCP tool.
10. Observe that **no confirmation dialog appears** — the global rule persists.

#### Expected Result
- "Always Allow" writes the tool key to the global preference store.
- The tool appears checked in the preference tree.
- The approval persists across conversations.

#### 📸 Key Screenshots
- [ ] Dropdown: selecting "Always Allow '<toolName>'".
- [ ] Preference tree: tool checked.
- [ ] New conversation: tool auto-approved without dialog.

---

## 5. "Always Allow" for an entire server — global persistence

### TC-005: "Always Allow all tools from '<serverName>'" → server shown
checked in preferences

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- No global approved servers or tools (clear any rules written by TC-004
  if running in sequence).
- "Trust MCP tool annotations" is **unchecked**.

#### Steps
1. In Agent Mode, trigger any MCP tool.
2. Confirmation dialog appears.
3. Click the dropdown and select **"Always Allow all tools from
   '<serverName>'"**.
4. The tool executes.
5. Open **Preferences → Tool Auto Approve → MCP Configuration**.
6. Verify the server node is **checked** in the tree.
7. Close preferences.
8. Start a **new conversation** and trigger different tools from the same
   server.
9. Observe all tools **auto-approve without dialog**.

#### Expected Result
- "Always Allow" for server writes to the global servers list.
- The server row appears checked in the preference tree.
- All tools from the server auto-approve in new conversations.

#### 📸 Key Screenshots
- [ ] Dropdown: "Always Allow all tools from '<serverName>'".
- [ ] Preference tree: server node checked.
- [ ] New conversation: all server tools auto-approved.

---

## 6. Trust MCP tool annotations — read-only tools auto-approve

### TC-006: Enable "Trust MCP tool annotations" → tools with
readOnlyHint=true auto-approve

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- An MCP tool is available with `readOnlyHint=true` and `openWorldHint=false`
  in its annotations.

#### Steps
1. Open **Preferences → Tool Auto Approve → MCP Configuration**.
2. Check **"Trust MCP tool annotations"**.
3. Click **"Apply and Close"**.
4. In Agent Mode, send a prompt that triggers the read-only MCP tool.
5. Observe that **no confirmation dialog appears** — the tool auto-approves
   because it is annotated as read-only and closed-world.
6. Send a prompt that triggers an MCP tool that does NOT have
   `readOnlyHint=true` (or has `openWorldHint=true`).
7. Observe that the **confirmation dialog appears** — only strictly
   read-only + closed-world tools bypass confirmation.

#### Expected Result
- Tools with `readOnlyHint=true` AND `openWorldHint=false` auto-approve.
- All other tools still show the confirmation dialog.

#### 📸 Key Screenshots
- [ ] Preference: "Trust MCP tool annotations" checked.
- [ ] Read-only tool: auto-approved.
- [ ] Non-read-only tool: confirmation dialog shown.
