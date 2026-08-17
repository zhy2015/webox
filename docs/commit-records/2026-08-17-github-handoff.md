# GitHub Handoff

Date: 2026-08-17

## Purpose

Prepare the verified initial release for external startup, review and continuation by another developer or AI coding agent.

## Changes

- Added a five-minute clone and verification path to the root README.
- Added an explicit maintainer and AI reading order.
- Documented architecture invariants that protect authentication, pricing, cutoff, idempotency and inventory correctness.
- Kept the raw AI conversation export visibly incomplete instead of replacing it with a generated summary.

## Release facts

- Primary branch: `main`.
- Verification entry point: `./scripts/verify.sh`.
- Local application URL: `http://127.0.0.1:5173`.
- Demo accounts and non-secret local credentials are documented in the root README.
