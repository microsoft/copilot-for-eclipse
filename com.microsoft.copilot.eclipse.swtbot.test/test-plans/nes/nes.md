# Next Edit Suggestions (NES)

## Overview

Tests the **Next Edit Suggestions** feature, which proactively shows AI-powered
code changes in the editor as the user types. When Copilot detects that a
recent edit implies a follow-up change elsewhere in the file, it highlights the
affected range with a background color, shows a diff popup with the proposed
change in green ghost text, and renders a Copilot icon in the editor gutter.

Users can:
- **Tab** to accept the suggestion (or jump to it if off-screen).
- **Esc** to dismiss the suggestion.
- **Click the gutter icon** to open an Accept / Reject action menu.
- **Hover the gutter icon** to see a tooltip with keyboard shortcuts.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- A signed-in Copilot account.
- **Enable Next Edit Suggestions** is checked in **Window → Preferences →
  GitHub Copilot → Completions** (this is the default).
- A Java project is open in the workspace with at least one Java source file.

---

## 1. Trigger a suggestion, inspect it, and accept via Tab

### TC-001: NES appears after related edits, shows diff and gutter icon, and is accepted with Tab

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions

- A Java file with multiple references to the same variable or method is open.
- NES is enabled in Preferences.

#### Steps

1. In the editor, make a series of related edits (e.g. rename a variable on
   one line, then move the cursor to another line where the same variable is
   used — or add a parameter to a method declaration, then navigate to a call
   site of that method).
2. After each edit, pause briefly (~1–3 seconds) and observe the editor.
3. Once a NES suggestion appears, verify:
   - A **background highlight** is shown on the text range that would be
     changed.
   - A **diff popup** appears next to the highlighted range showing the
     proposed replacement in **green ghost text**.
   - A **Copilot icon** appears in the editor gutter on the suggestion line.
4. Hover the mouse over the Copilot gutter icon and verify a tooltip appears
   showing keyboard shortcuts: **Tab: Accept suggestion** and
   **Esc: Dismiss suggestion**.
5. Press **Tab** on the keyboard.
6. Verify the suggestion is applied: the highlighted text is replaced with the
   proposed change, the diff popup disappears, the gutter icon disappears, and
   the file is modified (unsaved asterisk in the tab title).

#### Expected Result

- NES suggestion appears with highlight, diff popup, and gutter icon.
- Gutter icon tooltip shows Tab / Esc hints.
- Pressing Tab applies the change and clears all NES decorations.

#### 📸 Key Screenshots

- [ ] **NES suggestion visible** — editor showing the highlighted range, the
  diff popup with green ghost text, and the Copilot gutter icon.
- [ ] **Gutter icon tooltip** — tooltip showing "Tab: Accept / Esc: Dismiss".
- [ ] **After Tab accept** — editor showing the applied change with no
  remaining NES decorations.

---

## 2. Reject via Esc and use the gutter icon action menu

### TC-002: Dismiss a suggestion with Esc, trigger another, and use gutter icon Accept / Reject

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions

- A Java file is open in the editor.
- NES is enabled.

#### Steps

1. Make a series of related edits to trigger a NES suggestion (same approach
   as TC-001 steps 1–2).
2. Once the suggestion appears, press **Esc**.
3. Verify the diff popup, background highlight, and gutter icon all disappear.
4. Verify the file content is unchanged — no text was inserted or deleted.
5. Make another series of related edits to trigger a new suggestion.
6. Once the new suggestion appears, **click the Copilot icon** in the gutter.
7. Verify an action menu appears with two options: **Accept** and **Reject**.
8. Click **Reject** in the action menu.
9. Verify the suggestion is dismissed (same as pressing Esc: all decorations
   disappear, file unchanged).
10. Trigger yet another suggestion.
11. Click the gutter icon again and this time click **Accept**.
12. Verify the suggestion is applied (same result as pressing Tab in TC-001).

#### Expected Result

- Esc dismisses without modifying the file.
- Gutter icon click opens an Accept / Reject menu.
- Reject via menu dismisses the suggestion.
- Accept via menu applies the suggestion.

#### 📸 Key Screenshots

- [ ] **After Esc dismiss** — editor with no NES decorations, file unchanged.
- [ ] **Gutter icon menu** — the Accept / Reject action menu next to the
  gutter icon.
- [ ] **After menu Accept** — editor showing the applied change.

---

## 3. Out-of-viewport navigation

### TC-003: Tab jumps to an off-screen suggestion, then a second Tab accepts it

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions

- A long Java file (200+ lines) is open.
- NES is enabled.

#### Steps

1. Make a series of related edits to trigger a NES suggestion (same approach
   as TC-001).
2. Once the suggestion is visible (highlight, diff popup, gutter icon),
   **scroll away** from the suggestion line so it is no longer in the viewport.
3. Verify a **notification bar** (pill) appears at the bottom of the editor
   with a message like **"Press Tab to jump to Next Edit Suggestion"**.
4. Press **Tab**.
5. Verify the editor scrolls back to reveal the suggestion line, the diff
   popup and gutter icon become visible again, and the notification bar
   disappears.
6. Verify the suggestion is **not** accepted yet — the highlight and diff
   popup are still showing.
7. Press **Tab** again.
8. Verify the suggestion is now accepted and all NES decorations disappear.

#### Expected Result

- First Tab: jumps to the suggestion without accepting it.
- Second Tab: accepts the suggestion.

#### 📸 Key Screenshots

- [ ] **Bottom bar visible** — notification pill at the bottom of the editor.
- [ ] **After first Tab** — editor scrolled to show the suggestion with diff
  popup and gutter icon.
- [ ] **After second Tab** — suggestion accepted, all decorations cleared.

---

## 4. Suggestion auto-dismiss on further edits

### TC-004: Editing the file while a suggestion is active causes it to disappear

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions

- A Java file is open in the editor.
- NES is enabled.

#### Steps

1. Trigger a NES suggestion via a series of related edits (same approach as
   TC-001).
2. Once the suggestion is visible (highlight, diff popup, gutter icon), do
   **not** accept or reject it.
3. Instead, type additional text anywhere in the file (e.g. add a comment on a
   different line, or directly edit the text in the suggested range).
4. Observe whether the NES suggestion disappears immediately after the edit.

#### Expected Result

- The NES suggestion (highlight, diff popup, gutter icon) disappears as soon
  as the file content is modified by the user.
- The newly typed text is applied normally — no conflict with the old
  suggestion.

#### 📸 Key Screenshots

- [ ] **Suggestion visible before edit** — NES decorations present.
- [ ] **After typing** — all NES decorations gone, user's new edit applied.

---

## 5. Enable / disable toggle

### TC-005: Disabling and re-enabling NES in Preferences

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions

- NES is currently enabled.
- A Java file is open.

#### Steps

1. Open **Window → Preferences → GitHub Copilot → Completions**.
2. Uncheck **Enable Next Edit Suggestions**.
3. Click **Apply and Close**.
4. Make a series of related edits in the Java file (same approach as TC-001).
5. Verify no NES suggestion appears (no highlight, no diff popup, no gutter
   icon).
6. Open **Window → Preferences → GitHub Copilot → Completions** again.
7. Check **Enable Next Edit Suggestions**.
8. Click **Apply and Close**.
9. Make a series of related edits in the Java file again.
10. Verify a NES suggestion appears.

#### Expected Result

- Disabling: no NES decorations appear after edits.
- Re-enabling: NES suggestions resume without restarting Eclipse.
- Inline tab-completions are unaffected by either toggle.

#### 📸 Key Screenshots

- [ ] **NES disabled** — Preferences page with the checkbox unchecked.
- [ ] **No suggestion after edit** — editor with no NES decorations after
  editing with NES disabled.
- [ ] **NES re-enabled** — suggestion appears again after re-enabling.

---

## 6. No interference with other editor decorations

### TC-006: No red rectangular border from Copilot appears on SonarQube issue locations

**Type:** `Edge Case`
**Priority:** `P1`

#### Preconditions

- Both **GitHub Copilot for Eclipse** and **SonarQube for Eclipse** are
  installed.
- NES is enabled.
- A Java project is open with a file that contains a SonarQube issue. For
  example, a file like:
  ```java
  import java.util.List;

  public class TestSonar {
      boolean isEmpty(List l) {
          return l == null || l.size() == 0;
      }
  }
  ```
  where `l.size() == 0` triggers a SonarQube rule.
- SonarQube analysis has completed and its markers are visible in the editor.

#### Steps

1. Open the Java file that has SonarQube issue markers.
2. Do **not** make any edits — just observe the editor.
3. Check whether a **red rectangular border** (BOX style) with the label
   `"Text to be Deleted"` appears around the SonarQube issue location.
4. Hover over the SonarQube-annotated area and verify the tooltip shows
   SonarQube's own message — not `"Text to be Deleted"`.
5. If a SonarQube quick fix is available, apply it.
6. Verify the red border does not persist after the quick fix is applied.
7. Open a different Java file that has compiler warnings but no SonarQube
   issues, and verify no Copilot NES decorations appear on unedited code.

#### Expected Result

- No red rectangular border (`"Text to be Deleted"` BOX annotation) from
  Copilot appears on SonarQube issue locations.
- SonarQube markers display normally with their own styling.
- Hovering SonarQube markers shows SonarQube's message, not Copilot's.
- After applying a SonarQube quick fix, no Copilot annotation persists.
- On unedited files without SonarQube, no NES decorations appear either.

#### 📸 Key Screenshots

- [ ] **SonarQube file — no NES** — editor showing SonarQube markers with no
  Copilot decorations overlapping.
- [ ] **SonarQube hover** — tooltip showing the SonarQube message, not a
  Copilot annotation.

---

## Notes on failure modes

- No suggestion appears after editing → NES may be disabled in Preferences, or
  the Copilot account is not signed in; verify both and retry.
- Red rectangular border appears on SonarQube markers → the NES annotation
  type is still mapped to the root `textmarker`; check that `plugin.xml` does
  not declare `markerType="org.eclipse.core.resources.textmarker"` on the NES
  annotation types.
- Accepting a suggestion has no effect → the suggestion may have become stale
  (the underlying code changed again before Tab was pressed); make a fresh
  edit to trigger a new suggestion and accept immediately.
