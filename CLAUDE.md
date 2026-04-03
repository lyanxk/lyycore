# CLAUDE.md

## Purpose

This file defines the working rules for this Minecraft mod project.

Project priorities:

1. No Mixin
2. Performance first
3. Clean and maintainable code

---

## Hard Rules

### No Mixin

This project does **not** use Mixin.

Rules:

- Do not add Mixin as a dependency.
- Do not create any mixin classes or mixin config files.
- Do not use bytecode patching as a default solution.
- Prefer loader-supported APIs, events, registries, callbacks, and data-driven approaches.

If a feature seems to require Mixin, first try to redesign it or reduce scope.

### Performance First

Performance is more important than feature complexity.

Rules:

- Avoid unnecessary per-tick logic.
- Avoid allocations in hot paths.
- Avoid scanning large collections every tick.
- Avoid excessive packet syncing.
- Avoid heavy logic in rendering code.
- Prefer event-driven logic over polling.
- Cache only when invalidation is clear and safe.
