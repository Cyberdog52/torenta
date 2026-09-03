# Torenta Modernization: AI-Assisted vs. Classic Development

**A reasoned effort estimate for achieving the same two-day outcome without AI assistance**

## Executive estimate

The observed AI-assisted effort was:

- **4 engineers x 2 working days = 8 person-days**
- **2 elapsed working days**

Delivering the same integrated scope with a classic, predominantly manual development approach is
estimated at:

- **42-65 person-days**, with a planning midpoint of approximately **54 person-days**
- About **13-20 elapsed working days** for the same four-person team after allowing for sequencing,
  reviews, integration, and rework
- Roughly **8-13 weeks** for one engineer, assuming uninterrupted full-time work

This implies an estimated saving of **34-57 person-days**, or approximately **81-88% less effort**
for this particular modernization sprint. Expressed as throughput, the AI-assisted team delivered
approximately **5-8 times** the work normally expected from the same person-day investment.

> This is a counterfactual estimate, not a controlled scientific benchmark. It describes this
> repository, this team, and this unusually broad legacy-modernization scope. It should not be
> generalized into a universal AI productivity factor.

## Why the classic estimate is much larger

Torenta's change was not one isolated feature. The team simultaneously upgraded a deeply outdated
backend and frontend, replaced obsolete tooling, fixed security findings, created new product
features, redesigned workflows, expanded automated testing, improved accessibility, established
CI, produced a portable distribution, and documented the result.

Without AI, engineers would still be able to deliver the same result. More time would primarily be
spent on:

- Finding and comparing migration guides, release notes, CVEs, framework specifications, and API
  documentation.
- Discovering breaking changes one at a time through compiler, runtime, and test failures.
- Writing repetitive migrations, DTOs, tests, mocks, configuration, and documentation manually.
- Reconstructing unfamiliar legacy behavior before safely changing it.
- Diagnosing integration failures across Angular, Spring Boot, Gradle, Node.js, Playwright, TMDB,
  the filesystem, and the vendored BitTorrent engine.
- Performing additional review cycles because fewer issues would be detected during implementation.

AI did not remove engineering judgment, reviews, testing, or integration work. It compressed the
search, drafting, translation, and feedback loops around those activities.

## Workstream estimate

One person-day represents approximately one focused engineer working day. Ranges account for
different engineer familiarity, hidden legacy behavior, and normal trial-and-error.

| Workstream | Classic effort | Why it would take this long without AI |
| --- | ---: | --- |
| Legacy-system archaeology and planning | **2-3 days** | Trace frontend/backend flows, identify obsolete libraries, understand the vendored torrent engine, establish safe change boundaries, and split work across four people. |
| Manual research and specification reading | **3-5 days** | Read Java, Spring Boot, Gradle, Angular, Material, TypeScript, RxJS, Vitest, Playwright, OpenAPI, Spring AI, Ollama, WCAG, and packaging documentation; compare migration paths and breaking changes. |
| Backend platform modernization | **3-5 days** | Migrate Java 11 to 25, Spring Boot 2.2 to 4.1, Gradle 7.2 to 9.7, Springfox to Springdoc, EasyMock to Mockito, dependency APIs, Jakarta namespaces, trailing-slash behavior, and failing tests. |
| Frontend migration and architecture | **6-9 days** | Move Angular 6 to 22 and TypeScript 2.9 to 6.0; replace removed APIs; adopt standalone components, signals, `httpResource`, modern templates, lazy routes, zoneless operation, Material 3, ESLint, and Prettier. |
| Dependency and application security | **2-3 days** | Audit the legacy graph, research advisories, select safe versions, remove obsolete packages, move keys out of tracked configuration, add path containment, review logging, and verify the final dependency graph. |
| Durable download lifecycle | **4-6 days** | Design and implement persistent records, stable IDs, atomic writes, staging, recovery, pause/restart/delete/remove behavior, safe ownership-based cleanup, failure states, networking fixes, UI actions, and tests. |
| Recommendation feature | **3-4 days** | Design the API and scan strategy, resolve series through TMDB, find missing aired episodes across seasons, cache results, build the page, add torrent search/download integration, and test edge cases. |
| AI Concierge feature | **5-8 days** | Research Spring AI and provider APIs, design typed intent and allowlisted filters, implement Ollama/OpenAI adapters, candidate retrieval and ranking, timeouts, safe logging, REST/frontend contracts, UI, tests, and setup documentation. |
| UX, visual design, and accessibility | **3-5 days** | Redesign cards and toolbar, implement theming and backdrops, add responsive states and notifications, manually inspect semantics and keyboard behavior, read WCAG guidance, add ARIA, and create axe tests. |
| Test and quality modernization | **4-6 days** | Replace Protractor, establish Vitest and Playwright, migrate mocks, create dozens of test files and fixtures, raise backend coverage, configure JaCoCo gates, investigate flaky tests, and stabilize CI behavior. |
| CI, build, and portable distribution | **3-5 days** | Build the workflow, resolve permissions and environment differences, integrate Node into Gradle, enforce locked installs, bundle a runtime, create cross-platform launchers, preserve permissions, and test archive startup. |
| Reviews, integration, debugging, and documentation | **4-6 days** | Review parallel changes, resolve merge conflicts, fix regressions and NPEs, run complete quality gates, update architecture/setup/security docs, incorporate feedback, and prepare a coherent release. |
| **Estimated total** | **42-65 person-days** | Approximately **54 person-days** at the midpoint. |

The categories deliberately avoid counting product management, procurement, release approvals, or
waiting for external stakeholders. Including those organizational lead times could make the
calendar duration longer without increasing engineering effort.

## Side-by-side lifecycle comparison

| Development activity | AI-assisted camp approach | Classic approach |
| --- | --- | --- |
| Understanding legacy code | AI-assisted repository search, explanation, dependency mapping, and rapid hypothesis generation; engineers verified behavior in code and tests. | Engineers manually navigate call chains, search documentation, build mental models, and validate assumptions through debugging. |
| Technology research | Tools summarize relevant migration steps and suggest current APIs while work is in progress. | Engineers manually search release notes, Stack Overflow, issue trackers, specifications, and multiple migration guides, then reconcile conflicting advice. |
| Migration implementation | AI drafts repetitive changes and translates old patterns into modern equivalents; compiler and tests remain authoritative. | Engineers hand-convert files, look up syntax and APIs, compile, inspect each failure, and repeat. |
| New feature design | AI rapidly proposes DTOs, interfaces, edge cases, and test scenarios; engineers choose architecture and safety boundaries. | Engineers perform the same design manually through code reading, whiteboarding, API research, and incremental prototypes. |
| Coding | Boilerplate, mappings, tests, mocks, configuration, and documentation can be generated quickly and reviewed. | Every artifact is typed and cross-checked manually; context switching between technologies costs more time. |
| Trial and error | Short generate-run-diagnose-correct loops; AI can interpret build output and propose targeted corrections. | Engineers search each unfamiliar error, compare possible causes, experiment, and manually propagate fixes. |
| Security work | AI helps inventory dependency and code risks, explain advisories, draft guards, and check likely exposure paths. | Engineers manually run audits, research each advisory, trace vulnerable packages, inspect secret flows, and design mitigations. |
| Testing | AI creates broad candidate test matrices and repetitive fixtures; engineers run, review, and correct them. | Engineers manually enumerate cases and write fixtures, assertions, mocks, and E2E flows before stabilization. |
| Accessibility | AI accelerates WCAG lookup, semantic reviews, ARIA suggestions, and generation of axe checks. | Engineers manually audit components against WCAG and framework guidance, then implement and retest each correction. |
| Code review | AI-assisted pre-review catches some defects and inconsistencies before human review; human feedback still drives fixes. | Human reviewers find more first-pass mechanical issues, increasing review turnaround and rework. |
| Documentation | AI drafts synchronized setup, architecture, security, and feature documentation from implemented code for engineer review. | Engineers reconstruct and write documentation manually after implementation, often requiring another verification pass. |
| Integration and delivery | AI helps diagnose Gradle, Node, CI, packaging, and cross-platform failures across several tools. | Engineers manually search logs and ecosystem documentation, reproduce environment differences, and test hypotheses sequentially. |

## A plausible classic-development timeline

Simply dividing 42-65 person-days by four people gives 10.5-16.25 days, but not all work can run in
parallel. Platform migrations must stabilize before feature and E2E work can finish; reviews and
integration follow implementation; packaging depends on both builds.

A realistic four-person schedule would therefore look approximately like this:

| Phase | Elapsed time | Typical focus |
| --- | ---: | --- |
| Assessment and migration planning | **2-3 days** | Legacy analysis, dependency inventory, target versions, risks, work split |
| Platform and tooling modernization | **4-6 days** | Backend/frontend migrations, test runners, linting, CI foundation |
| Product features and download reliability | **4-6 days** | Concierge, recommendations, durable lifecycle, error handling |
| UX, accessibility, security, and packaging | **2-3 days** | Visual redesign, WCAG work, audit remediation, portable archive |
| Integration, review, stabilization, and documentation | **2-3 days** | Full test cycles, reviews, fixes, release readiness |
| **Likely elapsed duration** | **13-20 working days** | Approximately **2.5-4 calendar weeks** |

The phases overlap, so their individual ranges do not sum directly. The elapsed estimate also
assumes the same four engineers remain available and can sustain parallel work.

## What AI appears to have accelerated most

The largest likely gains were not in typing production code. They were in shortening the time
between an unfamiliar problem and a reviewed, testable solution:

1. **Cross-generation migration knowledge.** The stack crossed approximately 14 Angular major
   versions and several generations of Java, Spring Boot, Gradle, TypeScript, testing, and linting
   tools. AI reduced repeated documentation lookup and pattern translation.
2. **Breadth across ecosystems.** Each engineer could move between Java, TypeScript, Gradle, npm,
   CI, accessibility, security, and documentation with less context-recovery time.
3. **Test creation.** The team added 49 automated test/spec files and 8,334 lines of test code.
   Repetitive fixtures, assertions, DTO construction, and edge-case enumeration are well suited to
   assisted drafting.
4. **Fast feedback loops.** Build errors, failing tests, lint findings, and review comments could be
   interpreted and addressed without beginning every investigation with manual web research.
5. **Documentation coverage.** Architecture, setup, AI-provider, packaging, testing, security, and
   contributor guidance evolved alongside the code rather than becoming a separate later project.

## What AI did not replace

The two-day outcome still depended on engineering work that cannot responsibly be delegated:

- Choosing the target architecture and deciding what behavior must be preserved.
- Understanding product intent and making user-experience trade-offs.
- Designing safe download deletion and filesystem boundaries.
- Deciding how AI should be constrained and where it must not have access.
- Reviewing generated changes and rejecting incorrect suggestions.
- Running builds, tests, audits, and end-to-end flows against the real repository.
- Resolving interactions between four parallel contributors.
- Recognizing incomplete work, such as skipped tests and bundle-budget warnings.

The appropriate comparison is therefore not "AI did the work instead of four engineers." It is
"four engineers used AI to remove a large amount of search, translation, boilerplate, and
trial-and-error time while retaining responsibility for the result."

## Confidence and limitations

**Confidence: medium.** The lower and upper bounds are intended to be more credible than a single
precise number.

Factors that could reduce the classic estimate:

- Engineers already deeply familiar with every target framework and Torenta's legacy internals.
- Reusing a proven migration playbook or code from another project.
- Reducing the scope, test depth, documentation, or accessibility work.

Factors that could increase it:

- Migration dead ends discovered only after substantial rework.
- Cross-platform packaging failures on environments not immediately available.
- External API instability, flaky E2E behavior, or subtle torrent/filesystem defects.
- Additional security review, manual accessibility testing, release governance, or stakeholder
  acceptance.
- Normal meetings, support duties, and interruptions; this estimate assumes focused engineering.

## Basis of comparison

The estimate uses the measured outcome documented in `camp-achievements.md`:

- 87 integrated commits and 14 merged pull requests.
- 332 changed files and 42,703 total line changes.
- Java 11 to 25, Spring Boot 2.2 to 4.1, Angular 6 to 22, and replacement of several obsolete tools.
- Two new backend feature slices plus major download lifecycle work.
- 350 automated checks, 89.63% backend line coverage, and 79.52% branch coverage.
- A frontend dependency audit reduced from 147 vulnerable package entries to zero known findings.
- UX, accessibility, CI, packaging, security, and documentation delivered in the same window.

No attempt was made to estimate an "AI-written percentage," because Git history cannot reliably
distinguish generated, edited, reviewed, and manually authored lines. The estimate instead compares
the observed eight-person-day outcome with the activities normally required to reproduce the same
working, tested, reviewed, documented, and integrated result.
