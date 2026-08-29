> **Document:** Technical Review Summary Template
> **File:** `assets/codex-review-summary.md`
> **Version:** v1.1.0
> **Created:** 2026-08-16
> **Last Updated:** 2026-08-30
> **Status:** Template

# Technical Review Summary

```md
Review outcome: APPROVED_FOR_DEV_MERGE | CHANGES_NEEDED | BLOCKED
Issue: #<N>
Reviewed PR: #<N>
Reviewed head commit: <SHA>

## Contract and Scope

<result>

## Acceptance-Criteria Evidence

<result>

## Verification

<result, including unavailable checks>

## Safety Review

<result without reproducing secrets>

## Required Follow-up or Verification Limit

- None | ...
```

Use `APPROVED_FOR_DEV_MERGE` only when the reviewed head commit meets the accepted contract and passes CI within the stated verification limit.
