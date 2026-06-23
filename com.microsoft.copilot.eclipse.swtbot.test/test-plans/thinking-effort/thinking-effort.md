# Thinking Effort Selection

## Overview

Tests the **Thinking Effort** feature requested in
[issue #204](https://github.com/microsoft/copilot-for-eclipse/issues/204).

Some models (e.g. Claude Sonnet) support multiple reasoning effort levels —
`low`, `medium`, `high`. The language server advertises available levels via
`capabilities.supports.reasoningEfforts` on each model. The plugin should allow
the user to select an effort level per model from the model picker, persist the
choice across sessions, reflect it in the model picker suffix, and pass it to the language server with each
conversation turn.

Entry points:
- **Chat view → model picker** → hover card for a reasoning-capable model.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and activated.
- A GitHub account signed in with an active Copilot subscription.
- At least one **reasoning-capable** model (e.g. Claude Sonnet) and one
  **non-reasoning** model (e.g. GPT-4o) available in the model picker.
- The reasoning-capable model advertises at least two effort levels
  (`low`, `medium`, `high`).

---

## TC-001: Model picker UI and effort selection end-to-end

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- No effort has been explicitly set for the reasoning model (fresh state or
  reset).

#### Steps
1. Open the Copilot Chat view and click the model picker to open the dropdown.
2. Locate a **reasoning-capable model** (e.g. Claude Sonnet). Read its suffix.
3. Hover over the model to open its hover card. Inspect the effort section.
4. Click **`High`** in the effort section.
5. Close the model picker and reopen it. Read the suffix again.
6. Hover over the same model again and check which effort is marked.

#### Expected Result
- In step 2, the suffix contains the default effort level — **`Medium`** if
  supported, otherwise the first advertised level.
- In step 3, the hover card shows a **"Thinking effort"** section listing all
  available levels; the current default is marked.
- After clicking `High` (step 4), the suffix updates to show **`High`** and
  the checkmark moves to `High` in the hover card.
- A **non-reasoning model** hovered in the same session shows no effort section.

#### 📸 Key Screenshots
- [ ] **Model picker list** — Reasoning model with default effort in suffix.
- [ ] **Hover card** — Effort section with `High` selected and checkmark.
- [ ] **Model picker list** — Updated suffix after selecting `High`.

---

## TC-002: Effort selection is persisted per model across sessions

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- Model A (reasoning) effort set to `high`, Model B (reasoning) effort set to
  `low` (from TC-001 or set manually).

#### Steps
1. Restart Eclipse.
2. Open the model picker and check the suffixes and hover cards of Model A and
   Model B.

#### Expected Result
- Model A still shows **`High`**; Model B still shows **`Low`**.
- Each model's effort is independent — changing one did not affect the other.

---

## TC-003: Thinking effort descriptions are fully readable in the hover card

**Type:** `Regression`
**Priority:** `P1`

Regression guard for
[issue #233](https://github.com/microsoft/copilot-for-eclipse/issues/233) /
[PR #234](https://github.com/microsoft/copilot-for-eclipse/pull/234). The hover
card previously clamped its width to a fixed maximum (`LONG_POPUP_WIDTH = 300`),
which truncated the longer per-level description text in the **Thinking effort**
section. The fix removes that cap so the hover shell uses its natural packed
width (only enforcing a `250` px minimum), letting every description wrap and
render in full.

#### Preconditions
- A reasoning-capable model (e.g. Claude Sonnet 4.6) advertising multiple
  effort levels, each with a descriptive sub-label (e.g.
  `Low — Faster responses, less reasoning`,
  `Medium — Balanced reasoning and speed`,
  `High — Deeper reasoning, slower responses`).

#### Steps
1. Open the Copilot Chat view and click the model picker to open the dropdown.
2. Hover over the reasoning-capable model to open its hover card.
3. Locate the **Thinking effort** section and read each effort level's
   description line in full.
4. Visually compare the right edge of the longest description against the hover card border.

#### Expected Result
- The hover card is wide enough to display the full description for every effort
  level — no text is clipped, cut off, or replaced with an ellipsis at the right
  edge.
- Each description ends with its natural last word (or wraps onto a second line)
  rather than being truncated mid-word against the card border.
- The hover card width is never narrower than the `250` px minimum, so short
  content still renders cleanly.

#### 📸 Key Screenshots
- [ ] **Hover card — full descriptions** — Thinking effort section showing each
  level's complete, untruncated description text.
- [ ] **Right-edge close-up** — the longest description's final word fully
  visible inside the card border (no clipping/ellipsis).

