# BYOK (Bring Your Own Key) Model Management

## Overview
Tests the **Model Management** preference page (`ByokPreferencePage`), which lets
users register their own AI model providers and models alongside the GitHub
Copilot defaults. The page is the user-facing surface for the BYOK flow:
storing per-provider API keys, fetching available models, adding custom
deployments, and toggling which models are exposed to the chat model picker.

The page renders three distinct states driven by auth + feature-flag status:

1. **Signed-out** — a sign-in prompt label is shown.
2. **Signed-in but BYOK disabled by org policy** — a "disabled by your
   organization's GitHub settings" tip is shown.
3. **Signed-in + BYOK enabled** — the full provider tree, action buttons, and
   loading overlay.

Entry points exercised:
- **Window → Preferences → GitHub Copilot → Model Management**.
- Equivalently, the probe's `invokeCommand` of
  `com.microsoft.copilot.eclipse.commands.openPreferences` with parameter
  `com.microsoft.copilot.eclipse.commands.openPreferences.activePageId =
  com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage`.

Providers covered (`ByokModelProvider`): `Azure`, `OpenAI`, `Gemini`, `Groq`,
`OpenRouter`, `Anthropic`. Azure is special-cased: it has no top-level API
key, so the **Change API…** / **Delete API…** buttons stay disabled for it
even when models are configured.

Not exercised in this plan (separate scenarios):
- Actually issuing chat completions through a registered BYOK model — that's
  covered by the chat-send-receive plan once a BYOK model is selected in the
  picker.
- Network-failure paths against real provider endpoints (out of scope for a
  workbench probe).

---

## Prerequisites

- Eclipse IDE with the GitHub Copilot for Eclipse plugin installed and
  activated.
- **A signed-in Copilot account on the host machine** for every TC except
  TC-010 (which deliberately probes the signed-out empty state). The Copilot
  JS agent reads its token from the OS-standard Copilot store.
- The signed-in account must have BYOK enabled by GitHub policy. If the org
  disables custom models, only TC-011 (disabled tip) is observable; the rest
  will short-circuit to the disabled view.
- Valid API keys for at least one non-Azure provider (e.g. OpenAI) for the
  add-key / add-model / change-key / delete-key TCs. Use a throwaway key
  scoped to a sandbox account — the probe persists the key into the Copilot
  language server's secure store as part of the TC.
- For Azure-specific TCs: a deployment URL and API key for an Azure OpenAI
  deployment (or skip the Azure cases).
- No previously opened Preferences dialog. The probe runner pre-suppresses
  Quick Start, What's New, Welcome, and "Terminal Support Unavailable"
  pop-ups — keep that contract when authoring follow-up plans.
- Each TC starts from a freshly launched workbench (the probe sandbox
  satisfies this automatically). When running manually, close the
  Preferences dialog between cases so the page re-runs its `init` /
  `refreshPageData` lifecycle.

---

## 1. Page lifecycle

### TC-001: Open Model Management page and view providers

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Eclipse workbench is open.
- The user is signed in to Copilot with BYOK enabled by org policy.

#### Steps
1. Wait for the workbench to settle (`waitForIdle`).
2. Open the Preferences dialog and navigate to the BYOK page (`Window →
   Preferences → GitHub Copilot → Model Management`, or the probe's
   `invokeCommand` with `activePageId =
   com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage`).
3. Wait for the loading overlay to finish (the "Loading..." label is
   replaced by the provider tree).
4. Verify the page header shows the title **Model Management** and the
   description **Configure your own custom models.**
5. Verify the **Provider** group is visible with the description
   **Select a provider before adding models.**
6. Verify the tree has two columns — **Custom Models** and **Status** —
   and contains exactly the six providers `Azure`, `OpenAI`, `Gemini`,
   `Groq`, `OpenRouter`, `Anthropic`.
7. Verify the action buttons are present on the right side:
   **Add Model...**, **Remove Model**, **Enable** / **Disable**, **Reload**,
   **Change API...**, **Delete API...**. With no selection, **Add Model...**,
   **Remove Model**, **Enable**, **Change API...**, and **Delete API...** are
   disabled; only **Reload** is enabled.

#### Expected Result
- The page opens without an error dialog.
- All six providers render as collapsible tree nodes.
- Button enablement matches the no-selection state described above.
- `workspace.log` contains no `ERROR` entries from
  `com.microsoft.copilot.eclipse.ui.preferences.ByokPreferencePage` or
  `com.microsoft.copilot.eclipse.ui.chat.services.ByokService` during the
  open.

#### 📸 Key Screenshots
- [ ] **Loading state** — overlay shown immediately after the page opens.
- [ ] **Loaded state** — provider tree visible with the six providers and
  the action buttons on the right.

#### Notes on failure modes
- Page never leaves loading → `ByokService.refreshData()` failed; check
  `workspace.log` for the LS bind error.
- Sign-in label or org-disabled tip is shown instead of the tree → the
  preconditions don't match; fall through to TC-010 / TC-011.

---

## 2. API key management (non-Azure provider)

### TC-002: Add an API key for a provider with no key configured

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-001 preconditions hold.
- The signed-in account has **no** API key stored for the target provider
  (e.g. OpenAI). Run TC-007 first against this provider to guarantee the
  state, or use a fresh sandbox account.
- A valid OpenAI (or equivalent non-Azure) API key is available to the
  tester.

#### Steps
1. Open the BYOK page (steps 1–3 of TC-001).
2. Select the **OpenAI** provider node in the tree.
3. Verify **Add Model…** becomes enabled and **Change API…** /
   **Delete API…** stay disabled (no key registered yet).
4. Click **Add Model…**.
5. Verify the API-key dialog opens with shell title **Add OpenAI Models**
   (formatted from `preferences_page_byok_addModel_dialog_title`). The
   dialog body contains a single password-masked **API Key** text field
   with an eye-icon toggle button to reveal/mask the value, and the
   button bar shows the platform-default **OK** and **Cancel** buttons.
   **OK** is enabled on open (the dialog defers validation until the
   button is pressed).
6. Verify that pressing **OK** while the field is empty does **not**
   close the dialog: focus returns to the API Key field and no save is
   dispatched. (Equivalent of the empty-input guard in `okPressed`.)
7. Type a valid OpenAI API key into the field and click **OK**.
8. Wait for the per-provider loading indicator to clear (the "(Loading...)"
   suffix on the OpenAI node disappears).
9. Expand the OpenAI node.
10. Verify a list of models is fetched from the provider and rendered as
    children, each with a **Status** column entry of either **Enable** or
    **Disable** (matching their default registration state) and a status
    icon.
11. Verify the OpenAI node label now includes the registered/total count
    suffix, e.g. `OpenAI ( 0 of N Enabled )`.
12. Re-select the OpenAI provider node and verify **Change API…** and
    **Delete API…** are now enabled.

#### Expected Result
- The API key is persisted (subsequent reopens of the Preferences dialog
  show the OpenAI node already populated and the API-key buttons enabled).
- No error dialog is shown and no `ByokService` errors are logged.

#### 📸 Key Screenshots
- [ ] **Empty OpenAI node** — selected before clicking Add Model.
- [ ] **API key dialog** — shell title **Add OpenAI Models**, masked
  API Key field with eye toggle, **OK** / **Cancel** buttons.
- [ ] **OpenAI populated** — node expanded with models + count suffix.

---

### TC-003: Change the API key for a provider that already has one

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- TC-002 has succeeded (OpenAI has a stored API key and at least one
  fetched model).
- A second valid OpenAI API key is available.

#### Steps
1. Open the BYOK page and select the **OpenAI** provider node.
2. Click **Change API…**.
3. Verify a confirmation dialog appears with title **Change OpenAI API
   Key?** and warning text about the change potentially breaking models.
   Click **Yes**.
4. Verify the API-key dialog reopens. In the change flow its shell title
   is just the provider name (**OpenAI**, not the `Add %s Models`
   formatted title used for the add flow), and the masked API Key field
   is pre-populated with the current key. **OK** / **Cancel** are the
   button-bar labels; **OK** is enabled on open.
5. Replace the field with the new API key and click **OK**.
6. Wait for the per-provider loading indicator to clear.
7. Verify the OpenAI node still shows its model list (refreshed against
   the new key) and no error dialog is shown.

#### Expected Result
- The new key replaces the previous one in the secure store.
- The provider's model list is reloaded with the new credentials.

#### 📸 Key Screenshots
- [ ] **Change API confirmation dialog**.
- [ ] **API key dialog (change flow)** — shell title is just **OpenAI**,
  field pre-populated with the masked existing key.

---

### TC-004: Delete an API key (cascades to associated custom models)

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- TC-002 has succeeded for the chosen provider, and at least one custom
  model has been added through TC-005.

#### Steps
1. Open the BYOK page and select the **OpenAI** provider node.
2. Click **Delete API…**.
3. Verify a confirmation dialog appears with title **Delete OpenAI API
   Key?** and body warning that removing the key permanently deletes
   associated models. Click **Delete**.
4. Wait for the per-provider loading indicator to clear.
5. Verify the OpenAI node returns to the empty state — no children, no
   `(N of N Enabled)` count suffix.
6. Re-select the OpenAI node and verify **Change API…** / **Delete API…**
   are disabled again.

#### Expected Result
- The API key and any custom models attached to it are removed.
- Default (non-custom) models for the provider are also gone, since the
  provider has no credentials to fetch them.
- No error dialog is shown.

#### 📸 Key Screenshots
- [ ] **Delete API confirmation dialog**.
- [ ] **Empty OpenAI node after delete**.

---

### TC-005: Azure provider does not expose Change/Delete API actions

**Type:** `Negative / Edge Case`
**Priority:** `P1`

#### Preconditions
- TC-001 preconditions hold.
- No requirement for Azure deployments to be registered.

#### Steps
1. Open the BYOK page and select the **Azure** provider node.
2. Verify **Change API…** and **Delete API…** stay **disabled** even
   though Azure is selected (Azure has per-deployment credentials, not a
   provider-level key).
3. Verify **Add Model…** is **enabled** for Azure.
4. Click **Add Model…** and verify the dialog opened is the
   **Add Azure Models** dialog (model id, display name, deployment URL,
   API key, vision and tool-calling checkboxes) — **not** the simpler
   Add API Key dialog used by other providers.
5. Cancel the dialog.

#### Expected Result
- Azure is treated as a per-deployment provider: no provider-wide key,
  no Change/Delete API affordance, and the Add Model dialog includes
  deployment URL and per-model API key fields.

#### 📸 Key Screenshots
- [ ] **Azure selected** — Change/Delete API buttons greyed out.
- [ ] **Add Azure Models dialog** with deployment-URL and API-key fields.

---

## 3. Custom model management

### TC-006: Add a custom model under a provider with an API key

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-002 has succeeded for OpenAI (API key registered).

#### Steps
1. Open the BYOK page, select the **OpenAI** provider node, and click
   **Add Model…**.
2. Verify the **Add OpenAI Models** dialog opens with fields **Model
   ID** (required), **Display Name** (optional), **Support Vision**
   checkbox, and **Support Tool Calling** checkbox. There is no
   deployment-URL or API-key field for OpenAI (those are Azure-only).
   The **Add** button stays disabled until **Model ID** is non-blank;
   leaving **Display Name** empty does **not** block the **Add** button
   (validation only requires Model ID for non-Azure providers, and
   additionally Deployment URL + API Key for Azure).
3. Enter a model id (e.g. `gpt-4o-mini-test`), a display name (e.g.
   `My Custom Model`), leave the capability checkboxes at their defaults,
   and click **Add**.
4. Wait for the per-provider loading indicator to clear.
5. Expand the OpenAI node and verify the new model appears in the tree
   with the chosen display name (no `(Default)` suffix — that is reserved
   for fetched non-custom models). Its **Status** is **Enable** by
   default.
6. Select the new custom model and verify **Remove Model** is **enabled**
   for it (custom models can be removed).
7. Select any non-custom model in the same provider and verify **Remove
   Model** is **disabled** (default models cannot be removed).
8. Re-open **Add Model…**, enter only a model id (e.g.
   `gpt-4o-mini-blank-name`), leave **Display Name** empty, and click
   **Add**. Verify the model is created and rendered in the tree using
   the **model id** as its display name (the dialog falls back to the
   trimmed model id when display name is blank — see
   `AddByokModelDialog#buildModel`).

#### Expected Result
- The custom model is persisted, appears in the tree, and is selectable
  from the chat model picker after this point.
- The OpenAI node count suffix updates to reflect the new model count.
- When **Display Name** is left blank, the saved model's displayed name
  equals the trimmed **Model ID**.

#### 📸 Key Screenshots
- [ ] **Add OpenAI Models dialog** with required fields filled.
- [ ] **Custom model in tree** showing the new display name.
- [ ] **Custom model in tree (blank display name)** — node label equals
  the model id.

---

### TC-007: Toggle a model between Enable and Disable

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- At least one model is registered under any provider (custom or default).

#### Steps
1. Open the BYOK page and expand a provider with at least one model.
2. Select a model whose **Status** is **Enable**.
3. Verify the right-hand toggle button reads **Disable**.
4. Click the toggle button.
5. Verify the model's **Status** column flips to **Disable** with the
   matching disabled icon, and the toggle button text now reads
   **Enable**.
6. Click the toggle button again to flip the model back to **Enable**;
   verify both the column and the button text revert.
7. Repeat with a model whose initial status is **Disable** to confirm the
   inverse path.

#### Expected Result
- Status flips on each toggle and persists across re-opening the
  Preferences dialog.
- The provider's `(N of M Enabled)` count suffix updates to reflect the
  new enabled count.

#### 📸 Key Screenshots
- [ ] **Model enabled** — green keep icon and "Enable" status.
- [ ] **Model disabled** — delete icon and "Disable" status, button now
  reads "Enable".

---

### TC-008: Remove a custom model

**Type:** `Happy Path`
**Priority:** `P1`

#### Preconditions
- A custom model exists under some provider (e.g. created in TC-006).

#### Steps
1. Open the BYOK page, expand the provider, and select the custom model.
2. Verify **Remove Model** is enabled.
3. Click **Remove Model**.
4. Verify a confirmation dialog appears with title **Remove Model** and
   body **Do you want to remove this model?**, with **Remove** and
   **Cancel** buttons.
5. Click **Remove**.
6. Verify the model is removed from the tree and the provider's count
   suffix decreases by one.
7. Repeat steps 1–4, but click **Cancel** instead. Verify the model is
   still present (cancel does not delete).

#### Expected Result
- The custom model is removed from the secure store and the tree.
- Cancelling the confirmation dialog leaves state unchanged.

#### 📸 Key Screenshots
- [ ] **Remove Model confirmation dialog**.
- [ ] **Tree after removal** — model gone, count suffix decremented.

---

## 4. Reload

### TC-009: Reload a single provider vs. all providers

**Type:** `Happy Path`
**Priority:** `P2`

#### Preconditions
- TC-002 has succeeded for at least one non-Azure provider (so reload has
  something to fetch).

#### Steps
1. Open the BYOK page.
2. Select the **OpenAI** provider node and click **Reload**. Verify the
   OpenAI node briefly shows a `(Loading...)` suffix and that **only**
   OpenAI flips to loading — sibling provider nodes keep their normal
   labels.
3. Wait for the per-provider loading indicator to clear and verify the
   OpenAI model list is still rendered (refreshed).
4. Click on empty space inside the tree to clear the selection, then
   click **Reload**.
5. Verify the **page-level** loading overlay is shown (the entire tree is
   replaced with the centered "Loading..." label) and all action buttons
   are disabled.
6. Wait for the overlay to clear and verify every provider with a key
   shows its refreshed model list.

#### Expected Result
- Per-provider reload affects only the selected provider's row.
- Reload-all triggers the page-level overlay and disables all buttons
  while in flight.
- No error dialogs are shown for providers with valid keys.

#### 📸 Key Screenshots
- [ ] **Per-provider loading** — OpenAI shows `(Loading...)` while
  siblings render normally.
- [ ] **Page-level overlay** — full "Loading..." view during reload-all.

---

## 5. Conditional page states

### TC-010: Signed-out empty state

**Type:** `Negative / Edge Case`
**Priority:** `P1`

#### Preconditions
- The user is **not** signed in to Copilot on the host machine. Sign out
  via the Copilot status-bar menu, or run on a host with no
  `apps.json` token, before launching the workbench.

#### Steps
1. Open the BYOK page (`Window → Preferences → GitHub Copilot → Model
   Management`).
2. Verify the page contents are replaced by a single sign-in description
   label whose text is **Sign in to GitHub to access custom models.** —
   the provider tree, the loading overlay, and all action buttons are
   absent.

#### Expected Result
- The page renders the signed-out empty state and no LS calls are made
  (no `ByokService.refreshData()` activity in `workspace.log`).

#### 📸 Key Screenshots
- [ ] **Signed-out state** — sign-in description visible, no provider
  tree.

---

### TC-011: BYOK disabled by org policy / feature flag

**Type:** `Negative / Edge Case`
**Priority:** `P1`

#### Preconditions
- The user is signed in to Copilot, but the account's org has BYOK
  disabled (`FeatureFlags.isByokEnabled()` returns `false`). Reproduce
  by signing in with an org-managed account that has the policy off, or
  by overriding the feature flag in a test build.

#### Steps
1. Open the BYOK page.
2. Verify the page shows the **disabled tip** view: an information icon
   and the text **Custom models are disabled by your organization's
   GitHub settings. Please contact your organization's administrator for
   more information.** — the provider tree and action buttons are
   absent.

#### Expected Result
- The page renders the org-disabled state and no models are fetched.

#### 📸 Key Screenshots
- [ ] **Org-disabled state** — info icon and disabled-tip label.

---

## 6. Selection-driven button enablement

### TC-012: Action button enablement matches selection

**Type:** `Happy Path`
**Priority:** `P2`

#### Preconditions
- TC-001 preconditions hold.
- TC-002 has succeeded for OpenAI.
- TC-006 has succeeded so that OpenAI has at least one custom model and
  one default (fetched, non-custom) model.

#### Steps
1. Open the BYOK page, expand OpenAI, and clear any selection by clicking
   inside the tree's empty area.
2. Verify only **Reload** is enabled.
3. Select the **OpenAI** provider node (no model selection). Verify:
   **Add Model…** enabled; **Remove Model**, **Enable** disabled (no
   model selected); **Change API…**, **Delete API…** enabled (non-Azure
   provider with a stored key); **Reload** enabled.
4. Select the **Azure** provider node. Verify the same as step 3 but with
   **Change API…** and **Delete API…** disabled (Azure exception).
5. Select a **default** (non-custom) model under OpenAI. Verify
   **Remove Model** is disabled, **Enable**/**Disable** toggle is
   enabled, **Add Model…** enabled, API-key buttons enabled.
6. Select a **custom** model under OpenAI. Verify **Remove Model** is
   enabled, **Enable**/**Disable** toggle is enabled, the rest as in
   step 5.

#### Expected Result
- Button enablement strictly follows the rules in
  `ByokPreferencePage#refreshButtonsEnabled`.

#### 📸 Key Screenshots
- [ ] **No selection** — only Reload enabled.
- [ ] **Custom model selected** — Remove Model and toggle enabled.
- [ ] **Azure node selected** — API-key buttons disabled.

---

## Cross-cutting failure modes

If multiple TCs fail in similar ways, suspect a single root cause:

- **Page never leaves the loading overlay** → `ByokService.refreshData()`
  rejected. Inspect `workspace.log` for the language-server bind error and
  confirm the LS process started (`!ENTRY com.microsoft.copilot.eclipse.core`
  with no fatal stack).
- **Add API Key / Add Model dialogs fail to open** → the Preferences dialog
  may have closed before the click landed. `invokeCommand` for
  `org.eclipse.ui.window.preferences` is `asyncExec`-dispatched; follow it
  with `waitForIdle` and a short `sleep` before the next step.
- **`(Loading...)` suffix never clears for one provider** → that provider's
  `reloadProvider` future failed; check `handleError` toasts in screenshots
  and `workspace.log` for the upstream HTTP failure.
- **Buttons enablement looks wrong even when state is correct** → the
  selection listener may have raced with a `setInput` refresh; reproduce by
  opening Preferences twice in quick succession and capture a `dumpUi` of
  the page during the failure.
