# Copilot Free Plan — Quota UI

## Overview
Free plan accounts track separate **Code Completions** and **Chat Messages**
quotas and have no overage row. Upgrade Plan visibility depends on the
 language-server `canUpgradePlan` signal.

Expected plan label in the menu header: `Copilot Free Plan`.

---

## Test Cases

### TC-Free-000: Sign in with a Copilot Free plan account, record `canUpgradePlan`

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Plugin is installed and Eclipse is signed out.
- A GitHub account on the Copilot Free plan is available.
- Language-server logging is enabled and the Eclipse Copilot Language Servers Log console is open
  (see the README section *How to Read `canUpgradePlan` From the
  Language-Server Log*).

#### Steps
1. Open the Copilot status-bar menu and click `Sign in to GitHub`.
2. Complete the device-flow sign-in with the Free plan account.
3. Wait for the status-bar icon to switch to the signed-in icon.
4. In the Eclipse Copilot Language Servers Log console, find the most recent `copilot/quotaChange`
   notification and **record the value of `canUpgradePlan`** (`true`, `false`,
   or absent). Later cases in this file refer to this value as
   "the recorded `canUpgradePlan` value".

#### Expected Result
- The header row of both menus reads `<username> — Copilot Free Plan`.
- The recorded `canUpgradePlan` value has been written down so the rest of
  the cases can be evaluated against it.

#### 📸 Key Screenshots
- [ ] Signed-in state with the Free Plan label
- [ ] Eclipse Copilot Language Servers Log console showing the `canUpgradePlan` field

---

### TC-Free-001: Status-bar menu — under quota

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Both the Code Completions and Chat Messages quotas are under 75% used.

#### Steps
1. Click the Copilot icon in the status bar.

#### Expected Result
- Header row: `Copilot Usage` with the blue usage icon and the tooltip
  `Manage Copilot`.
- Row: `Code Completions    NN% used`.
- Row: `Chat Messages       NN% used`.
- Row showing the next reset date (e.g. `Reset in N days on <Month D, YYYY>`),
  disabled.
- The `Upgrade Plan` row (with an upgrade icon) is present **iff** the
  recorded `canUpgradePlan` value (TC-Free-000) is `true`; absent otherwise.
- No Included Credits / Monthly Limit row, no Enable Additional Usage row, no
  Additional usage status row.

#### 📸 Key Screenshots
- [ ] Full status-bar menu

---

### TC-Free-002: Menu-bar menu mirrors the status-bar menu

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Open `GitHub Copilot` from the Eclipse menu bar.
2. Compare against the menu shown in TC-Free-001.

#### Expected Result
The menu-bar menu shows the same rows in the same order, with the same labels,
tooltips, and icons as the status-bar menu.

#### 📸 Key Screenshots
- [ ] Full menu-bar menu

---

### TC-Free-003: Usage icon transitions

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. On the GitHub quota portal, raise either the Code Completions or Chat
   Messages usage to above 75% (to trigger the yellow icon); reopen the status-bar menu.
2. Raise the usage to above 90% (to trigger the red icon); reopen the menu.

#### Expected Result
- The header usage icon switches to **yellow** when the lower of the two
  quotas has roughly a quarter or less remaining.
- The header usage icon switches to **red** when the lower of the two quotas
  has roughly a tenth or less remaining.
- Percent text on each row updates without reopening Eclipse.

#### 📸 Key Screenshots
- [ ] Yellow header icon
- [ ] Red header icon

---

### TC-Free-004: Quota-warning static banner — 75% threshold (info)

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Chat Messages quota is set to ~76% used on the quota portal (so it
  crosses the 75% threshold but is below 90%). The Code Completions quota
  does not trigger a chat-view banner and is not relevant here. See
  *Quota-Warning Thresholds* in the README.

#### Steps
1. Send a chat message (or wait for the next quota refresh).
2. Observe the banner above the chat input area.

#### Expected Result
- A banner appears with an **info icon** (not the warning icon).
- If the recorded `canUpgradePlan` value (TC-Free-000) is `true`, a single
  action link `Upgrade Plan` is shown and opens the Copilot upgrade page in
  a browser when clicked. If the value is `false` or absent, no action link
  is shown.
- The dismiss (`×`) control closes the banner.

#### 📸 Key Screenshots
- [ ] Static banner with info icon

---

### TC-Free-005: Quota-warning static banner — 90% threshold (warning)

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Chat Messages quota is set to ~91% used on the quota portal (so it
  crosses the 90% threshold). The Code Completions quota does not trigger a
  chat-view banner and is not relevant here. See *Quota-Warning Thresholds*
  in the README.

#### Steps
1. Send a chat message (or wait for the next quota refresh).
2. Observe the banner above the chat input area.

#### Expected Result
- A banner appears with a **warning icon**.
- The message references the user having nearly exhausted the chat budget
  and prompts them to upgrade.
- The same conditional `Upgrade Plan` action link is shown only when the
  recorded `canUpgradePlan` value (TC-Free-000) is `true`.
- The dismiss (`×`) control closes the banner.

#### 📸 Key Screenshots
- [ ] Static banner with warning icon and Upgrade Plan link

---

### TC-Free-006: Inline warning under a chat turn when quota is exhausted

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- The Chat Messages quota is at 100% used. (Exhausting the Code Completions
  quota does not produce a chat-view warning.)

#### Steps
1. Send a chat message that the server rejects because the quota is exhausted.
2. Inspect the warning rendered under the assistant turn.

#### Expected Result
- The warning shows a warn icon and the server-supplied error message text.
- If the recorded `canUpgradePlan` value (TC-Free-000) is `true`, one primary
  button labelled `Upgrade Plan` is shown and opens the Copilot upgrade page
  in a browser when clicked. If the value is `false` or absent, no button is
  shown.

#### 📸 Key Screenshots
- [ ] Inline warning with a single Upgrade Plan button

---

## Screenshots Checklist
- [ ] `TC-Free-000` Signed-in Free Plan label
- [ ] `TC-Free-001` Status-bar menu
- [ ] `TC-Free-002` Menu-bar menu
- [ ] `TC-Free-003` Yellow header icon
- [ ] `TC-Free-003` Red header icon
- [ ] `TC-Free-004` Static banner (info, 75%)
- [ ] `TC-Free-005` Static banner (warning, 90%)
- [ ] `TC-Free-006` Inline warning under chat turn
