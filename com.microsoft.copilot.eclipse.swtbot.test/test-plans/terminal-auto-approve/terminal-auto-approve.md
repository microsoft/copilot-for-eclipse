# Terminal Auto Approve

## Overview
Tests the terminal command auto-approve feature end-to-end: configuring rules
in the preference page, then triggering Agent Mode tool calls and observing
whether the confirmation dialog appears or the command runs automatically.

Each test case exercises the full stack: preference store → CLS sync →
Agent Mode prompt → tool confirmation request → `ConfirmationService` →
`TerminalConfirmationHandler` → dialog (or auto-approve) → terminal
execution. This mirrors the real user workflow: tweak settings, chat with
Copilot, observe behavior.

Entry points exercised:
- **Preferences → GitHub Copilot → Tool Auto Approve** — the terminal
  rule table (add / remove / toggle / reset).
- **Agent Mode chat** — sending prompts that trigger `run_in_terminal`
  tool calls.
- **Confirmation dialog** — the split-dropdown button with session/global
  allow actions.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- A signed-in Copilot account on the host machine.
- Network access to `api.githubcopilot.com`.
- **Agent Mode** selected in the chat mode dropdown.
- No previous auto-approve rules beyond the defaults (reset via
  "Reset to Defaults" before each scenario).

---

## 1. Default deny rules block dangerous commands

### TC-001: Default deny rules + Agent Mode terminal call

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Default rules are active (not modified).
- "Auto approve commands not covered by rules" is **unchecked**.

#### Steps
1. Open **Preferences → GitHub Copilot → Tool Auto Approve**.
2. Verify the "Terminal Auto Approve" section is visible with a table
   showing default deny rules (rm, rmdir, del, kill, curl, wget, eval,
   chmod, chown, and the regex rules for subshells / backticks / braces).
3. Confirm "Auto approve commands not covered by rules" is unchecked.
4. Close preferences.
5. Open the **Copilot Chat** view and select **Agent** mode.
6. Wait for the model picker to resolve.
7. Type the prompt: `please run curl https://example.com`.
8. Wait for a Copilot turn to stream — the agent should invoke the
   `run_in_terminal` tool with a `curl` command.
9. Observe the confirmation dialog that appears in the chat panel.
10. Verify the dialog shows:
    - Bold title: **"Run command in terminal"**
    - Message: *"The tool is about to run the following command in the
      terminal."*
    - The command text in a scrollable panel with `bg-command-panel`
      background.
    - A blue **"Allow Once ▾"** split-dropdown button and a **"Skip"**
      button.
11. Click the dropdown arrow on "Allow Once ▾".
12. Verify the dropdown menu contains:
    - "Allow 'curl' in this Session"
    - "Always Allow 'curl'"
    - "Allow this exact command in this Session" (if the full command
      differs from "curl")
    - "Always Allow this exact command"
    - "Allow all commands in this Session"
13. Click **"Skip"**.
14. Verify the command was NOT executed — the agent receives a dismiss
    result and continues without terminal output.

#### Expected Result
- Default deny rule for `curl` causes the confirmation dialog to appear.
- The dialog renders correctly with split-dropdown actions.
- Skipping prevents execution.

#### 📸 Key Screenshots
- [ ] Preference page with default rules visible.
- [ ] Confirmation dialog with dropdown expanded showing all actions.
- [ ] Agent turn after skip — no terminal output.

---

## 2. Custom allow rule auto-approves matching commands

### TC-002: Add allow rule → Agent auto-approves → verify execution

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Open **Preferences → GitHub Copilot → Tool Auto Approve**.
2. Click **"Add..."** in the Terminal Auto Approve section.
3. Enter `systeminfo` as command, select **"Allow"**, click OK.
4. Verify `systeminfo` appears in the table with "Allow" status.
5. Click **"Apply and Close"**.
6. In Agent Mode chat, type: `run systeminfo to show my computer info`.
7. Wait for the agent to invoke `run_in_terminal`.
8. Observe that **no confirmation dialog appears** — the command runs
   directly.
9. Wait for the tool call to complete — the agent should report terminal
   output containing system information.
10. Open preferences again and verify the rule is still present.

#### Expected Result
- The custom allow rule causes the `systeminfo` command to auto-approve.
- No confirmation dialog is shown.
- The terminal runs the command and the agent receives output.

#### 📸 Key Screenshots
- [ ] Preference page with `systeminfo` Allow rule added.
- [ ] Agent turn showing "✔ Ran run_in_terminal tool" without a
  confirmation dialog in between.
- [ ] Agent response containing system information output.

---

## 3. Session "Allow command name" persists within conversation

### TC-003: Allow name in Session → same command auto-approves → new
conversation resets

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Ensure no custom rules for `echo` exist (only defaults).
2. In Agent Mode, type: `run echo hello world`.
3. Confirmation dialog appears (echo has no matching rule and unmatched
   is disabled).
4. Click the dropdown arrow and select **"Allow 'echo' in this Session"**.
5. The command executes.
6. In the **same conversation**, type: `run echo second message`.
7. Observe that **no confirmation dialog appears** — the command
   auto-approves because `echo` is session-approved.
8. Verify the agent shows terminal output for both commands.
9. Start a **new conversation** (click "New Chat" or equivalent).
10. Type: `run echo third message`.
11. Observe that the **confirmation dialog appears again** — session
    approvals do not carry over to new conversations.

#### Expected Result
- First invocation: dialog shown, user selects session allow.
- Second invocation (same conversation): auto-approved.
- Third invocation (new conversation): dialog shown again.

#### 📸 Key Screenshots
- [ ] First dialog with dropdown showing "Allow 'echo' in this Session".
- [ ] Second invocation auto-approved — no dialog.
- [ ] New conversation — dialog reappears.

---

## 4. "Always Allow" persists globally

### TC-004: Always Allow command name → persists across conversations
and appears in preferences

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Ensure no custom rules for `dir` exist.
2. In Agent Mode, type: `list files in current directory`.
3. Confirmation dialog appears for the `dir` (or `ls`) command.
4. Click the dropdown and select **"Always Allow 'dir'"**.
5. The command executes.
6. Open **Preferences → Tool Auto Approve**.
7. Verify `dir` appears in the rules table with **"Allow"** status.
8. Close preferences.
9. Start a **new conversation**.
10. Type: `show me what files are here` (triggers `dir` again).
11. Observe that **no confirmation dialog appears** — the global rule
    persists.

#### Expected Result
- The "Always Allow" action writes the command name to the preference
  store.
- The rule appears in the preference page UI.
- The rule survives across conversations.

#### 📸 Key Screenshots
- [ ] Dropdown selection: "Always Allow 'dir'".
- [ ] Preference page showing `dir` as Allow rule.
- [ ] New conversation: `dir` auto-approved.

---

## 5. Exact command vs. command name distinction

### TC-005: Exact command Session approval only matches the same
command line

**Type:** `Edge Case`
**Priority:** `P1`

#### Steps
1. In Agent Mode, type: `run echo hello world`.
2. Confirmation dialog appears.
3. Click dropdown and select **"Allow this exact command in this
   Session"**.
4. The command executes.
5. In the same conversation, type: `run echo hello world` again.
6. Observe **auto-approved** — exact same command line.
7. In the same conversation, type: `run echo different text`.
8. Observe **confirmation dialog appears** — different command line,
   even though command name `echo` is the same.

#### Expected Result
- Exact command approval is strict: only the identical command line
  is auto-approved.
- A different argument string requires separate confirmation.

#### 📸 Key Screenshots
- [ ] First dialog: selecting "Allow this exact command in this Session".
- [ ] Same command: auto-approved.
- [ ] Different arguments: dialog appears again.

---

## 6. Simple command hides redundant exact-command actions

### TC-006: Single-word command shows minimal dropdown

**Type:** `Edge Case`
**Priority:** `P1`

#### Steps
1. In Agent Mode, trigger a single-word command like `ipconfig`
   (or `hostname`).
2. Confirmation dialog appears.
3. Click the dropdown arrow.
4. Count the menu items.

#### Expected Result
- "Allow 'ipconfig' in this Session" — present.
- "Always Allow 'ipconfig'" — present.
- "Allow this exact command in this Session" — **NOT present**
  (redundant: exact command = command name for single-word commands).
- "Always Allow this exact command" — **NOT present**.
- "Allow all commands in this Session" — present.

---

## 7. Unmatched auto-approve toggle

### TC-007: Enable unmatched auto-approve → unknown commands pass
through → disable → they require confirmation again

**Type:** `Happy Path`
**Priority:** `P1`

#### Steps
1. Open **Preferences → Tool Auto Approve**.
2. Check **"Auto approve commands not covered by rules"**.
3. Click **"Apply and Close"**.
4. In Agent Mode, type: `run hostname`.
5. Observe **auto-approved** — `hostname` has no matching rule but
   unmatched auto-approve is enabled.
6. Open preferences again.
7. **Uncheck** "Auto approve commands not covered by rules".
8. Click **"Apply and Close"**.
9. In the same or new conversation, type: `run hostname`.
10. Observe **confirmation dialog appears**.

#### Expected Result
- With unmatched enabled: unknown commands auto-approve.
- With unmatched disabled: unknown commands require confirmation.

#### 📸 Key Screenshots
- [ ] Preference: "Auto approve commands not covered by rules" checked.
- [ ] `hostname` auto-approved.
- [ ] Preference: unchecked.
- [ ] `hostname` shows confirmation dialog.

---

## 8. Regex rule integration

### TC-008: Add a case-insensitive regex allow rule → verify it
matches in Agent Mode

**Type:** `Happy Path`
**Priority:** `P2`

#### Steps
1. Open **Preferences → Tool Auto Approve**.
2. Click **"Add..."**, enter `/^npm\b/i`, select **"Allow"**, click OK.
3. Click **"Apply and Close"**.
4. In Agent Mode, type: `install lodash using npm`.
5. Agent invokes `npm install lodash`.
6. Observe **auto-approved** — regex `/^npm\b/i` matches.
7. Type: `run NPM run build` (uppercase).
8. Observe **auto-approved** — case-insensitive flag `/i` matches.

#### Expected Result
- The regex allow rule auto-approves matching commands.
- Case-insensitive flag works correctly.

---

## 9. "Allow all commands in this Session" — blanket approval

### TC-009: Allow all → every subsequent terminal call auto-approves
in this conversation

**Type:** `Happy Path`
**Priority:** `P1`

#### Steps
1. In Agent Mode, trigger any terminal command.
2. Confirmation dialog appears.
3. Click dropdown and select **"Allow all commands in this Session"**.
4. The command executes.
5. In the same conversation, send multiple prompts that trigger
   different terminal commands (e.g., "run dir", "run echo test",
   "run ipconfig").
6. Observe that **none of them** show a confirmation dialog.
7. Start a **new conversation**.
8. Trigger any terminal command.
9. Observe that the **confirmation dialog appears again**.

#### Expected Result
- "Allow all" is a blanket session-scoped approval.
- Every terminal command in the same conversation auto-approves.
- New conversations start fresh.

---

## 10. Reset to Defaults clears custom rules

### TC-010: Add custom rules → Reset → verify Agent Mode behavior
reverts

**Type:** `Happy Path`
**Priority:** `P2`

#### Steps
1. Open preferences and add `echo` as Allow, `javac` as Allow.
2. Apply and close.
3. Verify in Agent Mode: `echo hello` auto-approves.
4. Open preferences again.
5. Click **"Reset to Defaults"** and confirm.
6. Verify only default deny rules remain — `echo` and `javac` are gone.
7. Apply and close.
8. In Agent Mode, trigger `echo hello`.
9. Observe **confirmation dialog appears** — the custom allow rule was
   removed.

#### Expected Result
- Reset removes all custom rules.
- Commands that were previously allowed now require confirmation.

#### 📸 Key Screenshots
- [ ] Preference page after adding custom rules.
- [ ] Preference page after reset — only defaults.
- [ ] `echo hello` shows confirmation dialog post-reset.

---

## 11. Already-approved names filtered from dropdown

### TC-011: Session-approved command name hidden from dropdown
actions in multi-command scenario

**Type:** `Edge Case`
**Priority:** `P1`

#### Steps
1. In Agent Mode, trigger a command that includes `echo`
   (e.g., "run echo hello").
2. Confirmation dialog appears.
3. Select **"Allow 'echo' in this Session"** from the dropdown.
4. The command executes.
5. In the same conversation, send a prompt that triggers a
   multi-command like `echo test && curl https://example.com`.
6. Confirmation dialog appears (because `curl` is a default deny
   rule).
7. Click the dropdown arrow.

#### Expected Result
- The dropdown shows **"Allow 'curl' in this Session"** and
  **"Always Allow 'curl'"**.
- `echo` does **NOT** appear in the command-name actions — it was
  already session-approved.
- "Allow all commands in this Session" is still shown.
- Exact command actions (if shown) refer to the full multi-command
  string, not individual parts.

#### 📸 Key Screenshots
- [ ] First dialog: approving `echo` in session.
- [ ] Second dialog: dropdown showing only `curl`, not `echo`.

---

### TC-012: Global-approved command name hidden from dropdown

**Type:** `Edge Case`
**Priority:** `P1`

#### Steps
1. Open **Preferences → Tool Auto Approve**.
2. Add `echo` as **Allow** rule. Apply and close.
3. In Agent Mode, trigger `echo hello && hostname`.
4. Confirmation dialog appears (because `hostname` has no matching
   rule and unmatched is disabled).
5. Click the dropdown arrow.

#### Expected Result
- The dropdown shows **"Allow 'hostname' in this Session"** and
  **"Always Allow 'hostname'"**.
- `echo` does **NOT** appear — it is globally allowed.
- "Allow all commands in this Session" is still shown.

---

## 13. Subagent inherits parent session approvals

### TC-013: Session approval in main agent applies to subagent calls

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- No custom allow rules for `echo` or `cat` (only defaults).
- "Auto approve commands not covered by rules" is **unchecked**.

#### Steps
1. In Agent Mode, send a prompt that triggers `echo hello`.
2. Confirmation dialog appears.
3. Click dropdown and select **"Allow 'echo' in this Session"**.
4. The command executes.
5. In the **same conversation**, send a complex prompt that causes the
   agent to spawn a **subagent** (e.g., `use a subagent to run echo
   from subagent`).
6. The subagent invokes `run_in_terminal` with an `echo` command.
7. Observe that **no confirmation dialog appears** — the session
   approval from the main agent carries over to the subagent.
8. Still in the subagent context, the subagent invokes a different
   command (e.g., `cat somefile.txt`).
9. Observe that a **confirmation dialog appears** — `cat` was not
   session-approved.

#### Expected Result
- Subagent tool calls use the parent conversation's session rules.
- Commands approved in the main agent conversation auto-approve in
  subagent context.
- Commands NOT approved still require confirmation in subagent context.

#### 📸 Key Screenshots
- [ ] Main agent: approving `echo` in session.
- [ ] Subagent: `echo` auto-approved — no dialog.
- [ ] Subagent: `cat` shows confirmation dialog.

---

### TC-014: Session approval in subagent carries back to main agent

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- No custom allow rules for `hostname`.
- "Auto approve commands not covered by rules" is **unchecked**.

#### Steps
1. In Agent Mode, send a complex prompt that triggers a **subagent**.
2. The subagent invokes `run_in_terminal` with `hostname`.
3. Confirmation dialog appears.
4. Click dropdown and select **"Allow 'hostname' in this Session"**.
5. The command executes in the subagent context.
6. After the subagent completes, continue in the **same conversation**
   with the main agent.
7. Send a prompt that triggers `hostname` again (e.g., `what is my
   hostname?`).
8. Observe that **no confirmation dialog appears** — the session
   approval made during the subagent call is shared with the main
   conversation.

#### Expected Result
- Session approvals made during subagent execution are stored under
  the parent conversation's session scope.
- The main agent benefits from approvals granted in subagent context.

#### 📸 Key Screenshots
- [ ] Subagent: approving `hostname` in session.
- [ ] Main agent: `hostname` auto-approved — no dialog.

---

### TC-015: "Allow all commands in this Session" in main agent covers subagent

**Type:** `Edge Case`
**Priority:** `P2`

#### Steps
1. In Agent Mode, trigger any terminal command.
2. Confirmation dialog appears.
3. Select **"Allow all commands in this Session"** from the dropdown.
4. In the **same conversation**, send a prompt that spawns a subagent
   which runs multiple different terminal commands.
5. Observe that **none of them** show a confirmation dialog — the
   blanket session approval covers subagent calls too.

#### Expected Result
- "Allow all commands in this Session" is a blanket approval that
  applies to both main agent and subagent tool calls within the
  same conversation.

---

## 14. Edge Cases

### TC-016: "Always Allow" overrides existing deny rule in preferences

**Type:** `Edge Case`
**Priority:** `P1`

#### Steps
1. Open **Preferences → Tool Auto Approve**.
2. Click **"Add..."**, enter `curl`, select **"Deny"**, click OK.
3. Click **"Apply and Close"**.
4. In Agent Mode, type a prompt that triggers `curl` (e.g., `fetch
   https://example.com using curl`).
5. Confirmation dialog appears (deny rule blocks it).
6. Click dropdown and select **"Always Allow curl"**.
7. The command executes.
8. Open **Preferences → Tool Auto Approve** again.
9. Verify the `curl` rule has been changed from **Deny → Allow**.
10. Start a **new conversation**, trigger `curl` again.
11. Observe **auto-approved** — the global allow rule now applies.

#### Expected Result
- Clicking "Always Allow" in the dialog overrides an existing deny
  rule in preferences, changing it from deny to allow.
- The updated rule persists across conversations.
- The preferences table reflects the updated rule.

#### 📸 Key Screenshots
- [ ] Preferences: `curl → Deny` rule before override.
- [ ] Confirmation dialog: showing "Always Allow curl" action.
- [ ] Preferences: `curl → Allow` rule after override.
- [ ] New conversation: `curl` auto-approved without dialog.
