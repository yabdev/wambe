---
name: sdlc-orchestrator
description: Coordinates PO-gated Agile stories, records decisions, reports status, and recommends the next SDLC persona. Use when starting a story, approving a gate, checking progress, or deciding what happens next.
disable-model-invocation: true
---

# SDLC Orchestrator

The human user is always the Product Owner. Coordinate the workflow; never replace
the PO or bypass `.cursor/rules/sdlc-governance.mdc`.

## Commands to understand

- `Start a new story: <idea>`
- `Status <STORY-ID>`
- `APPROVE <STORY-ID> stage: <stage>`
- `REJECT <STORY-ID> stage: <stage> — reason: <reason>`
- `WAIVE <STORY-ID> stage: <stage> — reason: <reason>`
- Any decision may end with `AND CONTINUE`.

## Start a story

1. Ask the PO for the problem, target user, desired outcome, priority, and constraints.
2. Assign the next unused `US-NNN` ID and a short kebab-case slug.
3. Create `docs/sdlc/stories/{STORY-ID}-{slug}/`.
4. Copy and customize all files from `docs/sdlc/_templates/story/`, including the
   wireframe template.
5. Set `intake` to `IN_PROGRESS`; leave later stages `BLOCKED`.
6. Recommend `/persona-business-analyst <STORY-ID> stage: intake`.

## Record a PO decision

1. Find the story by ID and read `STATUS.yaml`.
2. Confirm the named stage is `PENDING_PO`; otherwise explain the mismatch and stop.
3. For `REJECT`, require a reason and set `CHANGES_REQUESTED`.
4. For `WAIVE`, require a reason and set `WAIVED`.
5. For `APPROVE`, set `APPROVED`.
6. Append a dated entry to `decisions` with actor `Product Owner`.
7. After `APPROVED` or `WAIVED`, evaluate prerequisites in `docs/sdlc/WORKFLOW.md`,
   update eligible stage(s) to `READY`, and refresh the handoff. Do not mark work
   `IN_PROGRESS` until its persona starts.
8. If the PO did not say `AND CONTINUE`, stop and show the next persona command.
9. If the PO said `AND CONTINUE`, read the next persona's `SKILL.md`, set its stage to
   `IN_PROGRESS`, and adopt that contract only after the decision is saved. Stop if
   that persona needs unanswered PO input.

## Implementation fork and join

After architecture approval:

1. Require an approved `implementation.profile` and `implementation.order`.
2. For `fullstack`, both implementation gates are required.
3. For `backend-only`, set `implementation_frontend` to `WAIVED` with the approved
   architecture profile as reason.
4. For `frontend-only`, set `implementation_backend` to `WAIVED` similarly.
5. For `parallel`, set all required implementation gates to `READY`.
6. For `backend-first` or `frontend-first`, set only the first required gate to `READY`;
   set the other to `BLOCKED` until the first is `APPROVED` or `WAIVED`.
7. Set `qa` to `READY` only when both implementation gates are `APPROVED` or `WAIVED`
   and at least one was implemented rather than waived.
8. `AND CONTINUE` may start only one implementation persona in a turn. If both are
   eligible, ask the PO which one to start.

## Status report

Show the story title, current stage, each gate status, blockers, artifact path, and
one exact next action for the PO. Do not perform persona work during a status request.

## Stage order

`intake → requirements → ux → architecture → {implementation_backend, implementation_frontend} → qa → security → operations → done`

Business Analyst owns both `intake` and `requirements`. Backend and frontend are sibling
stages with a join before QA. All stages map to the matching `/persona-*` skill in
`docs/sdlc/WORKFLOW.md`.
