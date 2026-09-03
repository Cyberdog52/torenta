# Torenta: Two Days from Legacy to Modern

**Team achievement report | Monday 31 August and Tuesday 1 September 2026**

## What is Torenta?

Torenta is a media discovery and BitTorrent download manager. It helps users discover movies and
TV series through TMDB, find torrents, download and organize media in a local library, and identify
missing episodes. The modernized application now also supports natural-language, AI-assisted media
discovery while keeping factual media data and download decisions under application and user
control.

The product consists of an Angular web application, a Spring Boot REST API, integrations with TMDB
and torrent sources, and a vendored BitTorrent engine.

## Executive summary

In two focused days, four engineers moved Torenta from a long-unmaintained stack to a current,
testable, distributable application - while also delivering major new product capabilities.

| Headline | Result |
| --- | ---: |
| Integrated Git activity | **87 commits**: 68 regular commits and 19 merges |
| Pull requests merged | **14** |
| Activity by day | **38 commits Monday**, **49 Tuesday** |
| Repository change | **332 files**, 28,482 lines added, 14,221 removed |
| Total line changes | **42,703** additions and deletions |
| New files | **183** |
| New automated test/spec files | **49** |
| Test code added | **8,334 lines** |
| Current automated checks | **350** across backend, frontend, and E2E |
| Latest result | **346 passed, 4 skipped, 0 failed** |
| Backend coverage | **89.63% lines**, **79.52% branches**, **97.06% classes** |
| Legacy frontend audit | **147 vulnerable package entries** |
| Current frontend audit | **0 known vulnerabilities** across 624 dependencies |

This was not only a dependency update. The team introduced two new backend feature slices, rebuilt
the frontend architecture, made downloads durable and controllable, added recommendations and an
AI Concierge, established meaningful quality gates, improved accessibility, and created a portable
distribution.

## The technology leap

| Area | Before | After |
| --- | --- | --- |
| Java | 11 | **25** |
| Spring Boot | 2.2.1 | **4.1.1** |
| Gradle | 7.2 | **9.7.1** |
| Angular | 6.1 | **22.1** |
| Angular Material | 6.4 | **22.1 / Material 3** |
| TypeScript | 2.9 | **6.0** |
| RxJS | 6.2 | **7.8** |
| Node.js | Not pinned | **24 LTS**, pinned and build-managed |
| Frontend unit tests | Jasmine/Karma-era setup | **Vitest** |
| End-to-end tests | Protractor | **Playwright** |
| Linting and formatting | TSLint/Codelyzer | **ESLint, angular-eslint, Prettier** |
| API documentation | Springfox 2.9 | **Springdoc OpenAPI 3.1** |
| Java mocking | EasyMock | **Mockito** |
| jsoup | 1.12.1 | **1.23.2** |
| Guice | 3.0 | **7.0** |

The Angular application now uses standalone components, lazy-loaded routes, signals, computed
state, `httpResource`, OnPush change detection, modern template control flow, and zoneless
operation. Indicators of the architectural shift include:

- Components: **12 -> 16**
- REST controllers: **6 -> 8**
- Spring services: **6 -> 9**
- Signal initializations: **0 -> 11**
- Computed state expressions: **0 -> 35**
- `httpResource` usages: **0 -> 19**
- Application Java source files: **50 -> 122**
- Frontend application TypeScript files: **50 -> 75**

## New and improved functionality

### Natural-language AI Concierge

Users can now describe what they want to watch in natural language, for example a genre, mood,
actor, period, rating, language, or runtime. The new Concierge:

- Supports local **Ollama** by default and **OpenAI** as an alternative provider.
- Extracts a typed, evidence-backed search intent instead of trusting free-form model output.
- Resolves and retrieves factual candidates through TMDB.
- Lets the model rank only application-supplied candidate IDs and explanations.
- Validates criteria through an allowlisted filter registry.
- Returns at most 20 validated movie or series results.
- Never starts a torrent or download automatically.
- Has explicit backend and browser timeouts rather than an endless loading state.

This is a full vertical feature: provider abstraction, configuration, REST endpoint, DTOs,
normalization, TMDB discovery, ranking, frontend flow, tests, and cross-platform Ollama
documentation.

### "What should I watch next?" recommendations

A new Recommendations page scans the local series library and identifies aired episodes that are
still missing:

- Defaults to recently touched series for fast scans, with an option to scan the complete library.
- Returns up to three chronologically ordered missing episodes per series.
- Can continue into a new season that does not yet have a local directory.
- Separates "nothing missing" from series that could not be resolved through TMDB.
- Searches trusted/VIP torrent results directly from each recommendation card.
- Starts a selected episode download without another metadata round trip.
- Caches results and supports manual rescanning.

### A real download lifecycle

Downloads changed from a largely in-memory operation into a durable, recoverable workflow:

- **Pause, restart, stop and delete, and remove** actions were added.
- Stable download IDs and atomic on-disk records preserve lifecycle state.
- Running downloads recover as paused after an application restart.
- Finished files are verified against a persisted manifest.
- Partial payloads use an isolated staging directory before safe finalization.
- Failed peer discovery and terminated processing now surface as explicit failures.
- The backend calculates allowed actions so the UI does not guess what is safe.
- Shutdown pauses and persists running work.
- Torrent networking now chooses a routable address instead of potentially binding to a VPN,
  Hyper-V, or WSL adapter with no Internet route.

### Better first-run and error behavior

- TMDB configuration moved into the Preferences UI.
- Missing configuration now leads users to Preferences and produces actionable notifications.
- Backend errors are shown rather than silently failing.
- Null preference values are handled explicitly.
- Search, recommendation, download, and empty states provide visible progress and feedback.

## Look, feel, and usability

The frontend was not only migrated; it was redesigned:

- Material 3 theming, a refreshed color palette, and improved toolbar.
- Light/dark theme handling.
- Backdrop artwork and ambient gradients on expanded media results.
- More structured search cards for AI, series, movies, and torrents.
- Rich result headers with posters, ratings, dates, runtime, genres, and download state.
- Lazy-loaded images, routes, and detail content.
- Overview popovers and clearer media detail panels.
- Immediate visual feedback while downloads are starting or actions are pending.
- Confirmation before destructive download cleanup.
- Consistent notification service for errors and information.
- Per-route page titles, metadata, `robots.txt`, and favicon improvements.
- A browser shortcut in the portable archive for direct startup.

The production build now splits Search, Downloads, Recommendations, Preferences, and the 404 page
into lazy chunks. The initial bundle is approximately **511.39 kB raw / 123.92 kB estimated
transfer size**.

## Accessibility

Accessibility was treated as a user journey rather than a single linting task:

- The search-to-download flow gained semantic headings and clearer page structure.
- Controls have explicit accessible names; icon-only actions have labels.
- Decorative images and icons are hidden from assistive technology.
- Ratings, dates, runtimes, download locations, genres, and action state have screen-reader text.
- Loading, empty, success, result-count, and error changes use `aria-live` or alert semantics.
- Search fields have clear labels, hints, and named clear actions.
- Download progress and available actions are announced meaningfully.
- Keyboard/focus styling and touch targets were improved.
- Automated **axe-core WCAG 2 A/AA** checks now cover Search, Recommendations, Preferences, and the
  404 page.

The current Playwright run passed all four active accessibility route checks with no detectable
axe violations. This automation complements, rather than replaces, manual screen-reader and
keyboard evaluation.

## Security improvements

### Dependency risk reduction

Running `npm audit` against the legacy lockfile reports **147 vulnerable package entries**:
26 critical, 82 high, 31 moderate, and 8 low. The resolved legacy graph contained 1,237
dependencies.

After the migration, `npm audit` reports **zero known vulnerabilities** across the current 624
dependencies. The upgrade removed legacy dependency paths associated with advisories including:

- [CVE-2025-66035](https://github.com/advisories/GHSA-58c5-g7wp-6w37): Angular XSRF token leakage.
- [CVE-2026-33937](https://github.com/advisories/GHSA-2w6w-674q-4c4q): Handlebars JavaScript
  injection through AST type confusion.
- [CVE-2026-24842](https://github.com/advisories/GHSA-34x7-hfp2-rc4v): `node-tar` arbitrary file
  creation/overwrite through hardlink path traversal.
- [CVE-2026-59873](https://github.com/advisories/GHSA-23hp-3jrh-7fpw): `node-tar`
  decompression/parse denial of service.

These figures describe packages in the resolved frontend/build dependency graphs; they do not
imply that every advisory was exploitable through Torenta.

### Application and development security

- TMDB and OpenAI keys are stored in operating-system user preferences rather than tracked
  application configuration.
- Missing OpenAI credentials fail explicitly; there is no silent or insecure fallback.
- Diagnostic AI logging is disabled by default, and TMDB keys are removed from logged URLs.
- Download destinations are normalized and checked against the configured root, including
  real-path/symlink containment checks.
- Stop-and-delete removes only files owned by the selected download.
- AI-generated filters are typed, evidence-checked, media-compatible, and allowlisted.
- AI providers cannot access the torrent or download services.
- A central `AI_RULES.md` now defines protected files, secret handling, architecture, testing,
  documentation, and safe AI behavior for Copilot and other assistants.
- Tool-specific instruction pointers and ignore/deny rules reduce the chance that AI tooling reads
  protected credentials or modifies security-sensitive files.
- CI permissions were tightened and lint enforcement restored.

## Quality engineering

### Test expansion

| Test layer | Legacy | Current | Latest result |
| --- | ---: | ---: | ---: |
| Backend JUnit | 43 identifiable test annotations | **297 discovered tests** | 295 passed, 2 skipped |
| Frontend unit | 3 tests | **42 tests** | 42 passed |
| End-to-end | 1 Protractor scenario | **11 Playwright tests** | 9 passed, 2 skipped |
| **Total** | **47** | **350** | **346 passed, 4 skipped** |

The repository gained **34 backend test files**, **9 frontend unit spec files**, and **6 Playwright
spec files**. Net test-suite size grew from 14 to 61 files. In line terms, the change added:

- Backend tests: **6,343** lines.
- Frontend unit tests: **1,293** lines.
- End-to-end tests and support: **698** lines.
- Total: **8,334 added / 337 removed** test lines.

Coverage is now an enforced build gate for application code, excluding the vendored BitTorrent
library:

| Coverage | Result | Enforced minimum | Margin |
| --- | ---: | ---: | ---: |
| Lines | **89.63%** | 82% | +7.63 percentage points |
| Branches | **79.52%** | 70% | +9.52 percentage points |
| Methods | **86.02%** | - | - |
| Classes | **97.06% (99/102)** | - | - |

The quality toolchain now combines JUnit 5, Mockito, JaCoCo, Vitest, Playwright, axe-core, ESLint,
angular-eslint, Prettier, production bundle budgets, and GitHub Actions.

## Build, delivery, and developer experience

- Added a GitHub Actions Gradle workflow with explicit permissions.
- Integrated frontend installation and builds into Gradle through the Node Gradle plugin.
- Reproducible frontend builds use the lockfile and `npm ci`.
- Added a cross-platform portable ZIP containing the application, runtime, Windows and Unix
  launchers, and browser shortcut.
- No local Java installation is needed to run the packaged application.
- Added first-start, source-build, portable-build, Playwright, AI provider, and Ollama
  documentation.
- Added dev/test proxying, lazy production builds, and modern IDE/run support.
- Promoted the application version from `0.0.1-SNAPSHOT` to **1.0.0**.

## Four engineers, one result

Git identities and email aliases were normalized to the four team members. Commit counts include
integration and merge activity and should be read as workflow evidence, not as a productivity
ranking.

| Team member | Regular commits | Merge commits | Total activity | Main contribution themes |
| --- | ---: | ---: | ---: | --- |
| Andres | 23 | 13 | **36** | Backend/platform upgrade, CI, Mockito and coverage, build integration, download actions, visual redesign |
| David | 22 | 5 | **27** | Portable distribution, runtime packaging, preferences and TMDB-key flow, error UX, build cleanup and E2E stability |
| Simon | 10 | 0 | **10** | Angular 22 migration, signal modernization, accessibility, Lighthouse/SEO, recommendations, frontend fixes |
| Thomas | 13 | 1 | **14** | AI engineering guardrails, Playwright migration, torrent reliability, AI Concierge, tests and documentation |

The more important story is the overlap: modernization enabled new features; new features drove
tests; tests exposed reliability issues; accessibility changed UI implementation; and packaging
forced the frontend and backend builds to become one repeatable delivery process.

## AI-assisted engineering

Git cannot reliably identify which individual lines were generated, reviewed, or refined with AI,
so this report intentionally does not invent an "AI-written percentage." The observable outcome is
that the team used AI as an accelerator across migration, implementation, review, testing,
documentation, and accessibility work while adding controls around that usage:

- One authoritative ruleset for security, architecture, quality, and documentation.
- Thin entry points for GitHub Copilot, Claude, Gemini, and general coding agents.
- Protected-file exclusions and explicit secret-handling rules.
- An accessibility workflow with WCAG references, color-contrast tooling, and axe regression tests.
- Review-driven follow-up commits rather than treating generated code as finished code.

The result demonstrates a useful pattern for mixed-seniority teams: AI can widen the set of tasks
people can tackle quickly, while automated tests, reviews, architecture boundaries, and security
rules remain the shared safety net.

## Honest follow-ups

The two-day result is substantial, but not "finished forever":

- Two backend tests are skipped: a manual BitTorrent download test and a symlink-containment test
  that this Windows environment cannot execute without symlink privileges.
- Two Playwright tests are skipped: the real Concierge provider-protocol flow and the Downloads
  accessibility scan. The latter documents a remaining empty-key hint contrast issue.
- The production build passes but reports two budget warnings: the initial bundle is 11.39 kB over
  its 500 kB budget, and Search SCSS is 323 bytes over its 4 kB budget.
- The architecture document still contains two stale "Angular 6" labels even though the running
  stack and setup documentation use Angular 22.
- The npm audit result covers the frontend/build graph. The repository does not currently contain
  an equivalent Maven CVE scanner, so "zero vulnerabilities" must not be generalized to every
  backend dependency.
- Automated axe checks cover a useful WCAG subset, not every accessibility requirement; manual
  keyboard, screen-reader, zoom, and cognitive-usability testing should continue.

## Measurement notes

- Reporting window: local timezone `+02:00`, from 2026-08-31 00:00 through
  2026-09-01 23:59:59.
- Integrated scope: `master`, from the last pre-window commit `2387afe` through end-of-window
  commit `357604f`.
- Net file and line statistics use the baseline-to-final tree diff, avoiding inflation from merges,
  rebases, and repeated patches.
- Commit statistics include all commits integrated into that range; merge commits are shown
  separately.
- Git aliases were normalized by known contributor email identity.
- Coverage is from JaCoCo for `ch.andreskonrad.torenta/**`; vendored `bt/**` code is excluded.
- Test, build, lint, E2E, coverage, and audit figures were reproduced on 2 September 2026.
