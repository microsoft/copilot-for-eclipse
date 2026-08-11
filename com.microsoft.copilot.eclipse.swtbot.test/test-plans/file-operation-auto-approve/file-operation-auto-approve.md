# File Operation Auto Approve

## Overview
Tests the file-operation (read/write) auto-approve feature end-to-end:
configuring glob-pattern rules in the preference page, attaching context
files, then triggering Agent Mode tool calls and observing whether the
confirmation dialog appears or the operation runs automatically.

Each test case exercises the full stack: preference store → CLS sync →
Agent Mode prompt → tool confirmation request → `ConfirmationService` →
`FileOperationConfirmationHandler` → dialog (or auto-approve) → file
operation execution. This mirrors the real user workflow: tweak settings,
attach files, chat with Copilot, observe behavior.

Entry points exercised:
- **Preferences → GitHub Copilot → Tool Auto Approve** — the file
  operation rule table (add / remove / toggle / reset).
- **Agent Mode chat** — sending prompts that trigger `copilot.read_file`,
  `copilot.editFile`, `copilot.createFile`, or `copilot.deleteFile` tool
  calls.
- **Confirmation dialog** — the button with session/global allow actions.
- **Attached files** — the context panel for user-attached files.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- A signed-in Copilot account on the host machine.
- Network access to `api.githubcopilot.com`.
- **Agent Mode** selected in the chat mode dropdown.
- A workspace with at least one Java project open (e.g., `demo`).
- No previous file-operation auto-approve rules beyond the defaults
  (reset via "Reset to Defaults" before each scenario).
- "Auto approve file operations not covered by rules" is **unchecked**
  unless the test specifies otherwise.

---

## 1. Default behavior and dialog UI

### TC-001: File read in workspace triggers confirmation dialog

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Default rules are active (not modified).
- "Auto approve file operations not covered by rules" is **unchecked**.

#### Steps
1. Open **Preferences → GitHub Copilot → Tool Auto Approve**.
2. Verify the "File Operation Auto Approve" section is visible with a
   table showing default deny rules (e.g., `.github/instructions/*`,
   `github-copilot/**/*`).
3. **Manually uncheck** "Auto approve file operations not covered by rules"
   (system default is checked; uncheck it for this test). Close preferences.
4. Open the **Copilot Chat** view, select **Agent** mode.
5. Type: `read the file src/demo/App.java and summarize it`.
6. Wait for the agent to invoke the `copilot.read_file` tool.
7. Observe the confirmation dialog. Verify it shows:
   - Title: **"Read file"**, message mentioning the file name.
   - **"Allow Once"** button with dropdown, and a **"Skip"** button.
8. Click the dropdown arrow. Verify the menu contains:
   - "Allow this file in this Session"
   - "Always Allow"
9. Click **"Skip"**.
10. Verify the agent receives a dismiss result — no file content.

#### Expected Result
- No matching allow rule → confirmation dialog appears.
- Dialog renders correctly with session/global actions.
- Skip prevents execution.

#### 📸 Key Screenshots
- [ ] Preference page with default rules.
- [ ] Confirmation dialog with dropdown expanded.
- [ ] Agent turn after skip.

---

## 2. Attached file auto-approval

### TC-002: Attached file auto-approves; deny rule does not block attached files

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- **Manually uncheck** "Auto approve file operations not covered by rules"
  (system default is checked; uncheck it for this test).

#### Steps
1. Open preferences, add a deny rule `**/*.java` → **Deny**.
   Apply and close.
2. Open `src/demo/App.java` in the editor.
3. Open the **Copilot Chat** view, select **Agent** mode.
4. Attach `App.java` via the context panel (paperclip / "Add Context").
5. Type: `read App.java and explain what it does`.
6. Observe **no confirmation dialog** — the file is auto-approved
   because it is attached, even though the deny rule matches.
7. Verify the agent reads and summarizes the file content.
8. In the **same conversation**, type: `now read Helper.java`.
9. Observe **confirmation dialog appears** — `Helper.java` is not
   attached and the deny rule matches.

#### Expected Result
- Attached file auto-approval takes precedence over deny rules.
- Non-attached files still respect rules normally.

#### 📸 Key Screenshots
- [ ] Context panel showing attached `App.java`.
- [ ] Agent auto-approved read without dialog.
- [ ] Dialog appears for non-attached `Helper.java`.

---

## 3. Session-level file approval

### TC-003: Session approval — read, then write, then new conversation

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- **Manually uncheck** "Auto approve file operations not covered by rules"
  (system default is checked; uncheck it so the initial read triggers a dialog).

#### Steps
1. Ensure no custom rules exist for the test file.
2. In Agent Mode, type: `read src/demo/App.java`.
3. Confirmation dialog appears.
4. Click dropdown → **"Allow this file in this Session"**.
5. The file is read successfully.
6. In the **same conversation**, type: `read src/demo/App.java again`.
7. Observe **auto-approved** — session cache hit.
8. In the **same conversation**, type: `add a comment "// test" to
   the top of src/demo/App.java`.
9. The agent invokes `copilot.editFile` for `App.java`.
10. Observe **auto-approved** — session approval is path-based, covers
    both reads and writes.
11. Start a **new conversation** (click "New Chat").
12. Type: `read src/demo/App.java`.
13. Observe **confirmation dialog appears** — session approvals do not
    carry to new conversations.

#### Expected Result
- Session approval: same file re-read auto-approves.
- Session approval: write to same file auto-approves.
- New conversation: resets session state.

#### 📸 Key Screenshots
- [ ] First dialog: selecting "Allow this file in this Session".
- [ ] Write auto-approved in same conversation.
- [ ] New conversation: dialog reappears.

---

## 4. Global rules — allow, deny, and "Always Allow" from dialog

### TC-004: Glob allow and deny rules with unmatched toggle

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Open **Preferences → GitHub Copilot → Tool Auto Approve**.
2. Add rule: `**/*.java` → **Allow**. Click OK.
3. Add rule: `**/secret/**` → **Deny**. Click OK.
4. Enable **"Auto approve file operations not covered by rules"**.
5. Click **"Apply and Close"**.
6. In Agent Mode, type: `read src/demo/App.java`.
7. Observe **auto-approved** — matches `**/*.java` allow rule.
8. Type: `read src/secret/config.properties`.
9. Observe **confirmation dialog** — matches `**/secret/**` deny rule,
   even though unmatched is enabled.
10. Click **"Skip"**.
11. Type: `read README.md`.
12. Observe **auto-approved** — no rule matches, unmatched fallback.
13. Open preferences, **uncheck** "Auto approve file operations not
    covered by rules". Apply and close.
14. Type: `read README.md`.
15. Observe **confirmation dialog** — unmatched now disabled.

#### Expected Result
- Allow glob rule auto-approves matching files.
- Deny glob rule blocks matching files even with unmatched enabled.
- Unmatched toggle controls fallback for non-matching files.

#### 📸 Key Screenshots
- [ ] Preference page with both rules.
- [ ] `.java` auto-approved, `secret/**` denied, `README.md` varies.

---

### TC-005: "Always Allow" persists as global rule and overrides deny

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- **Manually uncheck** "Auto approve file operations not covered by rules"
  (system default is checked; uncheck it for this test).

#### Steps
1. Open preferences, add deny rule for the file's absolute path
   (e.g., `C:\<your-workspace-path>\demo\src\demo\App.java`) → **Deny**. Apply and close.
2. In Agent Mode, trigger a file read for `App.java`.
3. Confirmation dialog appears (deny rule matches).
4. Click dropdown → **"Always Allow"**.
5. The file is read.
6. Open **Preferences → Tool Auto Approve**.
7. Verify the rule changed from **Deny → Allow** (no duplicate).
8. Close preferences.
9. Start a **new conversation**.
10. Type: `read src/demo/App.java`.
11. Observe **auto-approved** — the updated global rule persists.

#### Expected Result
- "Always Allow" writes/updates the file path as a global allow rule.
- Overrides existing deny rule (case-insensitive match, no duplicates).
- Persists across conversations.

#### 📸 Key Screenshots
- [ ] Preference page: deny → allow transition.
- [ ] New conversation: auto-approved.

---

## 5. Outside-workspace files and folder-level approval

### TC-006: Outside-workspace file requires confirmation with folder
approval

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Enable "Auto approve file operations not covered by rules".
2. Add allow rule `**/*`. Apply and close.
3. In Agent Mode, type: `read C:\temp\test\file1.txt`
   (path outside workspace).
4. Observe **confirmation dialog** — outside-workspace files always
   require confirmation regardless of rules.
5. Verify the dialog offers folder-level approval:
   - "Allow Once"
   - "Allow files in 'test' folder in this Session"
   - "Skip"
6. Click dropdown → **"Allow files in 'test' folder in this Session"**.
7. The file is read.
8. In the same conversation, trigger read for `C:\temp\test\file2.txt`.
9. Observe **auto-approved** — same folder.
10. Trigger read for `C:\temp\other\file3.txt`.
11. Observe **confirmation dialog** — different folder.

#### Expected Result
- Outside-workspace files bypass rules, always show dialog.
- Folder-level session approval covers sibling files.
- Different folders still require confirmation.

#### 📸 Key Screenshots
- [ ] Dialog with folder-level action.
- [ ] Same-folder auto-approved.
- [ ] Different-folder dialog.

---

## 6. Session approval overrides deny rule

### TC-007: Session approval overrides deny rule within conversation

**Type:** `Edge Case`
**Priority:** `P1`

#### Steps
1. Add deny rule `**/App.java` in preferences. Apply and close.
2. In Agent Mode, trigger a read for `src/demo/App.java`.
3. Confirmation dialog appears (deny rule).
4. Click dropdown → **"Allow this file in this Session"**.
5. The file is read.
6. In the same conversation, trigger another read for `App.java`.
7. Observe **auto-approved** — session approval checked before rules.

#### Expected Result
- Session-level approval overrides global deny rules.

---

## 7. Subagent inherits parent session approvals

### TC-008: Session file approval applies to subagent

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- No custom rules for the test file.
- "Auto approve file operations not covered by rules" is **unchecked**.

#### Steps
1. In Agent Mode, trigger a read for `App.java`.
2. Confirmation dialog appears.
3. Select **"Allow this file in this Session"**.
4. The file is read.
5. In the **same conversation**, send a prompt that spawns a subagent
   (e.g., `use a subagent to analyze App.java`).
6. The subagent invokes `copilot.read_file` for `App.java`.
7. Observe **auto-approved** — session approval carries to subagent.

#### Expected Result
- Subagent shares parent conversation's session scope.

---

## 8. Reset to Defaults

### TC-009: Reset clears custom rules and reverts behavior

**Type:** `Happy Path`
**Priority:** `P2`

#### Steps
1. Add custom rules: `**/*.java` → Allow, `**/secret/*` → Deny.
   Apply and close.
2. Verify in Agent Mode: `.java` files auto-approve.
3. Open preferences, click **"Reset to Defaults"** and confirm.
4. **Manually uncheck** "Auto approve file operations not covered by rules"
   (Reset to Defaults only clears rules, it does not reset this checkbox).
   Apply and close.
5. Verify only default deny rules remain (`.github/instructions/*`,
   `github-copilot/**/*`).
5. Apply and close.
6. In Agent Mode, trigger a `.java` file read.
7. Observe **confirmation dialog** — custom allow rule removed.

#### Expected Result
- Reset removes all custom rules and restores defaults.

#### 📸 Key Screenshots
- [ ] Preference page after reset — only defaults.
- [ ] `.java` file shows confirmation dialog post-reset.

---

## 9. Customization file reads (skills, instructions, prompts, agents)

Reading a Copilot customization file is always auto-approved, even with
"Auto approve file operations not covered by rules" **unchecked** and even
when a deny rule would otherwise match (e.g. the default `.github/instructions/*`
rule). The exemption is read-only and covers both workspace and user-global
(`~/.copilot`, `~/.claude`, `~/.agents`) locations. Recognized files:
`<skills-dir>/<name>/**` (the whole skill folder, including helper files),
`*.instructions.md`, `*.prompt.md`, `*.agent.md`, and the fixed names
`copilot-instructions.md`, `git-commit-instructions.md`, `AGENTS.md`, `CLAUDE.md`.

### TC-010: Customization reads auto-approve; edits and ordinary files still prompt

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- "Auto approve file operations not covered by rules" is **unchecked**.
- The project contains `.github/skills/demo/SKILL.md` (whose body also tells
  Copilot to read a sibling `notes.md`), `.github/instructions/coding.instructions.md`,
  and `.github/copilot-instructions.md`.

#### Steps
1. In Agent Mode, invoke the demo skill (or ask Copilot to read
   `.github/skills/demo/SKILL.md`).
2. Observe **no confirmation** for `SKILL.md` **or** the referenced `notes.md`.
3. Type: `read the project instructions and summarize them`.
4. Observe **no confirmation** for `coding.instructions.md` — even though the
   default `.github/instructions/*` deny rule matches.
5. Type: `read .github/copilot-instructions.md`.
6. Observe **no confirmation**.
7. Type: `add a comment line to .github/skills/demo/SKILL.md`.
8. Observe **confirmation dialog appears** — editing is not exempt.
9. Type: `read src/demo/App.java`.
10. Observe **confirmation dialog appears** — ordinary files are unaffected.

#### Expected Result
- Reads of skill files (and their helpers), instruction/prompt/agent files, and
  the fixed well-known files auto-approve with no dialog.
- Editing a customization file still prompts.
- Ordinary (non-customization) reads still prompt.

#### 📸 Key Screenshots
- [ ] Skill `SKILL.md` + `notes.md` read with no confirmation.
- [ ] Instruction file read with no confirmation despite the deny rule.
- [ ] Edit to `SKILL.md` shows a confirmation dialog.

---

### TC-011: User-global customization files auto-approve

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- "Auto approve file operations not covered by rules" is **unchecked**.
- A user-global skill exists, e.g. `~/.copilot/skills/demo-global/SKILL.md`.

#### Steps
1. In Agent Mode, invoke the global demo skill (or ask Copilot to read its
   `SKILL.md`).
2. Observe **no confirmation** — global customization files are exempt even
   though they live outside the workspace (an ordinary outside-workspace file
   would still prompt, per TC-006).

#### Expected Result
- User-global customization reads auto-approve without a dialog.

#### 📸 Key Screenshots
- [ ] Global skill read with no confirmation.
