---
description: Initiate a code review on a specific pull request, using the `reviewing-changes` skill.
argument-hint: PR URL
version: 1.0.0
---

## INPUTS

- PR

## INSTRUCTIONS

Use the `reviewing-changes` skill to review pull request $1. This is a local code review, do not post any feedback to GitHub.

## OUTPUT FORMAT

- Overall summary must be written to `pr-review-summary.md`.
- Inline comments must be written to `pr-review-inline-comments.md`.
- Output files must be written, even if there are no issues found.
