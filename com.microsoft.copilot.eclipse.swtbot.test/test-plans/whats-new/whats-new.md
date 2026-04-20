# What's New Page

## Overview
Tests the "What's New" page feature of the GitHub Copilot for Eclipse plugin. The
page renders the contents of the bundled `intro/whatsnew/WHATISNEW.md` release
notes as HTML in an Eclipse editor, so users can see the latest features shipped
with the plugin.

Entry points:
- Copilot status-bar icon → **What's New** menu item.
- (Optional, not exercised in TC-001) Eclipse Welcome page → "What's New" quick link.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
- The plugin bundle ships `intro/whatsnew/WHATISNEW.md` (the source of the
  rendered page). The top-most level-1 release section in that file is the
  "latest feature" referenced below.
- A user signed in to GitHub Copilot is **not** required to open the page, but
  signing in first keeps the environment consistent with normal usage.
- No previously opened `WHATISNEW.html` editor tab is currently visible (close
  it if present to guarantee a clean observation of the open action).

---

## 1. Open What's New

### TC-001: Open What's New and verify latest feature is shown

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- The Eclipse workbench is open with at least one project in the workspace.
- Any previously opened `WHATISNEW.html` / `WHATISNEW.md` editor tab is closed.
- The Copilot status-bar icon is visible in the Eclipse status bar.

#### Steps
1. Click the **GitHub Copilot** icon in the Eclipse status bar to open the
   Copilot status-bar menu.
2. In the menu, click **What's New**.
3. Wait for a new editor tab to open in the workbench.
4. Inspect the opened editor:
   1. Verify the editor tab title corresponds to the generated HTML file
      (e.g. `WHATISNEW.html`) rendered via the internal browser editor
      (`org.eclipse.ui.browser.editor`). A fallback to the default text editor
      showing `WHATISNEW.md` is also acceptable if the browser editor is not
      available on the platform.
   2. Verify the page body renders the release notes as formatted HTML
      (headings, bullet lists, and images are visible — not raw Markdown
      syntax).
5. Locate the top-most level-1 release section in the page (the first
   `# GitHub Copilot <version> Release Notes` heading).
6. Verify that this top-most section, and the first feature sub-heading beneath
   it, match the top of the bundled `intro/whatsnew/WHATISNEW.md` file shipped
   with the installed plugin build. At the time of writing, the expected
   latest entry is:
   - Release heading: `GitHub Copilot 0.16.0 Release Notes`
   - First feature: `Tool Calling in Ask Mode`

#### Expected Result
- Clicking **What's New** from the Copilot status-bar menu opens an editor tab
  that renders the plugin's release notes.
- The latest release section from the bundled `WHATISNEW.md` appears at the top
  of the page, and its first feature sub-heading is visible without scrolling
  past any older release notes.
- No error dialog is shown and no error is logged by
  `com.microsoft.copilot.eclipse.core` during the action.

#### 📸 Key Screenshots
- [ ] **Status-bar menu** — Copilot status-bar menu showing the "What's New" entry.
- [ ] **Rendered What's New page** — The opened editor with the latest release
  heading and first feature sub-heading visible at the top.
