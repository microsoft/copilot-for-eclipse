# Custom Instructions

## Overview

Tests the new **Custom Instructions** preference setting introduced in
[PR #136](https://github.com/microsoft/copilot-for-eclipse/pull/136).

The setting controls which projects' `.github/copilot-instructions.md` files
(and other custom instruction sources) are loaded into the Copilot chat. It is
exposed as a drop-down combo in **Window → Preferences → GitHub Copilot →
Custom Instructions**, inside the existing _Project Custom Instructions_ group.

| Option | Behaviour |
|--------|-----------|
| **all projects in workspace** (`ALL_PROJECTS`, default) | Custom instructions are loaded from every project in the Eclipse workspace — same as the behaviour before this change. |
| **projects inferred from chat-attached files** (`REFERENCED_PROJECTS`) | Custom instructions are loaded only from the parent projects of files/folders that are currently attached to the chat window. If nothing is attached, no custom instructions are loaded. |

The selected value is persisted in the Eclipse preference store under the key
`customInstructionsChatLoadScope` and is read by `ChatView#deriveWorkspaceFolders(...)`,
which uses it to compute the set of workspace folders sent to the language server on
each chat request.

Entry points:
- **Window → Preferences → GitHub Copilot → Custom Instructions** → _Load custom instructions from_ combo.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
- A GitHub account signed in with an active Copilot subscription.
- At least **two Java (or any language) projects** in the workspace, each
  containing a `.github/copilot-instructions.md` with a distinct, observable
  style rule:
  - **Project A**: `.github/copilot-instructions.md` containing `Always start every response with "[SRC-A]".`
  - **Project B**: `.github/copilot-instructions.md` containing `Always end every response with "[SRC-B]".`

  Using a prefix marker for one project and a suffix marker for the other makes it
  unambiguous whether one or both instruction sources are active at the same time.
- The Copilot Chat view is open and visible in the workbench.
- The Custom Instructions preference page is accessible via
  **Window → Preferences → GitHub Copilot → Custom Instructions**.

---

## 1. Preference Page UI

### TC-001: "Restore Defaults" resets the combo to the default value

**Type:** `Happy Path`
**Priority:** `P2`

#### Preconditions
- The _Load custom instructions from_ preference is currently set to
  **`projects inferred from chat-attached files`**.

#### Steps
1. Open **Window → Preferences → GitHub Copilot → Custom Instructions**.
2. Click **Restore Defaults**.
3. Observe the combo value (do **not** click Apply yet).
4. Click **Apply and Close**.
5. Re-open the preference page and observe the value.

#### Expected Result
- After **Restore Defaults** the combo switches back to
  **`all projects in workspace`** without closing the page.
- After **Apply and Close** and reopening, the combo still shows
  **`all projects in workspace`**.

---

## 2. Behaviour — "All Projects in Workspace" (default)

### TC-002: Custom instructions from all workspace projects are loaded in chat

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The workspace contains **Project A** and **Project B**, each with a
  `.github/copilot-instructions.md` that has identifiable content.
- The _Load custom instructions from_ preference is set to
  **`all projects in workspace`**.
- No files are attached to the chat window.

#### Steps
1. Open the Copilot Chat view.
2. Send: _"What are your response style requirements?"_
3. Observe the response.

#### Expected Result
- The response mentions both the `[A]` and `[B]` rules (or the reply itself
  ends sentences with both markers), confirming instructions from both projects
  are active.

---

## 3. Behaviour — "Projects Inferred from Chat-Attached Files"

### TC-003: Only the parent project of attached file is used for custom instructions

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The workspace contains **Project A** and **Project B**, each with
  `.github/copilot-instructions.md`.
- The _Load custom instructions from_ preference is set to
  **`projects inferred from chat-attached files`**.
- A file from **Project A only** is attached to the chat window (e.g. via
  the paperclip / context button).

#### Steps
1. Open the Copilot Chat view.
2. Attach a file from **Project A** to the chat.
3. Send: _"What are your response style requirements?"_
4. Observe the response.

#### Expected Result
- The response mentions only the `[A]` rule.
- The `[B]` rule is **not** mentioned — Project B's instructions are not loaded.

---

### TC-004: No attached files — no custom instructions loaded (referenced-projects mode)

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions
- Same two-project workspace.
- Preference is set to **`projects inferred from chat-attached files`**.
- No files are attached to the chat input.

#### Steps
1. Open the Copilot Chat view and verify the chat input has no attachments.
2. Send: _"What are your response style requirements?"_
3. Observe the response.

#### Expected Result
- The response mentions neither `[A]` nor `[B]` — no custom instructions are
  active.

---

### TC-005: Attaching files from multiple projects loads instructions from all their parent projects

**Type:** `Happy Path`
**Priority:** `P2`

#### Preconditions
- Workspace has Project A and Project B.
- Preference is **`projects inferred from chat-attached files`**.
- Files from **both** projects are attached to the chat.

#### Steps
1. Attach one file from Project A and one file from Project B to the chat.
2. Send: _"What are your response style requirements?"_
3. Observe the response.

#### Expected Result
- The response mentions both the `[A]` and `[B]` rules.

---

## 4. Switching Between Modes

### TC-006: Switching from "all projects" to "referenced projects" takes effect immediately for the next request

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- Preference is currently **`all projects in workspace`**.
- One file from Project A is attached to the chat.

#### Steps
1. Send: _"What are your response style requirements?"_ and confirm both `[A]`
   and `[B]` rules appear in the response.
2. Open **Window → Preferences → GitHub Copilot → Custom Instructions**,
   switch to **`projects inferred from chat-attached files`**, click
   **Apply and Close**.
3. Send the same prompt again (same attachment — one file from Project A).
4. Observe the response.

#### Expected Result
- In step 1, both `[A]` and `[B]` rules are mentioned.
- In step 4, only the `[A]` rule appears — the preference change takes effect
  immediately without restarting Eclipse.

---

### TC-007: Switching from "referenced projects" back to "all projects" restores full workspace folder set

**Type:** `Happy Path`
**Priority:** `P2`

#### Preconditions
- Preference is currently **`projects inferred from chat-attached files`**.
- Only a file from Project A is attached.

#### Steps
1. Send: _"What are your response style requirements?"_ and confirm only `[A]`
   appears.
2. Change preference back to **`all projects in workspace`** and apply.
3. Send the same prompt again.
4. Observe the response.

#### Expected Result
- In step 4, both `[A]` and `[B]` rules are mentioned again.


