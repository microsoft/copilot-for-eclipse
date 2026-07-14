# 1. Sub-agent enablement is governed by the language server

Date: 2026-07-01

## Status

Accepted

## Context

Historically the Eclipse client tried to gate sub-agents (the `run_subagent`
tool) itself, through three overlapping mechanisms:

- a user preference (`subAgentEnabled`) with a toggle on the Chat preference
  page,
- an in-memory organization-policy flag (`subAgentPolicyEnabled`) updated from
  the server's `policy/didChange` notification, and
- the client-preview feature flag.

Because the `subAgent` capability is read only once, at LSP initialization —
which always happens *before* the first `policy/didChange` arrives — the client
persisted policy state by force-writing the user preference to `false`. This
coupling was intricate and had two defects we confirmed while investigating:

1. **The client-side policy gate never actually worked.** The language server
   emits the policy key `subagent.enabled` (all lowercase), but the Eclipse DTO
   deserializes `@SerializedName("subAgent.enabled")`. Gson never matched them,
   so `subAgentPolicyEnabled` stayed at its default `true` forever.
2. **Fresh users advertised the wrong value.** `FeatureFlags.isSubAgentEnabled()`
   reads the raw `InstanceScope` node with a `false` fallback and does not
   consult the default scope (`true`), so a user who never opened the Chat
   preference page advertised `subAgent=false` at init.

Meanwhile the language server already enforces the policy authoritatively:

- `run_subagent` registration is gated on `subagent.enabled` **before** the
  client capability is consulted (policy wins), and
- when the policy flips to disabled at runtime, the server forces
  `subAgent: false` into its own capabilities provider and re-evaluates,
  unregistering the tool live.

So the client capability is an *offer of support*, and the server is free to
override it. The elaborate client-side gating and persistence were redundant
with — and weaker than — the server's own enforcement.

## Decision

The Eclipse client will **always advertise the `subAgent` capability at
initialization** and delegate all sub-agent enablement and policy enforcement to
the language server. Concretely:

- Remove the `subAgentEnabled` user preference and its toggle on the Chat
  preference page.
- Remove the client-preview coupling for sub-agents.
- Remove the now-dead client-side policy plumbing: the `subAgentPolicyEnabled`
  flag, the `TOPIC_DID_CHANGE_SUB_AGENT_POLICY` event, the `subAgentEnabled`
  field on the policy DTO, and the preference force-write bridge.
- Do **not** persist any sub-agent policy state on the client.

## Consequences

- **Simpler, correct behavior.** Sub-agents are available to every user unless
  the organization policy forbids them, enforced in one authoritative place.
  This also fixes the fresh-user "advertised `false`" bug.
- **No dedicated client-side opt-out.** The `subAgentEnabled` toggle is removed; the client always advertises `subAgent`.
  Users who previously disabled sub-agents via the toggle may still have a persisted `mcpToolsModeStatus` override for
  `run_subagent` and may need to reset their MCP tool settings to re-enable it (subject to org policy).
- **Server dependency.** Correct behavior now relies on the server continuing to
  gate `run_subagent` on `subagent.enabled`. If a future server regressed that
  gate, the client would no longer provide a second line of defense.
- **Orphaned preference key.** The `subAgentEnabled` instance-scope value left on
  existing installs is inert; no migration is performed.
- **Reversal cost.** Re-introducing a client-side toggle would mean rebuilding
  the preference, the tool-picker seeding, and the init-vs-policy persistence
  bridge — non-trivial, which is why this is recorded here.
