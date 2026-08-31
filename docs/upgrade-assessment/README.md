# Upgrade Assessment

This directory holds the dependency- and platform-upgrade assessment for the project. The
documents are meant to be read **in order**: the baseline establishes the "before" state and the
single reference date, the two ecosystem assessments choose per-part upgrade targets, and the
upgrade plan reconciles them into one ordered, mutually-compatible backlog. **No upgrade is applied
in any of these documents** — they are assessment and planning only; later tasks execute the units.

All "latest stable" / "newest viable" values across every document are taken as of the single
**reference date recorded in [`00-baseline.md`](./00-baseline.md)**, so the whole assessment is
made against one consistent ecosystem snapshot.

## Index

| # | Document | Purpose |
|---|---|---|
| 00 | [`00-baseline.md`](./00-baseline.md) | Current build/test **baseline** (backend green on JDK 17, frontend red on Node 24), tool versions, ignored/flaky tests, restoration prerequisites, and the single reference date. |
| 10 | [`10-backend-assessment.md`](./10-backend-assessment.md) | **Backend** (Java/Gradle) per-part assessment: current vs. latest-stable vs. target, abandoned/replaced flags, and risk-based backlog units. |
| 20 | [`20-frontend-assessment.md`](./20-frontend-assessment.md) | **Frontend** (Angular/npm) per-part assessment: current vs. latest-stable vs. compatible target (Angular stepwise-major + version matrix), deprecated/removed flags, and risk-based backlog units. |
| 30 | [`30-upgrade-plan.md`](./30-upgrade-plan.md) | **Synthesis**: newest mutually-compatible target set (+ excluded newer releases), decision tickets for unresolved blockers, the numbered/parallelizable execution stages, and the backlog-integrity summary table. |

## How to read

1. Start with **00** for the baseline state and the reference date every later document cites.
2. Read **10** and **20** for the per-ecosystem target choices and the individual upgrade units.
3. Finish with **30** for the combined target set, the decision tickets, the execution order, and
   the backlog-integrity table that ties every unit to a stage.
