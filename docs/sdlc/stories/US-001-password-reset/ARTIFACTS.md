# US-001 — Self-service password reset

## Intake

### Problem

Registered users who forget their passwords must contact support, causing delayed
account recovery and avoidable support work.

### Target users and stakeholders

- Registered users who control the email address on their account
- Customer Support, Security, and Operations

### Desired outcome and success measures

Users can regain access without support. Initial success measures are reduced
password-reset tickets and a high successful-reset completion rate.

### Scope, constraints, dependencies, and priority

- In scope: request reset, receive email link, choose a new password, return to login
- Out of scope: MFA recovery, administrator-initiated resets, changing account email
- Dependencies: transactional email provider and unique user email addresses
- Constraint: responses must not reveal whether an account exists

### Open questions

None for this example.

## Requirements

### User story and business value

As a registered user, I want to reset a forgotten password so that I can regain access
without contacting support.

### In scope / out of scope

In and out of scope are unchanged from the approved intake.

### Functional and non-functional requirements

- A user can request a reset using an email address.
- The response is identical for known and unknown addresses.
- A reset link contains a single-use, expiring token.
- A valid token permits setting a password that meets the configured policy.
- Reset requests and successful resets create security audit events without secrets.
- The endpoint is rate-limited.

### Business rules and dependencies

Tokens expire after 24 hours and become invalid after successful use. A new successful
request invalidates older unused tokens for that account.

### Acceptance criteria

1. Given any syntactically valid email, when a reset is requested, then a generic success
   response is returned without disclosing account existence.
2. Given a known email, when a request is accepted, then a reset message is queued and
   no raw token is written to logs.
3. Given a valid unused token less than 24 hours old, when a compliant password is
   submitted, then the password changes and the token cannot be reused.
4. Given an invalid, expired, or used token, when a password is submitted, then no
   password changes and the user receives a safe error.
5. More than three requests for the same account identity within one hour are limited.

### Open questions

None for this example.

## UX

### User journey and flows

1. Login → **Forgot password?**
2. Enter email → generic confirmation screen
3. Open email link → enter and confirm new password
4. Success → return to login

### Screens, components, content, and states

- Email form: idle, invalid email, submitting, and generic success
- New-password form: loading, weak password, mismatch, invalid token, and success
- Unknown email uses the same confirmation content and timing policy as a known email

### Responsive and accessibility requirements

Forms work at mobile and desktop widths. Inputs have persistent labels, errors are linked
to inputs and announced, focus moves to the result heading, and all actions are keyboard
operable. Password requirements are available before submission.

### Wireframes or prototype links

[Wireframe diagrams](WIREFRAMES.md)

### Risks and open decisions

The PO should confirm whether successful reset invalidates all existing sessions.

## Architecture

Pending UX approval.

## Implementation — Backend

Blocked by architecture approval.

## Implementation — Frontend

Blocked by architecture approval.

## QA

Blocked until every required implementation gate is approved or waived.

## Security

Blocked by QA approval.

## Operations

Blocked by security approval.

## Final Product Owner Notes

This sample demonstrates a story currently waiting at the UX approval gate.
