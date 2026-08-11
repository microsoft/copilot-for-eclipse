# Support Editing and Creating Local Files Outside the Workspace

## Overview
Verify that Copilot Agent mode can edit and create local filesystem files that are outside the Eclipse workspace, and
that those changes are surfaced through the file change summary bar with the same review actions users expect for
workspace files.

This covers the user-visible flow for the `insert_edit_into_file` and `create_file` tools when the target is an
absolute local path rather than an Eclipse `IFile`.

Entry points:
- Window -> Show View -> Other... -> Copilot -> Copilot Chat -> Agent mode

Not exercised:
- Direct unit-level invocation of the file tools.
- Workspace-file edit coverage.
- Low-level compare editor APIs; this plan verifies the Compare UI through the summary bar.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
- The user is signed in to GitHub Copilot and Agent mode is available in the Copilot Chat view.
- A writable local directory outside the Eclipse workspace is available, for example:
  - Windows: `%TEMP%\\copilot-eclipse-local-file-tools`
  - macOS/Linux: `/tmp/copilot-eclipse-local-file-tools`
- The local directory contains an existing text file named `existing-local-file.txt` with this content:
  `before local edit`
- The local directory does not contain `created-local-file.txt` before the create-file test starts.

---

## 1. Edit an existing local file outside the workspace

### TC-001: Agent edits a local file and exposes the change in the summary bar

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Eclipse workbench is open.
- Copilot Chat is open in a fresh or cleared conversation.
- `existing-local-file.txt` exists outside the workspace and contains `before local edit`.

#### Steps
1. Open **Copilot Chat** from `Window -> Show View -> Other... -> Copilot -> Copilot Chat`.
2. Switch the chat mode selector to **Agent**.
3. Send a prompt that asks Agent mode to edit the external local file by absolute path, for example:
   `Edit <absolute path to existing-local-file.txt> so its entire content is exactly "after local edit".`
4. If Copilot asks for tool confirmation, approve the file edit operation.
5. Wait for the Agent turn to complete.
6. Verify the file change summary bar appears in the Chat view.
7. Verify the summary bar includes `existing-local-file.txt` and displays a local filesystem path for that file.
8. Click **View Diff** for `existing-local-file.txt`.
9. Verify the Compare editor opens and shows the original content `before local edit` against the modified content
   `after local edit`.
10. Close the Compare editor.

#### Expected Result
- Copilot completes the edit without reporting that the file is outside the workspace or cannot be edited.
- The local file on disk contains `after local edit`.
- The summary bar lists `existing-local-file.txt` even though it is not an Eclipse workspace file.
- The Compare editor opens from **View Diff** and shows the correct before/after content.
- No error dialog is shown. The Eclipse error log has no uncaught exception from `insert_edit_into_file`, local file
  path handling, or compare editor creation.

#### Key Screenshots
- [ ] **Agent edit prompt** -- Copilot Chat in Agent mode with the absolute local file path visible.
- [ ] **Summary bar after local edit** -- The changed local file appears in the file change summary bar.
- [ ] **Local file Compare editor** -- The Compare editor shows `before local edit` vs. `after local edit`.

#### Notes on failure modes
- The edit succeeds on disk but the file is missing from the summary bar -- the local `Path` change may not be tracked
  by the summary bar model.
- **View Diff** does nothing or throws an error -- local files may not be routed through the local Compare input path.
- The diff baseline shows the modified content on both sides -- the original content may not have been cached before
  applying the edit.

### TC-002: Keep clears the local file change and later edits use a new baseline

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Eclipse workbench is open.
- Copilot Chat is open in a fresh or cleared conversation.
- `existing-local-file.txt` exists outside the workspace and contains `before local edit`.
- Agent mode has edited `existing-local-file.txt` so it contains `after local edit`, and the file is listed in the
  summary bar.

#### Steps
1. Click **Keep** for `existing-local-file.txt` in the file change summary bar.
2. Verify the file is removed from the summary bar.
3. Send another Agent prompt to edit the same absolute file path so its entire content is exactly `second local edit`.
4. Approve the edit if prompted and wait for the turn to complete.
5. Click **View Diff** for `existing-local-file.txt`.
6. Verify the Compare editor shows `after local edit` as the original content and `second local edit` as the modified
   content.

#### Expected Result
- **Keep** accepts the current local file content and clears the tracked change.
- The next edit of the same local file starts a new diff baseline from the kept content.
- The file remains accessible through the summary bar and Compare editor after the second edit.

#### Key Screenshots
- [ ] **After Keep** -- The summary bar no longer lists the local file.
- [ ] **Second local diff** -- The Compare editor shows the kept content as the new baseline.

### TC-003: Undo restores the original local file content

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Eclipse workbench is open.
- Copilot Chat is open in a fresh or cleared conversation.
- `existing-local-file.txt` exists outside the workspace and contains `before local edit`.
- Agent mode has edited `existing-local-file.txt` so it contains `after local edit`, and the file is listed in the
  summary bar.

#### Steps
1. Click **Undo** for `existing-local-file.txt` in the file change summary bar.
2. Verify the file is removed from the summary bar.
3. Open `existing-local-file.txt` from the local filesystem and inspect its content.

#### Expected Result
- **Undo** restores the file to the original content captured before the tracked edit.
- The file is removed from the summary bar after undo completes.
- No error dialog is shown and the Eclipse error log has no local file undo exception.

#### Key Screenshots
- [ ] **Before Undo** -- The summary bar lists the edited local file.
- [ ] **After Undo** -- The summary bar no longer lists the local file and the file content is restored.

---

## 2. Create a new local file outside the workspace

### TC-004: Agent creates a local file and opens it from the summary bar

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- `created-local-file.txt` does not exist in the local test directory.
- Copilot Chat is open in Agent mode.

#### Steps
1. Send a prompt that asks Agent mode to create the external local file by absolute path, for example:
   `Create <absolute path to created-local-file.txt> with the exact content "created local content".`
2. If Copilot asks for tool confirmation, approve the file create operation.
3. Wait for the Agent turn to complete.
4. Verify `created-local-file.txt` exists on disk and contains `created local content`.
5. Verify the file change summary bar lists `created-local-file.txt`.
6. Click **View Diff** for `created-local-file.txt`.
7. Verify Eclipse opens `created-local-file.txt` in an editor and shows `created local content`.

#### Expected Result
- Copilot creates the local file without requiring it to be inside an Eclipse workspace project.
- The created file is listed in the summary bar.
- The created local file can be opened from the summary bar.
- No error dialog is shown and the Eclipse error log has no local file create or editor-open exception.

#### Key Screenshots
- [ ] **Agent create prompt** -- Copilot Chat in Agent mode with the absolute create path visible.
- [ ] **Summary bar after local create** -- The created local file appears in the file change summary bar.
- [ ] **Created local file editor** -- The external local file opens in an editor with the created content.

### TC-005: Undo removes a created local file

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Eclipse workbench is open.
- Copilot Chat is open in Agent mode.
- `created-local-file.txt` does not exist in the local test directory.
- Agent mode has created `created-local-file.txt` with content `created local content`, and the file is listed in the
  summary bar.

#### Steps
1. Click **Undo** for `created-local-file.txt` in the file change summary bar.
2. Verify the file is removed from the summary bar.
3. Verify `created-local-file.txt` no longer exists on disk.

#### Expected Result
- **Undo** for a created local file deletes the file, matching the create-file semantics.
- The summary bar no longer lists the created file after undo completes.
- No error dialog is shown and the Eclipse error log has no local file deletion exception.

#### Key Screenshots
- [ ] **Before created-file Undo** -- The summary bar lists `created-local-file.txt`.
- [ ] **After created-file Undo** -- The summary bar is clear and the file is absent from disk.

---

## 3. Navigate to local files from tool links

### TC-006: Tool result links open local files outside the workspace

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Eclipse workbench is open.
- Copilot Chat is open in Agent mode.
- The local test directory outside the workspace exists and contains `existing-local-file.txt`.

#### Steps
1. Send a prompt that causes Agent mode to reference or edit `existing-local-file.txt` by absolute path.
2. When the tool call appears in the Chat view, click the file path link for `existing-local-file.txt`.
3. Verify Eclipse opens `existing-local-file.txt` in an editor.

#### Expected Result
- File links for paths outside the Eclipse workspace open the local file in an Eclipse editor.
- No error dialog is shown and the Eclipse error log has no local file navigation exception.

#### Key Screenshots
- [ ] **Local file tool link** -- The tool result shows a clickable absolute path outside the workspace.
- [ ] **External local file editor** -- The external local file opens in an Eclipse editor.

### TC-007: Workspace directory and project links reveal in the Project Explorer

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The Eclipse workbench is open with at least one project (e.g., `demo`) that contains a sub-folder (e.g., `src`).
- The **Project Explorer** view is open.
- Copilot Chat is open in Agent mode.

#### Steps
1. Send a prompt that causes Agent mode to reference the workspace sub-folder by path (for example, ask it to list the
   contents of the `src` folder so the tool result renders a directory link).
2. When the tool call appears in the Chat view, click the directory link for the `src` folder.
3. Verify the `src` folder is selected and revealed in the **Project Explorer** (no external browser opens).
4. Send a prompt that causes Agent mode to reference the project root, then click the project link in the tool result.
5. Verify the project is selected and revealed in the **Project Explorer**.

#### Expected Result
- Clicking a workspace folder or project link reveals and selects that resource in the Project Explorer.
- No external browser or web page is opened for directory/project links.
- No error dialog is shown and the Eclipse error log has no navigation exception.

#### Key Screenshots
- [ ] **Directory tool link** -- The tool result shows a clickable workspace folder link.
- [ ] **Folder revealed** -- The `src` folder is selected and revealed in the Project Explorer.
- [ ] **Project revealed** -- The project root is selected and revealed in the Project Explorer.

#### Notes on failure modes
- Clicking the directory link opens a browser or does nothing -- the folder/project branch may not route through
  `UiUtils.revealInExplorer`.
- The resource is opened in an editor instead of being revealed -- folder/project types may be incorrectly treated as
  files.

### TC-008: File URI link with a line-number fragment opens the external local file

**Type:** `Edge Case`
**Priority:** `P2`

#### Preconditions
- The Eclipse workbench is open.
- Copilot Chat is open in Agent mode.
- The local test directory outside the workspace exists and contains `existing-local-file.txt` with multiple lines.

#### Steps
1. Send a prompt that causes Agent mode to reference `existing-local-file.txt` by absolute path with a line-number
   fragment (for example, a link ending in `#L10` or a `file:` URI with a `#L10` fragment).
2. When the tool call appears in the Chat view, click the file path link.
3. Verify Eclipse opens `existing-local-file.txt` in an editor (the trailing line-number fragment is treated as a
   fragment, not part of the file name).

#### Expected Result
- The line-number fragment is stripped when resolving the local path, so the correct external file opens.
- Eclipse does not report a missing file named `existing-local-file.txt#L10` or a path-resolution error.
- No error dialog is shown and the Eclipse error log has no local file navigation exception.

#### Key Screenshots
- [ ] **Fragment file link** -- The tool result shows a clickable local path with a `#L10` fragment.
- [ ] **External file opened** -- The external local file opens in an Eclipse editor despite the fragment.

#### Notes on failure modes
- Eclipse reports the file cannot be found -- the `#` fragment may not be stripped before resolving the local path.
- A relative-path or `https:` link is unexpectedly opened as a local file -- the URI-scheme guard in
  `FileUtils.getLocalFilePath` may be bypassed.
