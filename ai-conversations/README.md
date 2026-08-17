# AI Conversation Exports

This directory contains the raw, unedited AI coding conversation required by the source PRD. Screenshots, excerpts and post-hoc summaries are not substitutes for the export.

## Export manifest

| File | Tool | Coverage | Format |
| --- | --- | --- | --- |
| [`2026-08-17-codex-session.md`](2026-08-17-codex-session.md) | OpenAI Codex desktop | Project discovery through GitHub handoff and push verification | Raw Markdown export with prompts, responses and tool activity |

## Integrity and security review

- The export body was moved into its own file without rewriting its content.
- Common private-key, GitHub token, OpenAI key, AWS key, authorization-header and bearer-token patterns were scanned; none were found.
- Local demonstration credentials and expired local CSRF values may appear because they are part of the reproducible acceptance flow. They are not production credentials.
- If further AI work materially changes the project, add a new raw export or replace the snapshot with a later complete export. Do not silently edit prior conversation content.
