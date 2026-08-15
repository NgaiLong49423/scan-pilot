> **Document:** A01 Broken Access Control Research  
> **File:** `docs/research/security/A01-BROKEN-ACCESS-CONTROL.md`  
> **Version:** v0.1.0  
> **Created:** 2026-08-12  
> **Last Updated:** 2026-08-12  
> **Status:** Under Review  

# A01:2025 — Broken Access Control Research

## Research Status

Research checkpoint completed. The candidate rule scope is accepted as a research checkpoint, but the rules are not official inspection rules yet.

## Scope and Sources

This checkpoint reviewed:

- OWASP Top 10:2025 A01 — Broken Access Control;
- OWASP ASVS 5.0.0 V8 Authorization;
- OWASP Authorization Cheat Sheet;
- OWASP Authorization Testing Automation Cheat Sheet;
- OWASP Authorization Regression Testing Cheat Sheet;
- OWASP IDOR Prevention Cheat Sheet.

Primary links:

- https://owasp.org/Top10/2025/A01_2025-Broken_Access_Control/
- https://github.com/OWASP/ASVS/blob/v5.0.0_release/5.0/en/0x17-V8-Authorization.md
- https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
- https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Testing_Automation_Cheat_Sheet.html
- https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Regression_Testing_Cheat_Sheet.html
- https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html

## Core Concept

Authentication answers who the user is. Authorization answers what that user is allowed to do. A correctly authenticated user can still exploit broken access control if a protected operation or object lacks authorization enforcement.

Example:

```http
GET /orders/1001
```

If User B can read User A's order by changing the identifier, authentication succeeded but object-level authorization failed.

## Principles Retained

1. Authorization must be checked for every protected request.
2. An object identifier, including a UUID, is not an authorization control.
3. Client-side UI restrictions do not replace server-side enforcement.
4. Deny-by-default is recommended, but `permitAll()` alone does not prove a vulnerability.
5. Authorization should be tested continuously because later changes can reintroduce a weakness.
6. Existing authorization tests are stronger evidence than absence-based source inference alone.

## Verification Model

Most authorization requirements are classified as:

```text
Automability: PARTIAL
Detection: HYBRID
```

**Reason:** Authorization may be enforced in controllers, services, middleware, annotations, configuration, or external policy components. Not seeing a check in one file is insufficient proof.

## ASVS V8 Mapping

| ASVS requirement | Topic | Automability | Detection | V1 research priority |
|---|---|---|---|---|
| `v5.0.0-8.1.1` | documented function/data authorization | PARTIAL | AI | LATER |
| `v5.0.0-8.1.2` | documented field authorization | PARTIAL | AI | LATER |
| `v5.0.0-8.1.3`, `8.1.4` | contextual/adaptive decisions | PARTIAL/MANUAL | AI | NOT TARGETED |
| `v5.0.0-8.2.1` | function-level authorization | PARTIAL | HYBRID | MUST candidate |
| `v5.0.0-8.2.2` | object-level authorization | PARTIAL | HYBRID | MUST candidate |
| `v5.0.0-8.2.3` | field-level authorization | PARTIAL | HYBRID | SHOULD candidate |
| `v5.0.0-8.2.4` | adaptive authorization | PARTIAL/MANUAL | AI | NOT TARGETED |
| `v5.0.0-8.3.1` | trusted server-side enforcement | PARTIAL | HYBRID | MUST candidate |
| `v5.0.0-8.3.2` | authorization-change propagation | PARTIAL | AI/HYBRID | LATER |
| `v5.0.0-8.3.3` | delegated authorization | PARTIAL | AI | LATER |
| `v5.0.0-8.4.1` | tenant isolation | PARTIAL | HYBRID | LATER |
| `v5.0.0-8.4.2` | administrative interface protection | PARTIAL/MANUAL | AI | NOT TARGETED |

## Candidate Rule Scope

### MUST candidates

#### SP-AUTHZ-001 — Function-Level Authorization Verification

Determine whether sensitive or privileged operations have appropriate authorization enforcement.

Candidate evidence includes an admin-like route, destructive operation, privileged business function, authentication context, enforcement in the request path, and optional execution/test evidence.

#### SP-AUTHZ-002 — Object-Level Authorization Verification

Trace a request-controlled identifier through controller, service, and data access to determine whether access to a protected object includes ownership or permission enforcement.

This is a potential flagship rule, but absence-only source evidence must not be presented as certainty.

#### SP-AUTHZ-003 — Server-Side Authorization Enforcement

Correlate frontend authorization signals with backend endpoints and verify that the backend independently enforces access control.

### SHOULD candidate

#### SP-AUTHZ-004 — Field-Level Authorization Verification

Identify potentially unauthorized modification or exposure of sensitive properties such as `role`, `isAdmin`, `ownerId`, `balance`, or `permissions`.

## Signals That Are Not Standalone Findings

### Sequential IDs

Guessable identifiers can increase attack practicality, but they do not prove missing authorization.

### `permitAll()`

A public route may intentionally permit unauthenticated access. The setting is supporting evidence only when combined with protected-resource context.

## Authorization Regression

The lifecycle must support:

```text
OPEN → RESOLVED → REGRESSED
```

An authorization matrix comparing roles to expected operations is a possible Phase 2 feature, not a core V1 commitment.

## Finding Naming

Incomplete evidence should use cautious wording such as `Potential Object-Level Authorization Gap`. Strongly verified evidence may use `Missing Object-Level Authorization`.

## Open Questions

- exact confidence scale;
- Evidence Strength persistence;
- scoring impact;
- Java/Spring and React analysis implementation;
- whether policy input belongs in V1 or Phase 2;
- threshold for a confirmed finding;
- ASVS coverage representation.

Do not modify this checkpoint unless new evidence changes an A01 conclusion.
