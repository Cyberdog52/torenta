# 00 — Build / Test Baseline

This document records the current state of the project's existing backend and frontend
checks before any dependency or platform upgrade work begins. It is the reference both
ecosystem upgrade assessments cite when judging upgrade readiness. Later assessment
documents must not re-establish the baseline — they build on what is recorded here.

## Assessment date (reference date)

**Assessment date: 2026-08-31**

This single date is the **reference date** for the whole upgrade assessment. Every
"latest stable" lookup in the backend and frontend assessment documents MUST be taken as
of this date, so that all upgrade-target decisions are made against one consistent
snapshot of the ecosystem. Do not use "today" or a per-document date in later tasks — use
2026-08-31.

## Baseline summary

| Ecosystem | Command(s) | Tool versions | Result |
|---|---|---|---|
| Backend (Java/Gradle) | `./gradlew clean build`, `./gradlew build` | Gradle 7.2; JDK 17 (CI target) / JDK 11 | **GREEN** on JDK 11 & 17 (42 tests, 0 failures, 4 skipped). **RED** on JDK 21 (machine default) — Lombok 1.18.22 does not support JDK 21. |
| Frontend (Angular/npm) | `npm ci`; `npm run build`; `npm test` | Angular CLI 6.2.2; Node v24.16.0; npm 11.13.0 | **RED** — `npm ci` passes (with warnings); `npm run build` and `npm test` fail on Node 24. No green baseline could be established on the tooling available here (required Node ≤ 16 is not installed; `nvm` unavailable). |

Reproduction note: the raw `./gradlew build` command exits non‑zero on this machine only
because the machine's default JDK is 21; it exits 0 under the CI-target JDK 17 (and JDK
11). See the backend section for the exact per-JDK invocations.

## Backend baseline (Java / Gradle)

**Build tool:** Gradle 7.2 (via `./gradlew`).
**Test framework:** JUnit 5 (`useJUnitPlatform()`), EasyMock 4.1.

### Commands, JDK used, and exit status

| Command | JDK used | Exit | Outcome |
|---|---|---|---|
| `./gradlew clean build` | JDK 21.0.11 (Corretto — machine default, `$JAVA_HOME=.../java/current`) | 1 | **FAILED** at `:compileJava` |
| `./gradlew clean build` | JDK 17.0.19 (Corretto — CI target) | 0 | **BUILD SUCCESSFUL** |
| `./gradlew clean build` | JDK 11.0.31 (Corretto — matches `sourceCompatibility`) | 0 | **BUILD SUCCESSFUL** |

The `auto` acceptance check for this task is `./gradlew build`. It completes the full build
and test suite and exits 0 when run with a supported JDK (17 = CI parity, or 11). Under the
machine's current default JDK 21 the same command fails during compilation (see below);
that failure is captured as an investigation prerequisite rather than a code defect.

### JDK 21 failure detail

```
> Task :compileJava FAILED
Execution failed for task ':compileJava'.
> java.lang.NoSuchFieldError: Class com.sun.tools.javac.tree.JCTree$JCImport does not
  have member field 'com.sun.tools.javac.tree.JCTree qualid'
```

Root cause: the annotation processor Lombok 1.18.22 relies on internal `javac` fields that
changed in newer JDKs; Lombok 1.18.22 does not support JDK 21. The build compiles cleanly
on JDK 11 and 17.

### JDK mismatch across the project

The project declares three different Java levels:

- **CI** (`.github/workflows/gradle.yml`): the `build` job sets up **JDK 17**; the separate
  `dependency-submission` job sets up **JDK 11**.
- **`build.gradle`**: `sourceCompatibility = '11'`.
- **Local machine default**: **JDK 21** (Corretto, selected via SDKMAN `current`).

So the authoritative "known good" toolchain today is **JDK 17** (what CI builds and tests
with), the compiled bytecode target is **Java 11**, and the developer default here (JDK 21)
is *newer than anything the toolchain supports*.

### Test results (JDK 17 run)

- **42 tests executed, 0 failures, 0 errors, 4 skipped.**

Per class (tests / skipped):

| Test class | Tests | Skipped |
|---|---|---|
| `bt.bencoding.BEEncoderTest` | 4 | 0 |
| `bt.bencoding.BEParserTest` | 13 | 0 |
| `bt.bencoding.BtClientTest` | 1 | 1 |
| `bt.bencoding.ByteStringComparatorTest` | 5 | 0 |
| `bt.bencoding.model.YamlBEObjectModelLoaderTest` | 4 | 0 |
| `ch.andreskonrad.torenta.preference.service.PreferenceServiceTest` | 2 | 0 |
| `ch.andreskonrad.torenta.tmdb.service.RequestThrottlerTest` | 3 | 0 |
| `ch.andreskonrad.torenta.tmdb.service.TmdbServiceTest` | 2 | 2 |
| `ch.andreskonrad.torenta.torrent.service.TorrentServiceTest` | 1 | 1 |
| `directory.service.DirectoryServiceTest` | 7 | 0 |

### Warnings observed (non-fatal)

- `Note: Some input files use or override a deprecated API.`
- `Note: bt/bencoding/model/ClassUtil.java uses unchecked or unsafe operations.`

## Frontend baseline (Angular / npm)

**Framework:** Angular 6 (Angular CLI 6.2.2, per `frontend/package.json` and
`frontend/README.md`).
**Node version present:** **v24.16.0**; **npm 11.13.0**.
**Node version required:** `frontend/package.json` `engines` declares `node: ">=10 <17"`
(and `npm: ">=6 <9"`). Angular 6 / webpack 4 tooling requires **Node ≤ 16**.

### `nvm use` / `.nvmrc`

The task's expected setup step is `cd frontend && nvm use && npm ci && npm run build`.
On this environment:

- **`nvm` is not installed / not on `PATH`** (`nvm: command not found`).
- **No `.nvmrc` file exists** in `frontend/` (or anywhere in the repo). The Node version
  constraint is therefore taken from `package.json` `engines.node` (`>=10 <17`), not from
  an `.nvmrc`.

Because a Node ≤ 16 runtime is not available here and cannot be selected via `nvm`, the
frontend checks were run against the only Node present (v24.16.0) to capture the real
outcome.

### Commands, Node used, and exit status

| Command | Node used | Exit | Outcome |
|---|---|---|---|
| `npm ci` | v24.16.0 | 0 | **PASSED** — installed 1176 packages, but emitted an `EBADENGINE` warning (`required node >=10 <17, current v24.16.0`) plus many `deprecated` warnings. |
| `npm run build` (`ng build`) | v24.16.0 | 1 | **FAILED** — `No such module: http_parser`. |
| `npm test` (`ng test --watch=false --browsers=ChromeHeadless`) | v24.16.0 | — | **FAILED to compile — 0 specs executed.** Webpack aborts with `error:0308010C:digital envelope routines::unsupported`. |

### Frontend failure detail

`npm run build` (`ng build`):

```
No such module: http_parser
Error: No such module: http_parser
    at process.binding (node:internal/bootstrap/realm)
    at node_modules/http-deceiver/lib/deceiver.js:22:24
    ... node_modules/spdy/lib/spdy/handle.js
```

Root cause: the old `spdy` / `http-deceiver` dependency chain calls
`process.binding('http_parser')`, an internal API removed in modern Node — the Angular 6
CLI dev/build server stack cannot load under Node 24.

`npm test` (`ng test`):

```
Error: error:0308010C:digital envelope routines::unsupported
    at node_modules/webpack/lib/util/createHash.js
ERROR [karma]: Error: callback(): The callback was already called.
```

Root cause: webpack 4 (bundled with Angular CLI 6) requests the legacy MD4 hash, which the
OpenSSL 3 provider bundled in Node 17+ rejects. No test specs run before the failure.
(There is exactly one spec in the project:
`frontend/src/app/torrent/downloads/downloads.component.spec.ts`.)

Both failures are the canonical symptoms of running an Angular 6 / webpack 4 toolchain on a
Node runtime far newer than it supports. A green frontend baseline requires Node ≤ 16.

## Known ignored / flaky tests (backend)

These are pre-existing conditions in the repository, not regressions introduced here:

- **`bt.bencoding.BtClientTest`** — the single test is annotated
  `@Ignore("Manual test, requires a valid magnet link")`. It is a manual/integration test
  and is expected to be skipped in automated runs.
- **`ch.andreskonrad.torenta.tmdb.service.TmdbServiceTest`** — both tests are `@Ignore`d
  (exercise the live TMDb API).
- **`ch.andreskonrad.torenta.torrent.service.TorrentServiceTest`** — its test is `@Ignore`d.
- **`RequestThrottler`** — previously flaky at the throttle-window expiry boundary; fixed in
  commit `6d09515` ("Fix flaky RequestThrottler expiry boundary in throttle window").
  `RequestThrottlerTest` (3 tests) now passes reliably in the runs above.

The 4 skipped tests reported by the suite correspond to the three `@Ignore`d test methods
above (`BtClientTest` ×1, `TmdbServiceTest` ×2, `TorrentServiceTest` ×1).

## Baseline-restoration / investigation prerequisites

Each check below is classified as GREEN (no restoration needed) or as requiring a
restoration/investigation prerequisite before the upgrades it blocks can proceed.

### GREEN — no restoration required

- **Backend build & test suite on the CI-target toolchain (JDK 17), and on JDK 11.**
  `./gradlew build` completes with 42 tests passing (4 intentionally ignored). This matches
  what CI runs. No restoration is required to begin the backend upgrade assessment.

### Restoration / investigation prerequisites

- **[BACKEND-JDK21] Backend does not build on JDK 21 (machine default).**
  - *Outcome:* `./gradlew clean build` fails at `:compileJava` with a Lombok
    `NoSuchFieldError`; Lombok 1.18.22 does not support JDK 21. Gradle 7.2 also predates
    JDK 21 support.
  - *Scope:* affects only newer-JDK runs; the CI (JDK 17) and JDK 11 builds are unaffected.
  - *Blocks:* any upgrade that raises the runtime/build JDK past 17 (e.g. moving CI or the
    Gradle toolchain to JDK 21+). Such an upgrade requires bumping Lombok (≥ 1.18.30) and
    Gradle (to a JDK‑21‑capable version) first, and resolving the `sourceCompatibility 11`
    vs CI‑17 mismatch.

- **[FRONTEND-NODE] Frontend has no green baseline on the available Node runtime.**
  - *Outcome:* `npm ci` succeeds (with `EBADENGINE` + deprecation warnings), but
    `npm run build` fails (`No such module: http_parser`) and `npm test` fails to compile
    (`digital envelope routines::unsupported`), executing 0 specs. A green run needs Node
    ≤ 16, which is not installed and cannot be selected (`nvm` unavailable, no `.nvmrc`).
  - *Scope:* the entire Angular/npm ecosystem check. `npm ci` (dependency resolution) is the
    only part currently green.
  - *Blocks:* every frontend upgrade decision — the Angular / Node / build-tooling
    assessment cannot measure "before vs after" until a runnable green baseline exists.
    Restoration options: (a) install a Node ≤ 16 runtime (add an `.nvmrc`/`nvm use` path or
    an equivalent version manager) to reproduce the historical green build/test, or (b)
    treat the Angular 6 → modern-Angular upgrade itself as the mechanism that restores a
    green build on current Node, verifying the single existing spec afterwards.

- **[FRONTEND-NVM] Expected `nvm use` / `.nvmrc` workflow is not reproducible here.**
  - *Outcome:* `nvm` is not on `PATH` and no `.nvmrc` exists, so the documented
    `nvm use && npm ci && npm run build` flow cannot be followed as written.
  - *Scope:* environment/tooling setup for the frontend.
  - *Blocks:* reliable reproduction of the frontend baseline by other contributors; should be
    resolved (add `.nvmrc` and/or document the version manager) as part of restoring the
    frontend baseline above.

## How to reproduce this baseline

```bash
# Backend — CI parity (green). Requires a JDK 17 (or 11) available.
JAVA_HOME=<path-to-jdk17> ./gradlew clean build       # BUILD SUCCESSFUL, 42 tests
./gradlew build                                        # fails only if default JDK > 17

# Frontend — from repo root (currently red on Node 24; green needs Node <= 16).
cd frontend
npm ci                                                 # passes (EBADENGINE warning)
npm run build                                          # fails: No such module: http_parser
npm test -- --watch=false --browsers=ChromeHeadless    # fails: OpenSSL digital-envelope error
```
