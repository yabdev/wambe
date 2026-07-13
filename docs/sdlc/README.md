# SDLC Workspace

This folder is the shared memory and handoff bus for Cursor personas. The human user
is always the Product Owner.

## Quick start

```text
/sdlc-orchestrator Start a new story: <problem or opportunity>
```

For an existing story:

```text
/sdlc-orchestrator Status US-001
/persona-business-analyst US-001 stage: intake
/sdlc-orchestrator APPROVE US-001 stage: intake
```

Add `AND CONTINUE` only when you want approval and the next stage in one turn:

```text
/sdlc-orchestrator APPROVE US-001 stage: ux AND CONTINUE
```

Reject or waive with a reason:

```text
/sdlc-orchestrator REJECT US-001 stage: architecture — reason: cost is too high
/sdlc-orchestrator WAIVE US-001 stage: ux — reason: backend-only maintenance
```

## Source of truth

Each `docs/sdlc/stories/<story>/` folder contains:

- `ARTIFACTS.md` — outputs from every persona
- `STATUS.yaml` — gates, handoff, blockers, and PO decision history

A completed persona stage is not approved. It remains `PENDING_PO` until the PO
records a decision through the orchestrator.

After architecture, frontend and backend are separate gates. The architecture artifact
defines whether the story is full-stack, frontend-only, or backend-only and whether
required implementation work runs in parallel or contract-first.

See `WORKFLOW.md` for stage ownership and `DEFINITION-OF-DONE.md` for completion rules.
