# Copilot Pro Plan — Quota UI

## Overview
Pro accounts use the **Included Credits** row, have an Enable Additional Usage
overage row, and show Upgrade Plan based on the recorded `canUpgradePlan` value.

Expected plan label in the menu header: `Copilot Pro Plan`.

---

## Test Cases

### TC-Pro-000: Sign in with a Copilot Pro plan account, record `canUpgradePlan`

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Plugin is installed and Eclipse is signed out.
- A GitHub account on the Copilot Pro plan is available.
- Language-server logging is enabled and the Eclipse Copilot Language Servers Log console is open
  (see the README section *How to Read `canUpgradePlan` From the
  Language-Server Log*).

#### Steps
1. Open the Copilot status-bar menu and click `Sign in to GitHub`.
2. Complete the device-flow sign-in with the Pro plan account.
3. Wait for the status-bar icon to switch to the signed-in icon.
4. In the Eclipse Copilot Language Servers Log console, find the most recent `copilot/quotaChange`
   notification and **record the value of `canUpgradePlan`** (`true`, `false`,
   or absent). Later cases in this file refer to this value as
   "the recorded `canUpgradePlan` value".

#### Expected Result
- The header row of both menus reads `<username> — Copilot Pro Plan`.
- The recorded `canUpgradePlan` value has been written down so the rest of
  the cases can be evaluated against it.

#### 📸 Key Screenshots
- [ ] Signed-in state with the Pro Plan label
- [ ] Eclipse Copilot Language Servers Log console showing the `canUpgradePlan` field

---

### TC-Pro-001: Status-bar menu — under quota

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Premium / AI-credit usage is under 75% used.

#### Steps
1. Click the Copilot icon in the status bar.
2. Note the **Additional usage** status row text — it reflects whether the
   account currently has additional paid usage enabled.

#### Expected Result
- Header row: `Copilot Usage`, with a blank header icon (the usage icon
  appears on the Included Credits row instead).
- Row: `Included Credits   NN/MMM AI credits used` with the blue usage icon
  and a tooltip explaining included credits.
- Row showing the next reset date (e.g. `Reset in N days on <Month D, YYYY>`).
- Status row + action row **depend on the current additional-usage state**
  (the tester does not need to know this in advance — both layouts are valid
  starting points):
  - **Additional usage not enabled (default):**
    - Status row: `Additional usage not enabled` (no tooltip).
    - Action row: `Enable Additional Usage` with an upgrade icon.
  - **Additional usage enabled:**
    - Status row: `Additional usage enabled` (no tooltip).
    - Action row: `Increase Budget` with the same upgrade icon (clicking
      either label opens the overage management page).
- Row: `Upgrade Plan` is present **iff** the recorded `canUpgradePlan`
  value (TC-Pro-000) is `true`; absent otherwise. When present it uses a
  blank icon (the upgrade icon was already used by the row above).

#### 📸 Key Screenshots
- [ ] Status-bar menu (capture whichever additional-usage state the account
      starts in)

---

### TC-Pro-002: Menu-bar menu mirrors the status-bar menu

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Open `GitHub Copilot` from the Eclipse menu bar.
2. Compare against the menu shown in TC-Pro-001.

#### Expected Result
The menu-bar menu shows the same rows in the same order, with the same labels,
tooltips, and icons as the status-bar menu.

#### 📸 Key Screenshots
- [ ] Menu-bar menu

---

### TC-Pro-003: Usage icon transitions on the Included Credits row

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. On the quota portal, raise the AI-credit usage to above 75% (to trigger the yellow icon), then above 90% (to trigger the red icon).
2. Reopen menus after each change.

#### Expected Result
- The Included Credits row icon transitions from blue, to yellow when roughly
  a quarter or less remains, to red when roughly a tenth or less remains.
- The header icon stays blank throughout.

#### 📸 Key Screenshots
- [ ] Yellow Included Credits icon
- [ ] Red Included Credits icon

---

### TC-Pro-004: Quota-warning static banner — 75% threshold (info)

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- AI-credit usage is set to ~76% on the quota portal (so it crosses the 75%
  threshold but is below 90%). See *Quota-Warning Thresholds* in the README.

#### Steps
1. Send a chat message to trigger the quota-warning notification.

#### Expected Result
- A banner appears above the chat input with an **info icon** (not the
  warning icon).
- Two action links:
  - `Enable Additional Usage` (or `Increase Budget` if already enabled),
    opening the overage management page.
  - `Upgrade Plan`, opening the Copilot upgrade page — shown only when the
    recorded `canUpgradePlan` value (TC-Pro-000) is `true`. When the recorded
    value is `false` or absent, only the overage link is shown.
- The dismiss (`×`) control closes the banner.

#### 📸 Key Screenshots
- [ ] Static banner with info icon and both action links

---

### TC-Pro-005: Quota-warning static banner — 90% threshold (warning)

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- AI-credit usage is set to ~91% on the quota portal (so it crosses the 90%
  threshold). See *Quota-Warning Thresholds* in the README.

#### Steps
1. Send a chat message to trigger the quota-warning notification.

#### Expected Result
- A banner appears with a **warning icon**.
- Action links are the same as in TC-Pro-004:
  - `Enable Additional Usage` (or `Increase Budget` if already enabled).
  - `Upgrade Plan` — shown only when the recorded `canUpgradePlan` value
    (TC-Pro-000) is `true`.

#### 📸 Key Screenshots
- [ ] Static banner with warning icon and both action links

---

### TC-Pro-006: Inline warning under a chat turn when quota is exhausted

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- AI-credit usage is at 100%, with additional usage not enabled.

#### Steps
1. Send a chat message that the server rejects because the quota is exhausted.
2. Inspect the warning rendered under the assistant turn.

#### Expected Result
- The warning shows a warn icon and the server-supplied error message.
- A **primary** button `Enable Additional Usage` is shown and opens the
  overage management page.
- A **secondary** button `Upgrade Plan` is shown **iff** the recorded
  `canUpgradePlan` value (TC-Pro-000) is `true`; absent otherwise. When
  shown, it opens the Copilot upgrade page.
- The active model in the model picker is not silently switched away.

#### 📸 Key Screenshots
- [ ] Inline warning with two buttons

---

## Screenshots Checklist
- [ ] `TC-Pro-000` Signed-in Pro Plan label
- [ ] `TC-Pro-001` Status-bar menu (capture the current additional-usage state)
- [ ] `TC-Pro-002` Menu-bar menu
- [ ] `TC-Pro-003` Yellow / red Included Credits icon
- [ ] `TC-Pro-004` Static banner (info, 75%)
- [ ] `TC-Pro-005` Static banner (warning, 90%)
- [ ] `TC-Pro-006` Inline warning under chat turn
