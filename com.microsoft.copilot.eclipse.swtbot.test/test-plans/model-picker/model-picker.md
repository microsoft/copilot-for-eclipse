# Chat: Model Picker

## Overview

Tests the **model picker** dropdown in the Chat view action bar. The model
picker lets users choose which AI model powers their chat conversations. It
displays available models grouped by tier (Standard, Premium, Custom), shows
per-model details on hover, and includes a **Manage Models...** shortcut for
users with BYOK enabled.

Entry points exercised:
- **Ctrl+Alt+I** (or status bar → **Open Chat**), then interact with the model
  picker button in the action bar.

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- A signed-in Copilot account. Without a valid account the model list cannot
  be fetched and the picker will show no items.

---

## 1. Model picker loads, opens, selects, and routes a message

### TC-001: Open model picker, browse grouped models, switch model, and verify a response uses it

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions

- The Eclipse workbench is open.
- The user is signed in to Copilot.
- No previous Chat view is open.

#### Steps

1. Open the **Copilot Chat** view via the keyboard shortcut **Ctrl+Alt+I**
   (Windows/Linux) or **Ctrl+Cmd+I** (macOS), or click the Copilot status bar
   icon and select **Open Chat**.
2. Wait for the Chat view to fully load (the input area is editable, the model
   picker shows a resolved model name instead of "Loading...").
3. Verify the model picker button on the right side of the action bar displays
   a model name (e.g. `Claude Sonnet 4.6`) with a dropdown arrow.
4. Hover the mouse over the model picker button and verify a tooltip appears
   (e.g. `Pick Model`).
5. Click the model picker button to open the dropdown popup.
6. Verify the dropdown lists models grouped under labelled headers (e.g.
   **Standard Models**, **Premium Models**), with each model showing a cost
   multiplier on the right (e.g. `0x`, `1x`, `3x`).
7. Verify the currently selected model is marked with a **checkmark** (✓)
   in the list.
8. Hover over a model item in the dropdown and verify a **detail card**
   appears showing the model's **family** (e.g. `Family: claude-sonnet-4.5`),
   **cost** (e.g. `Cost: 1x premium`), and **category tag** (e.g.
   `Versatile`).
9. Click a model different from the currently selected one.
10. Verify the dropdown closes and the model picker button label updates to
    show the newly selected model name.
11. Type a prompt (e.g. `hello`) in the chat input and click **Send**.
12. Wait for the Copilot turn to complete (the model-info-label footer
    appears).
13. Verify the model info label at the bottom of the completed turn matches
    the model selected in step 9.

#### Expected Result

- The model picker loads and shows the default model.
- The dropdown opens with grouped models and cost multipliers; the selected
  model has a checkmark.
- Hovering a model shows a detail card with family, cost, and category.
- Selecting a different model updates the picker label immediately.
- The response is served by the newly selected model, confirmed by the turn
  footer.

#### 📸 Key Screenshots

- [ ] **Model picker loaded** — action bar showing the resolved model name.
- [ ] **Dropdown open** — popup showing grouped model list with the active
  model marked with a checkmark.
- [ ] **Model hover card** — detail card showing family, cost, and category
  tag for a model item.
- [ ] **Model switched** — picker button showing the new model name.
- [ ] **Response with new model** — completed Copilot turn whose footer shows
  the new model name.

---

## 2. Chat mode interaction

### TC-002: Switching chat mode updates the model picker

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions

- The Chat view is open.
- Both **Ask** and **Agent** modes are available in the mode picker.

#### Steps

1. Note the model currently shown in the model picker.
2. Switch the chat mode from **Agent** to **Ask** (or vice versa) using the
   mode picker on the left side of the action bar.
3. Observe the model picker button — it should still show a valid model name
   (which may differ from the previous one if the old model is not available
   in the new mode).
4. Open the model picker dropdown and verify the model list has loaded (it may
   contain different models than before).
5. Close the dropdown (click outside or press Escape).

#### Expected Result

- The model picker updates automatically when the chat mode changes.
- No crash, blank picker, or stale model name occurs.

#### 📸 Key Screenshots

- [ ] **After mode switch** — action bar showing the updated mode label and the
  model picker with a valid model name.

---

## 3. BYOK custom models and Manage Models shortcut

### TC-003: Custom models appear in the picker and "Manage Models..." opens the preference page

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions

- The user is signed in with an account that has BYOK enabled.
- At least one custom model has been added and enabled via **Window →
  Preferences → GitHub Copilot → Model Management**.

#### Steps

1. Open the model picker dropdown.
2. Verify a **Custom Models** group is present at the bottom of the list,
   containing the configured custom model(s).
3. Select a custom model and verify the picker label updates.
4. Open the model picker dropdown again.
5. Click the **Manage Models...** item at the bottom of the dropdown.
6. Verify the **Preferences** dialog opens directly on the **Model Management**
   page.
7. Close the Preferences dialog.

#### Expected Result

- Custom models are listed under a dedicated **Custom Models** group.
- Selecting a custom model works the same as selecting a built-in model.
- **Manage Models...** navigates directly to the BYOK preference page.

#### 📸 Key Screenshots

- [ ] **Custom Models group** — dropdown showing the Custom Models section with
  the user's configured models.
- [ ] **Manage Models... action** — Preferences dialog opened to the Model
  Management page after clicking the shortcut.

---

## Notes on failure modes

- Picker button stays blank / never shows a model name → the model list fetch
  failed; check that the account is signed in and network access is available.
- Dropdown is empty → auth is valid but the server returned no models; try
  signing out and back in to refresh the token.
- Selected model is not reflected in the completed turn's footer → the model
  switch event did not propagate; re-select the model and resend.
- Custom Models group does not appear → BYOK is disabled by org policy, or no
  custom model has been added; verify in Model Management preferences.
