# Agile SDLC Workflow

Work is organized as small user stories. A persona completes one stage, records its
artifact, sets the gate to `PENDING_PO`, and stops for Product Owner review.

## Stages

1. `intake` — `/persona-business-analyst`
2. `requirements` — `/persona-business-analyst`
3. `ux` — `/persona-ui-ux`
4. `architecture` — `/persona-architect`
5. `implementation_backend` — `/persona-backend-developer`
6. `implementation_frontend` — `/persona-frontend-developer`
7. `qa` — `/persona-qa`
8. `security` — `/persona-security`
9. `operations` — `/persona-devops-sre`
10. `done` — Product Owner only

The UX stage must produce `WIREFRAMES.md` with a Mermaid user-flow diagram and
low-fidelity screen/state diagrams. It also creates a visual Cursor Canvas wireframe
board when Canvas is available, with a dependency-free HTML preview as fallback.

## Frontend/backend fork

Architecture proposes and the PO approves:

- `implementation.profile`: `fullstack`, `backend-only`, or `frontend-only`
- `implementation.order`: `parallel`, `backend-first`, or `frontend-first`

For `parallel`, all required implementation gates become `READY` together. For a
contract-first order, the second gate remains `BLOCKED` until the first passes. A
non-required gate is `WAIVED` with the approved profile as its reason.

QA is the join point. It becomes `READY` only after both implementation gates are
`APPROVED` or `WAIVED`. Frontend and backend consume architecture-owned contracts;
contract changes require a recorded blocker and PO decision to reopen architecture.

## Gate states

- `BLOCKED`: prior stage has not been approved or waived.
- `READY`: prior stage passed and the persona may be invoked.
- `IN_PROGRESS`: the assigned persona is working on the stage.
- `PENDING_PO`: artifact is ready for PO review.
- `APPROVED`: PO accepted the stage.
- `CHANGES_REQUESTED`: PO rejected it with feedback; the same persona revises it.
- `WAIVED`: PO explicitly skipped the stage with a recorded reason.
- `DONE`: final completion state after PO approval.

## Handoff contract

Every persona must:

1. Read `STATUS.yaml` and all approved upstream sections in `ARTIFACTS.md`.
2. Verify all prerequisites are satisfied, including the implementation profile/order.
3. Ask the PO instead of guessing product decisions.
4. Work only within the active persona contract.
5. Record evidence, assumptions, blockers, and open questions.
6. Set its gate to `PENDING_PO` and stop.
7. Provide the exact approval command and next recommended persona.

## Iteration

If a downstream stage exposes an upstream problem, do not silently rewrite approved
artifacts. Record the issue as a blocker and ask the PO which stage to reopen. The
orchestrator records the decision before work resumes.

Skills guide behavior but are not a deterministic workflow engine. The orchestrator
and `STATUS.yaml` provide coordination; PO decisions provide authority.

## Migrating an older story

Older kit versions used one `implementation` gate. Do not automatically mark both new
gates approved. Add `implementation.profile`, `implementation.order`,
`implementation_backend`, and `implementation_frontend`; then ask the PO which existing
evidence belongs to each gate. Preserve the old decision in `decisions` for audit history.
