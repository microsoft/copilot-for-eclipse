# Chat: Add Context (Attach Files)

## Overview

Tests the **Add Context** button in the Chat view, which lets users attach
workspace files to a chat prompt. Attached files provide additional context to
Copilot so it can give more relevant answers. The button is an icon-only flat
button (tooltip: **Add Context...**) in the action bar's file reference area.

After attaching, files appear as **chips** below the input area showing the
file name and a close (×) button. Users can click a chip to open the file, or
click × to remove it.

Entry points exercised:
- **Ctrl+Alt+I** (or status bar → **Open Chat**), then click the Add Context
  button in the action bar.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- A signed-in Copilot account.
- A Java project is open in the workspace with at least two files.

---

## 1. Attach files, verify chips, remove, and send with context

### TC-001: Attach files via the Add Context button, verify file chips, remove a file, and send a prompt with attached context

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions

- The Chat view is open.
- The workspace contains at least two Java files.

#### Steps

1. Locate the **Add Context** button in the action bar (icon-only button with
   tooltip **Add Context...**).
2. Verify the button appears as a flat icon without a visible rectangular
   border.
3. Click the **Add Context** button.
4. Verify a file picker dialog opens (title: **Search attachments**).
5. Select two files from the workspace and click **OK**.
6. Verify the dialog closes and two **file chips** appear below the chat input
   area, each showing the file name and a close (×) button.
7. Click the × button on one of the file chips.
8. Verify that chip is removed and only one file chip remains.
9. Click on the remaining file chip's file name.
10. Verify the corresponding file opens (or is revealed) in the editor.
11. Type a prompt (e.g. `explain this file`) in the chat input and click
    **Send**.
12. Wait for the Copilot turn to complete.
13. Verify the response references or uses content from the attached file.

#### Expected Result

- The Add Context button opens the file picker dialog.
- Selected files appear as removable chips below the input area.
- Clicking × removes the chip.
- Clicking the file name opens the file in the editor.
- Sending a prompt with an attached file includes that file as context in the
  conversation.

#### 📸 Key Screenshots

- [ ] **Add Context button** — flat icon button in the action bar, no border.
- [ ] **File picker dialog** — dialog showing workspace files to select.
- [ ] **File chips** — two file chips visible below the chat input area.
- [ ] **After removing one chip** — only one chip remaining.
- [ ] **Response with context** — Copilot turn that references the attached
  file content.

---

## 2. Currently active file is shown automatically

### TC-002: The currently open file appears as a reference in the action bar

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions

- The Chat view is open.
- A Java file is open and active in the editor.

#### Steps

1. Open a Java file in the editor (click on it in the Package Explorer or
   switch to an already-open tab).
2. Switch focus to the Chat view.
3. Observe the file reference area in the action bar.

#### Expected Result

- The currently active editor file is automatically shown as a reference in
  the action bar (displaying the file name).
- Switching to a different file in the editor updates the displayed reference.

#### 📸 Key Screenshots

- [ ] **Current file reference** — the action bar showing the active file name
  as a context reference.

---

## Notes on failure modes

- File picker dialog does not open → the Add Context button's selection
  listener may not be wired; check the Eclipse error log.
- File chips do not appear after selection → the ReferencedFileService may not
  have received the files; verify the file selection result is non-empty.
- Clicking × does not remove the chip → the close button's mouse listener
  may not be attached; check the ReferencedFile widget.
