# Copilot Enterprise Plan (Unlimited Org) — Quota UI

## Overview
This file covers the **Enterprise plan with unlimited premium-interaction
usage** configured by the organization — the most common Enterprise
configuration. Under this configuration the usage menus replace the Monthly
Limit row with a single informational message, and the chat-view
quota-warning surfaces stay dormant because there is no monthly cap to cross.

### Surface-by-surface expectations (unlimited org)

| Surface                                       | Expected behaviour                                                                                                  |
|-----------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| Plan label in menu header                     | `Enterprise Plan`                                                                                                   |
| Header row icon                               | Blue (full) usage icon                                                                                              |
| Monthly Limit row                             | **Hidden.** Replaced by a disabled message: `You have no monthly limit on AI credits usage set by your organization`. |
| Allowance-reset row                           | **Hidden.** There is no monthly allowance to reset.                                                                 |
| Additional-usage status row                   | **Hidden.**                                                                                                         |
| Enable Additional Usage / Increase Budget row | **Hidden.**                                                                                                         |
| `Upgrade Plan` row                            | Present **iff** the language server reports `canUpgradePlan = true` for the signed-in account. Typically `false`.   |
| Static banner (chat view)                     | Not triggered. No 75% / 90% threshold to cross.                                                                     |
| Inline quota warning under a chat turn        | Not triggered by quota exhaustion (no cap).                                                                          |

### Why the chat-view warning surfaces are not exercised here
With unlimited premium-interaction usage there is no allowance to cross, so
neither the 75% info banner nor the 90% warning banner can fire, and the
quota-exhausted inline warning is unreachable. Banner / inline cases live in
the bounded-org and CFI plans. Any appearance of those surfaces on an
unlimited Enterprise account is a regression.

---

## Test Cases

### TC-CE-U-000: Sign in with an unlimited Enterprise org account, record `canUpgradePlan`

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- Plugin is installed and Eclipse is signed out.
- A GitHub account that belongs to a **Copilot Enterprise organization
  configured with unlimited premium-interaction usage** is available.
- Language-server logging is enabled and the Eclipse Copilot Language Servers
  Log console is open (see the README section *How to Read `canUpgradePlan`
  From the Language-Server Log*).

#### Steps
1. Open the Copilot status-bar menu and click `Sign in to GitHub`.
2. Complete the device-flow sign-in with the unlimited Enterprise account.
3. Wait for the status-bar icon to switch to the signed-in icon.
4. In the Eclipse Copilot Language Servers Log console, find the most recent
   `copilot/quotaChange` notification and:
   - **Record `canUpgradePlan`** (`true`, `false`, or absent). Later cases
     refer to this as "the recorded `canUpgradePlan` value". For a typical
     Enterprise account this is `false`.
   - Confirm the payload indicates that the premium-interactions quota is
     unlimited. If it is not, the account is not actually on an unlimited
     org and the test should be re-run on a correctly provisioned account.

#### Expected Result
- The header row of both menus reads `<username> — Enterprise Plan`.
- The `canUpgradePlan` value and the "unlimited" indication have been
  recorded.

#### 📸 Key Screenshots
- [ ] Signed-in state with the `Enterprise Plan` label
- [ ] Language-server log showing `canUpgradePlan` and the unlimited flag

---

### TC-CE-U-001: Status-bar menu — unlimited org informational message

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-CE-U-000 completed; the signed-in user is on an unlimited Enterprise org.

#### Steps
1. Click the Copilot icon in the Eclipse status bar.

#### Expected Result
1. Header row: `<username> — Enterprise Plan` on top; second row reads
   `Copilot Usage` with the **blue/full usage icon**.
2. A **single disabled message row** reading exactly:
   `You have no monthly limit on AI credits usage set by your organization`.
3. **No** Monthly Limit row.
4. **No** allowance-reset row (no `Resets today` / `Reset in N days …` text).
5. **No** `Additional usage enabled` / `Additional usage not enabled` status
   row, and **no** tooltip on such a row.
6. **No** `Enable Additional Usage` / `Increase Budget` action row.
7. `Upgrade Plan` row — present **iff** the recorded `canUpgradePlan` value
   (TC-CE-U-000) is `true`; absent otherwise. For a typical Enterprise
   account it must not appear.
8. Standard footer rows (Sign Out, Preferences, etc.) appear as usual below
   the usage section.

#### 📸 Key Screenshots
- [ ] Status-bar menu, unlimited Enterprise org (full menu)

---

### TC-CE-U-002: Menu-bar menu mirrors the status-bar menu

**Type:** `Happy Path`
**Priority:** `P0`

#### Steps
1. Open `GitHub Copilot` from the Eclipse menu bar.
2. Compare against the menu captured in TC-CE-U-001.

#### Expected Result
The menu-bar menu shows the same rows in the same order with the same labels,
tooltips, and icons as the status-bar menu. The unlimited-org informational
message must be identical between the two surfaces.

#### 📸 Key Screenshots
- [ ] Menu-bar menu, unlimited Enterprise org

---

### TC-CE-U-003: Chat works without any quota banner or inline warning

**Type:** `Happy Path`
**Priority:** `P0`

#### Preconditions
- TC-CE-U-001 succeeded.
- Copilot chat view is open in Agent mode.

#### Steps
1. Send a normal chat message (e.g. `Hello, what can you do?`) and wait for
   the response.
2. Send a second chat message that triggers tool use (e.g. ask to read a file
   in the workspace).
3. Inspect the chat view above the input area and under each completed turn.

#### Expected Result
- Both turns complete successfully.
- **No** static banner appears above the chat input (no info-icon banner, no
  warning-icon banner, no `Upgrade Plan` link).
- **No** inline warning appears under either turn (no quota-exhausted
  message, no `Upgrade Plan` button, no `Enable Additional Usage` button).

#### 📸 Key Screenshots
- [ ] Chat view with two completed turns, no banner and no inline warning

---

## Screenshots Checklist
- [ ] `TC-CE-U-000` Signed-in `Enterprise Plan` label + language-server log payload
- [ ] `TC-CE-U-001` Status-bar menu, unlimited org informational message
- [ ] `TC-CE-U-002` Menu-bar menu mirrors status-bar menu
- [ ] `TC-CE-U-003` Chat view with no banner / no inline warning
