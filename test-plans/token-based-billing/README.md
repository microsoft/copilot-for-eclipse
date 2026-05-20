# Copilot Plan / Quota UI Test Plans

## Overview
Test plans for the plan- and quota-aware UI surfaces in GitHub Copilot for
Eclipse. One file per Copilot plan; each plan exercises the same three
surfaces so cross-plan differences stay easy to compare.

All accounts under test are on token-based billing.

## Surfaces Under Test
1. **Status-bar usage menu** — opened by clicking the Copilot icon in the
   Eclipse status bar.
2. **Menu-bar usage menu** — opened from `GitHub Copilot` in the Eclipse menu
   bar. Must be visually identical to the status-bar menu.
3. **Quota-warning surfaces in the chat view**
   - **Static banner** shown above the chat input area when the user crosses
     one of the chat-view quota-usage thresholds (see *Quota-Warning
     Thresholds* below).
   - **Inline warning widget** shown under a chat turn when a chat request
     fails because the quota is exhausted.

   The chat-view warning surfaces are driven only by the quotas that affect
   chat usage: the **Chat Messages** quota on the Free plan and the
   **AI-credit (premium interactions)** quota on every paid plan. The Code
   Completions quota does not trigger banners or inline warnings in the chat
   view, even though it is shown in the status-bar / menu-bar menus.

## Quota-Warning Thresholds
The static banner is pushed by the language server when the signed-in
account's tracked usage crosses one of these two thresholds:

| Usage crossed | Banner severity / icon |
|---------------|------------------------|
| 75%           | Info icon              |
| 90%           | Warning icon           |

Each threshold fires at most once per quota reset period. To re-trigger a
banner for the same threshold, the quota usage on the GitHub quota portal
must first be lowered back below the threshold (or the reset date must pass)
so the language server re-arms the notification.

Per-plan cases below are ordered from **rich quota to poor quota**: usage
**under 75%** (no banner), usage **above 75%** (info banner), and usage
**above 90%** (warning banner). To exercise a specific threshold set usage
on the quota portal a few percentage points **above** the target threshold
(for example, **~76%** to cross 75% and **~91%** to cross 90%) and then send
a chat message.

## Plan → File Map
| Copilot plan       | Plan label shown in the menu      | File                       |
|--------------------|-----------------------------------|----------------------------|
| Free               | Copilot Free Plan                 | `TBB-test-free.md`         |
| Pro                | Copilot Pro Plan                  | `TBB-test-pro.md`          |
| Pro+               | Copilot Pro+ Plan                 | `TBB-test-pro-plus.md`     |
| Max                | Copilot Max Plan                  | `TBB-test-max.md`          |
| Business           | Business Plan                     | `TBB-test-business.md`     |
| Enterprise         | Enterprise Plan                   | `TBB-test-enterprise.md`   |

## Common Preconditions
- Eclipse is running with the GitHub Copilot for Eclipse plugin installed.
- The tester has access to the GitHub quota portal at
  `https://github.com/github-copilot/quotas/<accountId>` to adjust usage on the
  signed-in account. The menus refresh in real time after a quota change; no
  Eclipse restart is required.
- The Copilot chat view is open in Agent mode for chat-related cases.
- Each plan starts with a sign-in case (`TC-<plan>-000`) using an account on
  that plan with token-based billing enabled.

## Quota Action Summary
This table describes which overage action the chat-view warning surfaces are
expected to show for each plan.

| Plan         | Overage action                                        |
|--------------|-------------------------------------------------------|
| Free         | —                                                     |
| Pro          | Enable Additional Usage / Increase Budget (primary)   |
| Pro+         | Enable Additional Usage / Increase Budget (primary)   |
| Max          | Enable Additional Usage / Increase Budget (primary)   |
| Business     | —                                                     |
| Enterprise   | —                                                     |

Notes:
- The label switches from "Enable Additional Usage" to "Increase Budget" once
  additional paid usage has been enabled for the user.
- The **Upgrade Plan** action (row in the menu, link in the static banner,
  button in the inline warning) is **not** plan-driven. It must appear if and
  only if the language server reports `canUpgradePlan = true` for the
  signed-in account; otherwise it must be absent from every surface. Each
  per-plan file starts with a step that records the actual `canUpgradePlan`
  value from the language-server log, and all later cases reference that
  recorded value.

## How to Read `canUpgradePlan` From the Language-Server Log
1. In Eclipse, open `Window → Preferences → Language Servers`.
2. Tick **Log to console** (and optionally **Log to file**) for the GitHub
   Copilot language server, then `Apply and Close`.
3. Open `Window → Show View → Other... → General → Console`.
4. From the Console toolbar dropdown (the open-console icon), choose the
   `Eclipse Copilot Language Servers Log` console entry that traces traffic for the GitHub
   Copilot language server.
5. Sign in to GitHub (or sign out and back in) so a fresh handshake is logged.
6. Locate the most recent `copilot/quotaChange` notification (sent from the
   server). Note the value of the `canUpgradePlan` field in its payload — it
   is `true`, `false`, or absent.
   - `true` → expect Upgrade Plan everywhere it can appear.
   - `false` or absent → expect Upgrade Plan to **not** appear.
