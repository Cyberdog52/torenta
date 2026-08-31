# 20 — Frontend Upgrade Assessment (Angular / npm)

This document assesses every versioned part of the frontend — the Node/Angular
toolchain plus every dependency declared in `frontend/package.json` (with the Node
constraint from `engines`/`.nvmrc` and the compile target from
`frontend/tsconfig.json`) — chooses compatible stable upgrade targets that honour
Angular's stepwise-major and version-matrix rules, flags deprecated/removed/replaced
components, and groups the work into risk-based backlog units. **No upgrade is applied
here**; this is assessment only. Later tasks execute the units below.

## Assessment date and sources

- **Assessment (reference) date: 2026-08-31** — the single reference date recorded in
  [`00-baseline.md`](./00-baseline.md). Every **latest-stable** value in this document is
  the newest stable (non-alpha/beta/RC/next/insiders) release available **as of
  2026-08-31**, not "today".
- **Sources for latest-stable and the version matrix** (queried on the assessment date):
  the npm registry `latest` dist-tag for each package, and — for the compatibility
  matrix — the target packages' own `peerDependencies`/`engines` metadata
  (`@angular/compiler-cli@22`, `@angular/core@22`, `@angular/cli@22`,
  `@angular-devkit/build-angular@22`) plus the versions `@schematics/angular@22`
  scaffolds. Prefer this registry/peer-metadata data over prose blog posts for exact
  version bounds.
- **Baseline reference:** the frontend build/test baseline is **RED** (see
  `00-baseline.md`). `npm ci` resolves, but `npm run build` fails
  (`No such module: http_parser`) and `npm test` fails to compile
  (`digital envelope routines::unsupported`), executing 0 specs, on the only Node runtime
  available here (v24.16.0). The two recorded restoration prerequisites are
  **[FRONTEND-NODE]** (no green build/test on the available Node; a green run needs Node
  ≤ 16, which is not installed) and **[FRONTEND-NVM]** (the `nvm use`/`.nvmrc` workflow is
  not reproducible — `nvm` is absent and **no `.nvmrc` file exists**). Because the
  baseline is red, every unit below is marked **blocked on baseline restoration / not
  execution-ready** (see the per-unit readiness lines and the Baseline linkage summary).

## Chosen target stack

The coherent, well-supported modern stack targeted below is **Angular 22.1.x + TypeScript
6.0.3 + RxJS 7.8.2 + zone.js 0.16.2 on Node 24 LTS**, with **angular-eslint 22** replacing
the deprecated TSLint/Codelyzer lint stack. Rationale:

- **Angular 22.1.4** (core/CDK/Material) / **22.1.6** (CLI/build tooling) is the latest
  stable Angular as of the assessment date. Its engines accept **Node `^24.15.0`** — which
  the baseline machine already runs (v24.16.0) — so the target stack is the natural
  mechanism that restores a green build on a modern Node (restoration option (b) in
  `00-baseline.md`), rather than pinning the project forever to an unsupported Node ≤ 16.
- The **stepwise-major** rule (below) means the framework cannot jump 6 → 22 in one move;
  the target is the *end state* of a sequenced 6 → 7 → … → 22 `ng update` journey, not a
  single edit.
- Where the individually newest release is **incompatible** with Angular 22 (TypeScript
  7.0.2, jasmine-core 7.0.2), the **newest viable** version is chosen and the excluded
  release is recorded (see "Version matrix & exclusions").

## Assessment table

Every versioned part in `frontend/package.json` (`engines`, `dependencies`,
`devDependencies`), the (absent) `frontend/.nvmrc`, and the compile target in
`frontend/tsconfig.json`. "Latest stable" is as of **2026-08-31** (per `00-baseline.md`).

| # | Part | Source of truth | Current | Latest stable (2026-08-31) | Compatible target | Action |
|---|---|---|---|---|---|---|
| 1 | Node runtime | `package.json` `engines.node` | `>=10 <17` | 26.8.1 (current); 24.x LTS | **Node 24 LTS (≥ 24.15.0)** — Angular 22 engines: `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0` | **Upgrade** — Unit 1 |
| 2 | `.nvmrc` (Node pin) | `frontend/.nvmrc` | **absent** (no file) | n/a | **Create** — pin Node 24 to make the toolchain reproducible | **Create** — Unit 1 (resolves [FRONTEND-NVM]) |
| 3 | npm | `package.json` `engines.npm` | `>=6 <9` | 11.x | **`>=8`** (Angular 22 CLI engines: `^6.11.0 \|\| ^7.5.6 \|\| >=8.0.0`) | **Widen** — Unit 1 |
| 4 | `@angular/core` (+ common, compiler, animations, forms, router, platform-browser, platform-browser-dynamic) | `package.json` deps | `^6.1.0` | 22.1.4 | **22.1.4** | **Upgrade** (major 6→22, stepwise) — Unit 1 |
| 5 | `@angular/compiler-cli` | devDeps | `^6.1.0` | 22.1.4 | **22.1.4** (must equal core) | **Upgrade** — Unit 1 |
| 6 | `@angular/language-service` | devDeps | `^6.1.0` | 22.1.4 | **22.1.4** (must equal core) | **Upgrade** — Unit 1 |
| 7 | `@angular/http` | deps | `^6.1.0` | — (**removed in Angular 8**) | **Remove** — replaced by `HttpClientModule` (`@angular/common/http`), **already the only HTTP API used** in `src/` | **Remove** — Unit 1 |
| 8 | `@angular/cdk` | deps | `^6.2.0` | 22.1.4 | **22.1.4** (CDK tracks Angular's major) | **Upgrade** — Unit 1 |
| 9 | `@angular/material` | deps | `^6.4.7` | 22.1.4 | **22.1.4** (Material tracks Angular's major) | **Upgrade** — Unit 1 |
| 10 | `@angular/cli` | devDeps | `~6.2.2` | 22.1.6 | **22.1.6** (must equal build tooling) | **Upgrade** — Unit 1 |
| 11 | `@angular-devkit/build-angular` | devDeps | `~0.8.0` | 22.1.6 | **22.1.6** (must equal CLI) | **Upgrade** — Unit 1 |
| 12 | `typescript` | devDeps | `~2.9.2` | 7.0.2 | **6.0.3** — Angular 22 peer `typescript: >=6.0 <6.1`; **7.0.2 excluded** | **Upgrade** — Unit 1 |
| 13 | `rxjs` | deps | `~6.2.0` | 7.8.2 | **7.8.2** — Angular 22 peer `^6.5.3 \|\| ^7.4.0` | **Upgrade** — Unit 1 |
| 14 | `zone.js` | deps | `~0.8.26` | 0.16.2 | **0.16.2** — Angular 22 peer `~0.15.0 \|\| ~0.16.0` (import path `zone.js/dist/zone` → `zone.js`) | **Upgrade** — Unit 1 |
| 15 | `core-js` | deps | `^2.5.4` | 3.50.0 | **Remove** — modern Angular polyfills need only `zone.js`; the sole active import (`core-js/es7/reflect` in `src/polyfills.ts`) is obsolete (evergreen browsers). Use `core-js` 3.x only if a specific legacy target is reinstated | **Remove** — Unit 1 |
| 16 | TypeScript compile target | `tsconfig.json` (`target`/`module`/`lib`) | `es5` / `es2015` / `[es2017, dom]` | n/a | **ES2022** target/module + `[ES2022, dom]` lib (regenerated by `ng update` for Angular 22) | **Upgrade** — Unit 1 |
| 17 | `@types/node` | devDeps | `~8.9.4` | 26.4.0 | **`^24`** — match the Node 24 LTS target (Angular 22 scaffolds `^20.17.19`; align to chosen runtime) | **Upgrade** — Unit 1 |
| 18 | `@types/jasmine` | devDeps | `~2.8.8` | 6.0.0 | **`~6.0.0`** (Angular 22 scaffold) | **Upgrade** — Unit 1 |
| 19 | `jasmine-core` | devDeps | `~2.99.1` | 7.0.2 | **`~6.3.0`** (Angular 22 scaffold; **7.0.2 excluded** — not yet validated by the Angular karma builder) | **Upgrade** — Unit 1 |
| 20 | `jasmine-spec-reporter` | devDeps | `~4.2.1` | 7.0.0 | **`~7.0.0`** (Angular 22 scaffold) | **Upgrade** — Unit 1 |
| 21 | `karma` | devDeps | `~3.0.0` | 6.4.4 | **`~6.4.0`** — `build-angular@22` peer `karma: ^6.3.0` | **Upgrade** — Unit 1 |
| 22 | `karma-chrome-launcher` | devDeps | `~2.2.0` | 3.2.0 | **`~3.2.0`** | **Upgrade** — Unit 1 |
| 23 | `karma-coverage-istanbul-reporter` | devDeps | `~2.0.1` | 3.0.3 | **Replace → `karma-coverage` `~2.2.0`** (the reporter Angular 22 scaffolds; istanbul reporter no longer used) | **Replace** — Unit 1 |
| 24 | `karma-jasmine` | devDeps | `~1.1.2` | 5.1.0 | **`~5.1.0`** (Angular 22 scaffold) | **Upgrade** — Unit 1 |
| 25 | `karma-jasmine-html-reporter` | devDeps | `^0.2.2` | 2.3.0 | **`~2.2.0`** (Angular 22 scaffold) | **Upgrade** — Unit 1 |
| 26 | `sass` (dart-sass) | deps | `^1.63.6` | 1.103.1 | **1.103.1** | **Upgrade** — Unit 4 |
| 27 | `protractor` | devDeps | `~5.4.0` | 7.0.0 (**deprecated / end-of-life**) | **Remove; decision-needed** replacement (Cypress / Playwright / WebdriverIO) | **Replace/decision** — Unit 3 |
| 28 | `@types/jasminewd2` | devDeps | `~2.0.3` | 2.0.13 | **Remove** — jasminewd2 is the Protractor/Jasmine adapter; goes with Protractor | **Remove** — Unit 3 |
| 29 | `ts-node` | devDeps | `~7.0.0` | 10.9.2 | **Decision-bound** — only kept if the chosen e2e replacement needs it; else remove with Protractor | **Decision** — Unit 3 |
| 30 | `tslint` | devDeps | `~5.11.0` | 6.1.3 (**deprecated, EOL 2020**) | **Replace → `angular-eslint` 22.2.0** | **Replace** — Unit 2 |
| 31 | `codelyzer` | devDeps | `~4.3.0` | 6.0.2 (**deprecated**) | **Replace → `angular-eslint` 22.2.0** (Angular lint rules folded in; standalone codelyzer retired) | **Replace** — Unit 2 |

## Angular stepwise-major requirement

Angular's supported upgrade path advances **one major at a time**: `ng update
@angular/core@N @angular/cli@N` refuses to skip majors and runs that major's schematic
migrations before the next. Going from the current **Angular 6** to the target **Angular
22** is therefore a sequenced journey of **16 major hops** (6 → 7 → 8 → … → 22), each an
`ng update` with its own codemods — not a single package.json edit. The breaking changes
along the path that touch parts assessed here:

- **v8** — `@angular/http` is **removed** (deprecated since v4.3/v5); code must use
  `HttpClient` from `@angular/common/http`. This project already does, so row 7 is a clean
  drop. Differential loading; TypeScript ≥ 3.4.
- **v9** — Ivy becomes the default renderer/compiler (largest single-hop risk).
- **v10–v11** — `tslint`/`codelyzer` are **deprecated**; `angular-eslint` is named the
  successor. `core-js` disappears from the scaffolded polyfills.
- **v12–v13** — **Protractor** is dropped from `ng new` (third-party e2e prompt instead);
  View Engine is removed (Ivy only); production build by default; Node minimum rises.
- **v16** — the RxJS baseline is firmly on the 7.x line; esbuild dev-server preview.
- **v17** — the esbuild/vite **application builder** becomes the default build system;
  new control-flow; Node minimum rises again.
- **v18–v22** — incremental; TypeScript and Node minimums climb to the v22 matrix below.

Each hop has its **own** version matrix, so TypeScript, RxJS, zone.js and Node advance in
lockstep with `@angular/core` at every step — they cannot be pinned while core moves.

## Version matrix & exclusions (Angular 22 target)

The Angular 22 target binds the toolchain as follows (from the packages' own
`peerDependencies`/`engines` as of 2026-08-31):

| Bound part | Angular 22 constraint | Chosen | Newest available | Excluded? Why |
|---|---|---|---|---|
| Node | `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0` | **24 LTS (≥ 24.15.0)** | 26.8.1 | Node 26 is *allowed* but 24 LTS is chosen for stability and because the baseline host already runs v24.16.0. **Node 23 and 25 (odd, non-LTS) are excluded** — the matrix has gaps at them. |
| npm | `^6.11.0 \|\| ^7.5.6 \|\| >=8.0.0` | **≥ 8** (11.x available) | 11.x | none |
| TypeScript | `>=6.0 <6.1` | **6.0.3** | 7.0.2 | **7.0.2 excluded** — outside `<6.1`; Angular 22's compiler is not built against TS 7. |
| RxJS | `^6.5.3 \|\| ^7.4.0` | **7.8.2** | 7.8.2 | none — 7.8.2 is both newest and viable (no RxJS 8 line). |
| zone.js | `~0.15.0 \|\| ~0.16.0` | **0.16.2** | 0.16.2 | none. |
| jasmine-core | (via Angular karma builder scaffold) | **~6.3.0** | 7.0.2 | **7.0.2 excluded** — the Angular 22 karma builder scaffolds jasmine-core `~6.3.0`; jasmine 7 is not yet validated against it. |

**Newest viable combination:** `{ Node 24 LTS, npm ≥ 8, Angular 22.1.4/22.1.6, TypeScript
6.0.3, RxJS 7.8.2, zone.js 0.16.2, jasmine-core 6.3.x, karma 6.4.x }`. The individually
newest TypeScript (7.0.2) and jasmine-core (7.0.2) are deliberately **not** taken because
Angular 22 does not support them; taking them would break `ng build`/`ng test`.

## Deprecated / removed / replaced components

None of these may be silently dropped — each carries a replacement or an explicit
decision:

- **`@angular/http` (row 7) — REMOVED.** Deprecated in Angular 4.3/5, removed in **Angular
  8**. Replacement: `HttpClientModule` / `HttpClient` from `@angular/common/http`. **No
  capability is lost** — `src/app/app.module.ts` already imports `HttpClientModule` and all
  four services (`library`, `directory`, `search`, `preference`) already inject
  `HttpClient`; `@angular/http` is an **unused** dependency and can be dropped outright at
  the v8 hop.
- **`protractor` + `@types/jasminewd2` + (e2e) `ts-node` (rows 27–29) — DEPRECATED /
  END-OF-LIFE.** The Protractor project reached end-of-life (final 7.0.0) and Angular
  dropped it from new projects at v12/13. It is **not** silently removed: replacement is a
  **decision** between Cypress, Playwright, or WebdriverIO (or dropping e2e), tracked as
  **Unit 3**. The single e2e spec under `frontend/e2e/src` and `e2e/protractor.conf.js`
  must be reproduced on the chosen tool (or its removal explicitly recorded).
- **`tslint` + `codelyzer` (rows 30–31) — DEPRECATED.** TSLint reached end-of-life
  (Dec 2020); `codelyzer` is a TSLint ruleset with no supported successor of its own.
  Replacement: **`angular-eslint`** (latest **22.2.0**, version-aligned to Angular 22),
  which supplies the Angular-specific lint rules and the ESLint builder that replaces
  `@angular-devkit/build-angular:tslint`. Migration touches `frontend/tslint.json`
  (→ ESLint config) and the `lint` targets in `frontend/angular.json`. Tracked as
  **Unit 2**.
- **`karma-coverage-istanbul-reporter` (row 23) — SUPERSEDED.** Modern Angular scaffolds
  **`karma-coverage`** instead. Replacement, not a drop — coverage reporting is preserved.
- **`core-js` (row 15) — OBSOLETE polyfill.** Modern Angular targets evergreen browsers and
  ships only `zone.js` in `polyfills`; the `core-js/es7/reflect` import is removed. No
  capability lost for supported browsers.

## Risk-based backlog units

Coupling rule: parts appear in the same unit **only when they must move together**;
independent parts are separated so they can ship on their own schedules. All latest-stable
values referenced below are as of **2026-08-31** per `00-baseline.md`. Baseline
execution-readiness is stated per unit against `00-baseline.md`, whose frontend baseline is
**RED**.

### Unit 1 — Angular 6 → 22 stepwise framework + toolchain migration (COUPLED · high risk)

- **Members:** every `@angular/*` runtime package (core, common, compiler, animations,
  forms, router, platform-browser[-dynamic]) · `@angular/compiler-cli` ·
  `@angular/language-service` · `@angular/cdk` · `@angular/material` · `@angular/cli` ·
  `@angular-devkit/build-angular` · `typescript` 2.9→6.0.3 · `rxjs` 6.2→7.8.2 · `zone.js`
  0.8→0.16.2 · the karma/jasmine unit-test stack (`karma`, `karma-jasmine`,
  `karma-chrome-launcher`, `karma-jasmine-html-reporter`, `karma-coverage-istanbul-reporter`
  → `karma-coverage`, `jasmine-core`, `@types/jasmine`, `jasmine-spec-reporter`) ·
  `@types/node` · the `tsconfig.json` compile target · the Node 24 engine bump + new
  `.nvmrc` · **removal** of `@angular/http` and `core-js`.
- **Outcome:** the app builds (`ng build`), unit-tests (`ng test`), lints and serves on
  **Angular 22.1.x / TypeScript 6.0.3 / RxJS 7.8.2 / zone.js 0.16.2** under **Node 24 LTS**,
  reached via the sequential `ng update` path — restoring a green frontend baseline on a
  supported Node.
- **Scope (files):** `frontend/package.json` (deps/devDeps/engines), new `frontend/.nvmrc`,
  `frontend/package-lock.json`, `frontend/angular.json` (builder/schematic updates),
  `frontend/tsconfig*.json` and `src/tsconfig.*.json`, `src/polyfills.ts` (drop `core-js`,
  fix `zone.js` import path), `src/test.ts` (`zone.js/dist/zone-testing` → `zone.js/testing`),
  `src/environments/*`, and any RxJS/Material API call sites the per-hop schematics rewrite
  (e.g. RxJS pipe operators, Material component renames).
- **Prerequisites:** a restoration path for the RED frontend baseline (**[FRONTEND-NODE]** /
  **[FRONTEND-NVM]**) — either (a) install Node ≤ 16 to reproduce the historical green
  Angular-6 build as the "before" reference, **or** (b) treat this unit as the restoration
  itself, upgrading to Angular 22 on the available Node 24 and verifying the one existing
  spec afterwards. A path must be chosen before execution.
- **Observable completion criteria:** on Node 24, `npm ci` + `ng build` succeed;
  `ng test --watch=false` compiles and the existing spec
  (`src/app/torrent/downloads/downloads.component.spec.ts`) passes; `ng serve` starts and
  the app loads; `@angular/http` and `core-js` no longer appear in `package.json`.
- **Risks:** 16 sequential major migrations (Ivy at v9, esbuild builder at v17, control-flow,
  Material component/API renames, RxJS 6→7 operator changes); each hop can surface
  template/DI/build breakages; `zone.js`/polyfill import-path changes; the project has only
  **one** unit spec, so automated regression coverage during the journey is thin (manual
  smoke-testing of each screen is required).
- **Why coupled (cannot proceed independently):** Angular's **version matrix** binds
  `@angular/core` ↔ `@angular/cli` ↔ `@angular-devkit/build-angular` ↔ `typescript` ↔ `rxjs`
  ↔ `zone.js` at every major, and CDK/Material share Angular's major version exactly. The
  **stepwise-major** rule forces them through each major together via `ng update`, which
  also rewrites the karma/jasmine devDeps and the tsconfig compile target in the same pass.
  The Node bump is coupled because Angular 22's engines require Node ≥ 24.15/26. Splitting
  any of these ships a non-building intermediate state.
- **Baseline / readiness:** **NOT execution-ready — blocked on baseline restoration**
  ([FRONTEND-NODE]/[FRONTEND-NVM]). There is no green "before" to measure against; a
  restoration path (a or b above) must be chosen first. Note that completing this unit to
  Angular 22 on Node 24 is itself restoration option (b).
- **Decision note:** Node **24 LTS** is targeted over Node 26 (also matrix-allowed) for LTS
  stability and because the baseline host already runs v24.16.0.

### Unit 2 — Lint migration: TSLint + Codelyzer → angular-eslint (COUPLED · moderate risk)

- **Members:** `tslint` (removed) · `codelyzer` (removed) · **`angular-eslint` 22.2.0**
  (added) · `frontend/tslint.json` → ESLint config · the `lint` targets in
  `frontend/angular.json` (`@angular-devkit/build-angular:tslint` → ESLint builder).
- **Outcome:** `ng lint` runs on ESLint via `angular-eslint`, with the Angular-specific
  rules that Codelyzer used to provide, and no deprecated TSLint on the toolchain.
- **Scope:** `frontend/package.json` (drop tslint/codelyzer, add angular-eslint),
  `frontend/tslint.json` (replaced by `eslint.config.*`/`.eslintrc`),
  `frontend/angular.json` lint builders.
- **Prerequisites:** **Unit 1** — `angular-eslint` 22 requires Angular 22, and the TSLint
  builder is removed from modern `@angular-devkit/build-angular`; this migration only makes
  sense once the framework has moved. Also inherits Unit 1's baseline block.
- **Observable completion criteria:** `ng lint` exits cleanly on the ESLint stack; TSLint
  and Codelyzer are absent from `package.json`; the equivalent Angular rules are enforced.
- **Risks:** rule-set is not 1:1 between TSLint/Codelyzer and ESLint/angular-eslint — some
  rules have no equivalent and lint output will differ; config format change.
- **Why coupled (cannot proceed independently):** `codelyzer` is a **TSLint plugin**, so it
  cannot be upgraded or kept once TSLint is dropped; both are deprecated and are replaced by
  the single `angular-eslint` successor together. Removing one without the other leaves
  either a broken lint config or an unenforced Angular rule-set.
- **Baseline / readiness:** **NOT execution-ready — blocked on baseline restoration** (via
  its Unit 1 prerequisite).

### Unit 3 — E2E replacement: retire Protractor (DECISION · moderate risk)

- **Members:** `protractor` (removed) · `@types/jasminewd2` (removed) · e2e `ts-node`
  (decision-bound) · `frontend/e2e/protractor.conf.js`, `frontend/e2e/tsconfig.e2e.json`,
  the e2e spec(s) under `frontend/e2e/src` · the `frontend-e2e` `e2e`/`lint` targets in
  `frontend/angular.json`.
- **Outcome:** a recorded decision on the e2e tool — **Cypress**, **Playwright**,
  **WebdriverIO**, or dropping e2e entirely — with the existing e2e coverage reproduced on
  the chosen tool or its removal explicitly documented.
- **Scope:** the `frontend/e2e` tree, `frontend/angular.json` e2e wiring, and the e2e
  devDeps in `frontend/package.json`.
- **Prerequisites:** conceptually gated by **Unit 1** (Angular dropped built-in Protractor
  support; a modern e2e runner integrates with the upgraded CLI). Inherits Unit 1's baseline
  block.
- **Observable completion criteria:** a recorded tool decision; the chosen runner's smoke
  e2e executes against `ng serve` (or, if e2e is dropped, that removal is documented and the
  `frontend-e2e` project is deleted from `angular.json`).
- **Risks:** e2e specs must be rewritten for the new runner's API; CI wiring changes;
  `directConnect`/webdriver-manager assumptions in `protractor.conf.js` do not carry over.
- **Why a decision, not a scheduled upgrade:** Protractor is end-of-life with **no drop-in
  successor** — the replacement is a product/tooling choice a human must make on cost vs.
  benefit, not an automatable bump. `@types/jasminewd2` and the e2e `ts-node` are grouped
  here because they exist **only** to serve the Protractor/Jasmine e2e stack and must be
  disposed of with it.
- **Baseline / readiness:** **NOT execution-ready — decision ticket, and blocked on baseline
  restoration** (via Unit 1).

### Unit 4 — sass (dart-sass) 1.63.6 → 1.103.1 (INDEPENDENT · low risk)

- **Outcome:** the Sass compiler used for the app's `.scss` styles sits at its latest stable
  release.
- **Scope:** `frontend/package.json` version string only (no `.scss` source change expected;
  the project already uses `dart-sass`, not the retired `node-sass`).
- **Prerequisites:** none of its own beyond a green frontend baseline; it does **not** need
  Unit 1 to bump, but cannot be *verified* until the baseline builds (so it inherits the
  baseline block for readiness).
- **Observable completion criteria:** `ng build` still compiles all `.scss`
  (`src/styles.scss` and component styles) without new errors; rendered styling unchanged.
- **Risks:** low — Sass 1.80+ emits **deprecation warnings** for the legacy `@import` rule
  (slated for removal in a future Sass 3.0); those are warnings only at 1.103.1 and do not
  block the build, but flag a future `@import` → `@use`/`@forward` migration.
- **Coupling note:** **independent** — groups with no other part; it is separated precisely
  because it is a drop-in dev-tool bump with no matrix tie to Angular.
- **Baseline / readiness:** **NOT execution-ready to verify — blocked on baseline
  restoration** (needs a building frontend to confirm), though the version bump itself is
  Angular-independent.

## Baseline linkage summary

- The **frontend baseline is RED** (`00-baseline.md`): no green `ng build`/`ng test` exists
  on the available Node runtime, and the `nvm`/`.nvmrc` workflow is not reproducible. Per the
  restoration rule, **every unit above is marked NOT execution-ready — blocked on the
  baseline-restoration prerequisites [FRONTEND-NODE] and [FRONTEND-NVM]**.
- **Unit 1 is both blocked by and the resolver of** those prerequisites: restoration option
  (a) installs Node ≤ 16 to reproduce the historical green Angular-6 build as a "before"
  reference; option (b) makes the Angular-22-on-Node-24 upgrade itself the restoration
  (Angular 22 supports the baseline's Node v24.16.0). Creating `frontend/.nvmrc` (row 2)
  additionally clears [FRONTEND-NVM]. A path must be chosen before Unit 1 executes.
- **Units 2, 3, and 4 depend on Unit 1** (angular-eslint 22 needs Angular 22; Protractor's
  successor integrates with the upgraded CLI; the sass bump can only be verified once the
  build is green), so they are blocked transitively until Unit 1 restores a green baseline.
- **Unit 3** is additionally a **decision** ticket (choose the e2e replacement) before any
  code moves.
