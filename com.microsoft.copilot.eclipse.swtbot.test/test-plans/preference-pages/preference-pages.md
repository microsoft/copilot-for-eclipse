# GitHub Copilot Preference Pages

## Overview
Covers behavior shared by **all** GitHub Copilot preference pages
(**Preferences → GitHub Copilot → …**), independent of any single feature.
Every Copilot page is hosted in the same JFace `PreferenceDialog`, so concerns
like dialog sizing, layout stability, and navigation are cross-cutting.

JFace grows the shared dialog to the **tallest** page visited and never shrinks
it again. To keep the dialog a stable size, each Copilot page keeps its content
within a common target height — `PreferencePageUtils.STANDARD_CONTENT_HEIGHT`.
Pages whose natural content is taller (e.g. **Tool Auto Approve**, which stacks
four rule sections) scroll within a height-capped `ScrolledComposite` instead of
ballooning the dialog. The same constant drives `McpPreferencePage`'s two groups,
so the pages settle at a consistent size.

Entry points exercised:
- **Preferences → GitHub Copilot** — the root category page (only hyperlinks; no
  sign-in, always valid). A stable baseline for size comparisons.
- **Preferences → GitHub Copilot → Tool Auto Approve** — the tallest page; the
  regression magnet for dialog ballooning.
- Other child pages (MCP, Completions, Chat, …) — for navigation stability.

---

## Prerequisites
- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
- No sign-in required: the pages below render independently of language-server
  data.

---

## 1. Dialog size stability

### TC-001: Opening a tall page does not balloon the Preferences dialog

**Type:** `Regression`
**Priority:** `P1`

#### Steps
1. Open **Window → Preferences** and select **GitHub Copilot** (root page). Note
   the dialog's size.
2. In the tree, select **GitHub Copilot → Tool Auto Approve**.
3. Observe the dialog does **not** grow toward screen height — it stays close to
   the root page's size.
4. Confirm all four sections (Terminal, File Operation, MCP, Global) are
   reachable by scrolling **within the page** using a single page-level vertical
   scrollbar.
5. Navigate to a couple of sibling pages (e.g. **MCP**, **Completions**) and back
   to Tool Auto Approve; the dialog size stays stable throughout.
6. Close Preferences.

#### Expected Result
- The Preferences dialog stays a normal, stable size as the selection moves
  between Copilot pages; Tool Auto Approve does not balloon toward screen height.
- Tool Auto Approve content is reachable via the page's own single vertical
  scrollbar; no second (dialog-level) scrollbar appears.

#### Reference measurements (Eclipse 2025-03, macOS)
- Root **GitHub Copilot** page dialog height: **~551px**.
- **Tool Auto Approve** dialog height (fixed by the height cap): **~656px** —
  deterministic and screen-independent.
- Pre-fix, Tool Auto Approve ballooned to ~screen height (≈986px or
  screen-clamped).

#### 📸 Key Screenshots
- [ ] Dialog on the root **GitHub Copilot** page.
- [ ] Dialog on **Tool Auto Approve** — only slightly taller, content scrolling
      within the page (single page scrollbar).

---

## Notes
- Page height is **data-independent** (table/tree height hints are fixed), so MCP
  servers loading asynchronously does not change the page height.
- The height cap is unified via `PreferencePageUtils.STANDARD_CONTENT_HEIGHT`,
  shared by `AutoApprovePreferencePage` (scroller cap) and `McpPreferencePage`
  (two stacked groups). Keep the pages within this height when adding content.
- Use the root **GitHub Copilot** page as the size baseline — it is always valid
  and needs no sign-in, whereas the MCP page can enter an invalid state in an
  unauthenticated sandbox and pop a JFace validation dialog.
