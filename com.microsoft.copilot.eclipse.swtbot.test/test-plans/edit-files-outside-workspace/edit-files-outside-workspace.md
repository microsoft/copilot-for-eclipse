# Support Editing/Creating Local Files Outside the Workspace

## Overview
Verify that Copilot Agent mode can edit and create local filesystem files whose paths are outside the Eclipse workspace, and that those changes appear in the file change summary bar with the same View Diff, Keep, and Undo actions available for workspace files.

---

## Test Cases

### TC-001: Agent edits an existing local file and it appears in the summary bar

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Eclipse IDE with the GitHub Copilot plugin is installed and the user is signed in.
- Copilot Chat is open in Agent mode.
- A writable local directory outside the Eclipse workspace exists (e.g. `%TEMP%\copilot-local-files` on Windows or `/tmp/copilot-local-files` on macOS/Linux).
- A file `existing-local-file.txt` exists in that directory containing exactly `before local edit`.

#### Steps
1. Open **Copilot Chat** (`Window → Show View → Other… → Copilot → Copilot Chat`).
2. Switch the mode selector to **Agent**.
3. Send a prompt: `Edit <absolute path to existing-local-file.txt> so its entire content is exactly "after local edit".`
4. If Copilot requests tool confirmation, approve the file edit operation.
5. Wait for the Agent turn to complete.
6. Inspect the file change summary bar in the Chat view.
7. Verify `existing-local-file.txt` is listed in the summary bar with a local filesystem path.
8. Click **View Diff** for `existing-local-file.txt`.
9. Verify the Compare editor shows `before local edit` on the left and `after local edit` on the right.
10. Close the Compare editor.
11. Open `existing-local-file.txt` from the local filesystem and verify its content is `after local edit`.

#### Expected Result
- Copilot completes the edit without reporting that the file is outside the workspace.
- `existing-local-file.txt` appears in the file change summary bar.
- The Compare editor correctly shows the before/after diff.
- The file on disk contains `after local edit`.
- No error dialog is shown; the Eclipse error log has no uncaught exception from the file tool or compare editor.

#### 📸 Key Screenshots
- [ ] **Agent edit prompt** — Copilot Chat in Agent mode with the absolute local path visible in the prompt.
- [ ] **Summary bar after edit** — The edited local file listed in the file change summary bar.
- [ ] **Compare editor** — The Compare editor showing `before local edit` vs. `after local edit`.

---

### TC-002: Keep clears the local file change and subsequent edits use the new baseline

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-001 has completed: `existing-local-file.txt` contains `after local edit` and is listed in the summary bar.

#### Steps
1. Click **Keep** for `existing-local-file.txt` in the file change summary bar.
2. Verify the file is removed from the summary bar.
3. Send another Agent prompt: `Edit <absolute path to existing-local-file.txt> so its entire content is exactly "second local edit".`
4. Approve the edit if prompted and wait for the turn to complete.
5. Click **View Diff** for `existing-local-file.txt` in the summary bar.
6. Verify the Compare editor shows `after local edit` as the baseline (left side) and `second local edit` as the new content (right side).

#### Expected Result
- **Keep** accepts the current file content and removes the entry from the summary bar.
- The next edit of the same local file starts a fresh diff baseline from the kept content.
- The Compare editor correctly reflects the new before/after pair.

#### 📸 Key Screenshots
- [ ] **After Keep** — Summary bar no longer lists the local file.
- [ ] **Second diff** — Compare editor showing `after local edit` as the new baseline.

---

### TC-003: Undo restores the original local file content

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-001 has completed: `existing-local-file.txt` contains `after local edit` and is listed in the summary bar.

#### Steps
1. Click **Undo** for `existing-local-file.txt` in the file change summary bar.
2. Verify the file is removed from the summary bar.
3. Open `existing-local-file.txt` from the local filesystem and verify its content.

#### Expected Result
- **Undo** restores the file to `before local edit` (the content captured before the tracked edit).
- The entry is removed from the summary bar after undo.
- No error dialog is shown; the Eclipse error log has no undo exception for a local file.

#### 📸 Key Screenshots
- [ ] **Before Undo** — Summary bar listing the edited local file.
- [ ] **After Undo** — Summary bar cleared; file content restored to original.

---

### TC-004: Agent creates a new local file outside the workspace

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Copilot Chat is open in Agent mode.
- `created-local-file.txt` does not exist in the local test directory.

#### Steps
1. Send a prompt: `Create <absolute path to created-local-file.txt> with the exact content "created local content".`
2. If Copilot requests tool confirmation, approve the file create operation.
3. Wait for the Agent turn to complete.
4. Verify `created-local-file.txt` exists on disk and contains `created local content`.
5. Verify the file change summary bar lists `created-local-file.txt`.
6. Click **View Diff** for `created-local-file.txt`.
7. Verify Eclipse opens the file in an editor showing `created local content` (empty-baseline diff or direct open).

#### Expected Result
- Copilot creates the local file without requiring it to be inside an Eclipse workspace project.
- The created file is listed in the summary bar.
- The file can be opened/diffed from the summary bar without errors.
- No error dialog is shown; the Eclipse error log has no local file create or editor-open exception.

#### 📸 Key Screenshots
- [ ] **Agent create prompt** — Copilot Chat in Agent mode with the absolute create path visible.
- [ ] **Summary bar after create** — The created local file listed in the file change summary bar.
- [ ] **Created file view** — The external local file opened in an editor with `created local content`.

---

### TC-005: Undo removes a newly created local file

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-004 has completed: `created-local-file.txt` exists on disk with `created local content` and is listed in the summary bar.

#### Steps
1. Click **Undo** for `created-local-file.txt` in the file change summary bar.
2. Verify the file is removed from the summary bar.
3. Verify `created-local-file.txt` no longer exists on the local filesystem.

#### Expected Result
- **Undo** for a created local file deletes the file from disk.
- The summary bar no longer lists the file after undo.
- No error dialog is shown; the Eclipse error log has no local file deletion exception.

#### 📸 Key Screenshots
- [ ] **Before Undo** — Summary bar listing `created-local-file.txt`.
- [ ] **After Undo** — Summary bar cleared; file absent from disk.

---

### TC-006: Workspace file behavior is unaffected by local file support

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- Eclipse workspace contains a project with a file `workspace-file.txt` containing `workspace original`.
- Copilot Chat is open in Agent mode.

#### Steps
1. Send a prompt: `Edit <workspace-relative or absolute path to workspace-file.txt> so its content is "workspace edited".`
2. Approve the edit if prompted and wait for the turn to complete.
3. Verify `workspace-file.txt` appears in the file change summary bar.
4. Click **View Diff** and verify the Compare editor shows the expected before/after content.
5. Click **Keep** and verify the workspace file is removed from the summary bar.

#### Expected Result
- Workspace file edits continue to work exactly as before.
- No regressions in View Diff, Keep, or Undo for workspace files.

---

### TC-007: Agent gracefully handles a non-writable local path

**Type:** `Negative`
**Priority:** `P1`

#### Preconditions
- Copilot Chat is open in Agent mode.
- A read-only file `readonly-local-file.txt` exists outside the workspace (set read-only via OS permissions).

#### Steps
1. Send a prompt: `Edit <absolute path to readonly-local-file.txt> so its content is "attempt edit".`
2. Approve any tool confirmation if prompted.
3. Observe the response in the Chat view.

#### Expected Result
- Copilot reports a failure or error to the user rather than silently succeeding.
- The summary bar does not list the file as a pending change.
- No unhandled exception in the Eclipse error log.

---

## Screenshots Checklist
> Consolidated list of all key screenshot moments.

- [ ] `TC-001` Agent edit prompt with absolute local path
- [ ] `TC-001` Summary bar after local edit
- [ ] `TC-001` Compare editor showing before/after local edit
- [ ] `TC-002` Summary bar after Keep (local file removed)
- [ ] `TC-002` Compare editor showing new baseline after Keep
- [ ] `TC-003` Summary bar before Undo (local file listed)
- [ ] `TC-003` Summary bar after Undo (local file removed, content restored)
- [ ] `TC-004` Agent create prompt with absolute local path
- [ ] `TC-004` Summary bar after local file create
- [ ] `TC-004` Created local file opened in editor
- [ ] `TC-005` Summary bar before Undo (created file listed)
- [ ] `TC-005` Summary bar after Undo (created file absent from disk)
