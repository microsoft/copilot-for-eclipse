# Copilot Pro+ Plan — Quota UI

## Overview
Pro+ accounts use the **Included Credits** row, have an Enable Additional
Usage overage row, and may show the `Upgrade Plan` row depending on the
upgrade-eligibility signal sent by the language server.

Expected plan label in the menu header: `Copilot Pro+ Plan`.

---

## Test Cases

### TC-ProPlus-000: Sign in with a Copilot Pro+ plan account, record `canUpgradePlan`

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Plugin is installed and Eclipse is signed out.
- A GitHub account on the Copilot Pro+ plan is available.
- Language-server logging is enabled and the Eclipse Copilot Language Servers Log console is open
  (see the README section *How to Read `canUpgradePlan` From the
  Language-Server Log*).

#### Steps
1. Open the Copilot status-bar menu and click `Sign in to GitHub`.
2. Complete the device-flow sign-in with the Pro+ plan account.
3. Wait for the status-bar icon to switch to the signed-in icon.
4. In the Eclipse Copilot Language Servers Log console, find the most recent `copilot/quotaChange`
   notification and **record the value of `canUpgradePlan`** (`true`, `false`,
   or absent). Later cases in this file refer to this value as
   "the recorded `canUpgradePlan` value".

#### Expected Result
- The header row of both menus reads `<username> — Copilot Pro+ Plan`.
- The recorded `canUpgradePlan` value has been written down so the rest of
  the cases can be evaluated against it.

#### 📸 Key Screenshots
- [ ] Signed-in state with the Pro+ Plan label
- [ ] Eclipse Copilot Language Servers Log console showing the `canUpgradePlan` field

---

### TC-ProPlus-001: Status-bar menu — under quota

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- AI-credit usage is under 75% used.

#### Steps
1. Click the Copilot icon in the status bar.
2. Note the **Additional usage** status row text — it reflects whether the
   account currently has additional paid usage enabled.

#### Expected Result
Rows (top → bottom):
1. `Copilot Usage` header with a blank header icon.
2. `Included Credits   NN/MMM AI credits used` with the blue usage icon and a
   tooltip explaining included credits.
3. Row showing the next reset date.
4. Status row + action row **depend on the current additional-usage state**
   (the tester does not need to know this in advance — both layouts are
   valid starting points):
   - **Additional usage not enabled (default):**
     - Status row: `Additional usage not enabled`.
     - Action row: `Enable Additional Usage` with an upgrade icon.
   - **Additional usage enabled:**
     - Status row: `Additional usage enabled`.
     - Action row: `Increase Budget` with the same upgrade icon (clicking
       either label opens the overage management page).
5. `Upgrade Plan` with a blank icon — present **iff** the recorded
   `canUpgradePlan` value (TC-ProPlus-000) is `true`; absent otherwise.

#### 📸 Key Screenshots
- [ ] Status-bar menu (capture whichever additional-usage state the account
      starts in)

---

### TC-ProPlus-002: Menu-bar menu mirrors the status-bar menu

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Open `GitHub Copilot` from the Eclipse menu bar.
2. Compare against the menu shown in TC-ProPlus-001.

#### Expected Result
The menu-bar menu shows the same rows in the same order, with the same labels,
tooltips, and icons as the status-bar menu.

#### 📸 Key Screenshots
- [ ] Menu-bar menu

---

### TC-ProPlus-003: Usage icon transitions on the Included Credits row

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. On the quota portal, raise AI-credit usage to above 75% (to trigger the yellow icon), then above 90% (to trigger the red icon).

#### Expected Result
- Blue → yellow (≈25% or less remaining) → red (≈10% or less remaining) on
  the Included Credits row.
- The header icon stays blank throughout.

#### 📸 Key Screenshots
- [ ] Yellow / red Included Credits icon

---

### TC-ProPlus-004: Quota-warning static banner — 75% threshold (info)

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- AI-credit usage is set to ~76% on the quota portal (so it crosses the 75%
  threshold but is below 90%). See *Quota-Warning Thresholds* in the README.
- Additional usage is disabled.

#### Expected Result
- Banner above the chat input with an **info icon** (not the warning icon).
- Action links:
  - `Enable Additional Usage` — always shown.
  - `Upgrade Plan` — shown **iff** the recorded `canUpgradePlan` value
    (TC-ProPlus-000) is `true`.

#### 📸 Key Screenshots
- [ ] Static banner with info icon and both action links

---

### TC-ProPlus-005: Quota-warning static banner — 90% threshold (warning)

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- AI-credit usage is set to ~91% on the quota portal (so it crosses the 90%
  threshold). See *Quota-Warning Thresholds* in the README.
- Additional usage is disabled.

#### Expected Result
- Banner above the chat input with a **warning icon**.
- Action links are the same as in TC-ProPlus-004:
  - `Enable Additional Usage` — always shown.
  - `Upgrade Plan` — shown **iff** the recorded `canUpgradePlan` value
    (TC-ProPlus-000) is `true`.

#### 📸 Key Screenshots
- [ ] Static banner with warning icon and both action links

---

### TC-ProPlus-006: Inline warning under a chat turn when quota is exhausted

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- AI-credit usage is at 100%, additional usage not enabled.

#### Steps
1. Send a chat message that the server rejects because the quota is exhausted.

#### Expected Result
- Warn icon plus the server-supplied error message.
- A **primary** button `Enable Additional Usage` is shown and opens the
  overage management page.
- A **secondary** button `Upgrade Plan` is shown **iff** the recorded
  `canUpgradePlan` value (TC-ProPlus-000) is `true`; absent otherwise.

#### 📸 Key Screenshots
- [ ] Inline warning with two buttons

---

## Screenshots Checklist
- [ ] `TC-ProPlus-000` Signed-in Pro+ Plan label
- [ ] `TC-ProPlus-001` Status-bar menu (capture the current additional-usage state)
- [ ] `TC-ProPlus-002` Menu-bar menu
- [ ] `TC-ProPlus-003` Yellow / red Included Credits icon
- [ ] `TC-ProPlus-004` Static banner (info, 75%)
- [ ] `TC-ProPlus-005` Static banner (warning, 90%)
- [ ] `TC-ProPlus-006` Inline warning under chat turn
