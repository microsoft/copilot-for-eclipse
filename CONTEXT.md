# Context & Glossary

This file is a glossary of the ubiquitous language used in this codebase. It is
intentionally free of implementation detail — it defines *what words mean*, not
*how things are built*.

## Terms

### Sub-agent
A delegated agent that the main agent spawns to carry out a scoped sub-task on
its behalf. The main agent invokes it through the `run_subagent` tool and
receives the sub-agent's result back as part of its own turn.

### Sub-agent policy
Organization-level governance that decides whether sub-agents are permitted for
a user. It is authoritative and is **enforced by the language server**, not by
the editor client: when the policy forbids sub-agents, the server refuses to
offer the `run_subagent` tool regardless of what the client advertises.

### Capability
A feature the editor client declares support for when it initializes its
connection to the language server. A capability is an *offer of support*, not a
*grant of permission* — the server may still withhold the corresponding feature
(for example, because the sub-agent policy forbids it).

### Client preview feature
A gate that exposes in-development features to a subset of users. It is distinct
from the sub-agent policy: after this change, the availability of sub-agents no
longer depends on the client preview feature.
