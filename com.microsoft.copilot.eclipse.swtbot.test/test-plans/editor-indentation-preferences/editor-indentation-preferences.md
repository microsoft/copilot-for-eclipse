# Editor Indentation Preferences for Inline Completions

## Overview
When Copilot produces an **inline completion (ghost text)**, the indentation it
suggests (tab character vs. spaces and the tab width) should follow the
platform's **default text editor preferences** instead of always defaulting to
spaces with a tab size of 4.

The formatting options are resolved by `FormatOptionProvider` and sent on each
completion request as `CompletionDocument.insertSpaces` / `tabSize` by
`CompletionProvider`. For Java and C/C++ projects the language-specific
formatter settings continue to apply. For every other case — an unknown
language, a file with no extension, or a file with no project — the provider
now reads the Eclipse text editor preferences
`org.eclipse.ui.editors/spacesForTabs` and `org.eclipse.ui.editors/tabWidth`.
If those preferences cannot be read it falls back to the previous hardcoded
defaults (spaces, tab width 4).

> **Important:** these options affect **inline completions only**, not the
> Agent-mode file create/edit tools. The verification must therefore be done
> by triggering ghost-text completions in an editor, not by asking Agent mode
> to write a file.

These preferences are configured under
**Preferences → General → Editors → Text Editors**:
- **"Insert spaces for tabs"** maps to `spacesForTabs`.
- **"Displayed tab width"** maps to `tabWidth`.

Entry points:
- Window → Preferences → General → Editors → Text Editors
- Typing in a text editor to trigger an inline completion (ghost text)

Not exercised:
- Direct unit-level invocation of `FormatOptionProvider` (covered by
  `FormatOptionProviderTests`).
- Agent-mode file create/edit tools (they do not consult these options).
- Java/C-specific formatter configuration screens.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- The user is signed in to GitHub Copilot and inline completions are enabled.
- A workspace with at least one open project that is **neither a Java nor a
  C/C++ project** (a General/empty project), so the editor text preferences
  are the deciding factor.
- Knowledge of the current values of **Preferences → General → Editors →
  Text Editors → "Insert spaces for tabs"** and **"Displayed tab width"** so
  they can be restored after testing.
- Because completion content is model-influenced, prefer a prompt/context that
  reliably forces an **indented multi-line suggestion** (e.g. an opening
  brace/block), and inspect the **raw whitespace** of the accepted text rather
  than relying on the displayed tab width.

---

## 1. Spaces configuration

### TC-001: Inline completion uses spaces when "insert spaces for tabs" is on

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- A non-Java/non-C project is open in the workspace.
- A file whose language is unknown / has no extension (e.g. a file named
  `sample` with code-like content) is open in the editor.

#### Steps
1. Open **Preferences → General → Editors → Text Editors**, check
   **"Insert spaces for tabs"**, set **"Displayed tab width"** to `2`, then
   click **Apply and Close**.
2. In the editor, type content that forces an indented multi-line
   continuation, for example an opening block and a newline:
   `function greet() {` then press Enter so Copilot suggests the indented
   body as ghost text.
3. Accept the inline completion (Tab).
4. Reveal whitespace ("Show whitespace characters") or inspect the raw bytes
   of the inserted lines.

#### Expected Result
- The accepted completion indents the body with **spaces** (not tabs), at
  **2 spaces** per level, matching the configured tab width.
- No error dialog appears and the Eclipse error log has no formatting
  exception.

#### 📸 Key Screenshots
- [ ] **Editor preferences** — "Insert spaces for tabs" checked, tab width 2.
- [ ] **Accepted completion whitespace** — Indentation shown as 2-space groups.

---

## 2. Tabs configuration

### TC-002: Inline completion uses tabs, and switching the preference updates the next request

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- A non-Java/non-C project is open in the workspace.
- An unknown-language / extensionless file is open in the editor.

#### Steps
1. Open **Preferences → General → Editors → Text Editors**, **uncheck**
   "Insert spaces for tabs", set **"Displayed tab width"** to `4`, then click
   **Apply and Close**.
2. In the editor, trigger an indented multi-line completion (as in TC-001) and
   accept it. Reveal whitespace / inspect raw bytes.
3. Change editor preferences to **spaces**, tab width `2`. Apply and close.
4. Trigger another indented completion in the same or a new unknown-language
   file and accept it. Reveal whitespace.

#### Expected Result
- The first accepted completion indents with **tab characters** (`\t`).
- After switching the preference, the next completion indents with **2
  spaces**.
- The provider reads the current editor preference value on **each completion
  request** rather than caching the first observed value.
- No error dialog appears and the Eclipse error log has no formatting
  exception.

#### 📸 Key Screenshots
- [ ] **Tabs completion** — Tab indentation under the tabs preference.
- [ ] **Spaces completion** — 2-space indentation after switching the preference.

---

## 3. Regression — language-specific projects are unaffected

### TC-003: Java completion still uses the Java formatter settings, not editor preferences

**Type:** `Regression`
**Priority:** `P0`

#### Preconditions
- A **Java** project is open in the workspace with its own
  formatter/indentation settings (e.g., tabs for Java).
- Editor preferences (General → Text Editors) are set to a **different**
  value than the Java formatter (e.g., spaces, tab width 2).
- A `.java` source file is open in the editor.

#### Steps
1. In the Java file, trigger an indented multi-line inline completion (e.g.
   inside a method body) and accept it.
2. Reveal whitespace / inspect raw bytes of the inserted lines.

#### Expected Result
- The Java completion follows the **Java formatter** indentation settings, not
  the generic text editor preferences.
- Only unknown/extensionless/projectless cases consult the editor
  preferences; Java (and C/C++) behavior is unchanged.

#### 📸 Key Screenshots
- [ ] **Java completion whitespace** — Indentation matches the Java formatter,
      independent of the General text-editor preference.

---

## Screenshots Checklist

- [ ] TC-001 — Editor preferences (spaces, width 2) + accepted completion with
      2-space indentation.
- [ ] TC-002 — Tabs completion vs. spaces completion after a preference switch.
- [ ] TC-003 — Java completion indentation unchanged by the General
      text-editor preference.
