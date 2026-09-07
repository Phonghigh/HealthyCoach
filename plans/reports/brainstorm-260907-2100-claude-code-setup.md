# Brainstorm: Claude Code setup for HealthyCoach

date: 260907-2100 | scope: tooling only, no product/feature planning

## Problem statement
Repo has docs/ (11 files, Vietnamese, docs-only, no code/stack decided) but no `.claude/` dir.
Need baseline Claude Code config so future sessions have project context without re-deriving it.

## Decisions (confirmed w/ user via AskUserQuestion)
- Session scope: Claude Code setup ONLY. No product/feature brainstorming today.
- Product type: undecided (personal vs commercial) — keep architecture-neutral.
- Tech stack: undecided — do not pick one in this setup.
- Garmin API access: none yet — FIT/TCX import remains fallback assumption, unchanged.
- CLAUDE.md: create at repo root.
  - Content: project one-liner, doc-map (links to docs/index.md tree), explicit "docs-only, no code yet" status, condensed open-questions summary (from assumptions-and-open-questions.md) so it's visible without opening that file, path rules (plans/ and docs/ only for .md, already global but worth restating for the project-level reader).
- Memory system: SKIP for now. Reason (user's own): product/stack decisions not firm yet — writing memory now risks stale/invalidated entries. Revisit once product type + stack are decided.
- settings.json: create minimal file. Permissions allowlist: NONE yet (empty) — user prefers to add entries later as friction comes up, rather than pre-guess categories.

## Approach chosen
Minimal, doc-status-mirroring setup — do not over-build config for a repo with zero code.
Rejected alternative: seeding memory now + broad allowlist — rejected by user as premature (YAGNI).

## Next steps (implementation, pending user go-ahead)
1. Create `E:\Tool\HealthyCoach\CLAUDE.md`:
   - Project one-liner (from docs/index.md line 3)
   - Doc map (mirror docs/index.md structure/links)
   - Status: docs-only, no implementation, stack undecided
   - Condensed open-questions table (from assumptions-and-open-questions.md, trimmed)
   - Note: plans/ and docs/ are the only allowed locations for new .md files (already global rule, restated locally for visibility)
2. Create `E:\Tool\HealthyCoach\.claude\settings.json`:
   - Minimal valid settings.json, empty/no permissions allowlist entries (placeholder structure only if needed, otherwise `{}`)
3. Do NOT touch memory directory this session.

## Open questions
None remaining for this setup scope — all clarified via AskUserQuestion above.
Product-level open questions (target date fixed?, commercial vs personal, stack, Garmin API, clinical review, privacy, maps) remain tracked in docs/assumptions-and-open-questions.md and are out of scope today.
