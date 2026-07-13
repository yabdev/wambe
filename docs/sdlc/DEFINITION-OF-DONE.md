# Definition of Done

A story is done only when:

- All required stages are `APPROVED` or explicitly `WAIVED` by the Product Owner.
- The PO-approved implementation profile is recorded; every required frontend/backend
  gate is `APPROVED` and every non-required gate has a reasoned `WAIVED` decision.
- Acceptance criteria are unambiguous and mapped to QA evidence.
- Frontend/backend contracts and integration behavior are verified where both apply.
- Relevant automated checks pass, with failures and unexecuted tests disclosed.
- No unresolved Critical or High security finding remains without an explicit PO waiver.
- Deployment, rollback, monitoring, and support ownership are documented.
- Documentation and operational runbooks affected by the change are updated.
- Known limitations and follow-up work are visible.
- The Product Owner explicitly enters:

```text
/sdlc-orchestrator APPROVE <STORY-ID> stage: done
```

The final approval changes the story state to `DONE`; no persona may do this on the
Product Owner's behalf.
