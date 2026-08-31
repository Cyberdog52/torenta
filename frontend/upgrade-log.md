# Angular Upgrade Log

This log records the reproducibility decisions and verification results for
[`plan.md`](./plan.md). Update it after every migration checkpoint.

## Frozen Endpoint

Frozen on 2026-08-31 against the public npm registry (`https://registry.npmjs.org/`).

| Tool | Version |
| --- | --- |
| Node.js | 24.20.0 LTS |
| npm | 11.19.0 |
| Angular core | 22.1.4 |
| Angular CLI | 22.1.6 |
| Angular Material/CDK | 22.1.4 |
| TypeScript | 6.0.3 |
| RxJS | 7.8.2 |

TypeScript 7 and Node 26 are deferred until Angular officially supports them and
Node 26 is an LTS release. No compatibility checks may be bypassed with force
flags.

## Reproducibility Decisions

- The committed npm lockfile is the dependency source of truth at each checkpoint.
- Package tarballs will not be mirrored initially. Revisit this before execution
  if the public npm registry is not an acceptable long-term dependency.
- Historical Node runtimes run only in disposable Docker containers without
  credentials or production access.
- Each green Angular major is preserved in a separate Git commit.
- Baseline artifacts are generated under `frontend/.upgrade-artifacts/` and are
  not committed unless a later phase explicitly identifies a stable artifact
  that belongs in source control.
- The in-place migration stops rather than using `--force`,
  `--legacy-peer-deps`, skipped schematics, or package-internal edits. The
  fallback is a clean Angular 22 workspace with application code ported into it.

## Browser Decision

Angular 22's May 2026 web-platform Baseline is the proposed browser policy. It
requires product approval before final acceptance. Desktop and mobile Playwright
projects will exercise Chromium initially; any additional product-supported
browsers must be added before final qualification.

## Build And Deployment

The frontend is currently served separately from the Spring Boot backend:

- Angular writes production artifacts to `frontend/dist/frontend`.
- `build.gradle` has no frontend build, copy, or packaging task.
- No CI workflow or deployment configuration consumes the frontend artifact.
- The repository documentation only describes running `npm start` locally.

The Angular application-builder migration is expected to write browser assets to
`frontend/dist/frontend/browser`. Since no automated consumer exists today, the
deployment owner must document the external/manual deployment process and update
it before final acceptance.

## Checkpoint Record

| Phase | Angular | Node/npm | Result | Commit |
| --- | --- | --- | --- | --- |
| 0 | 6.1.10 | Host audit: 22.19.0 / 10.9.3 | Decisions frozen; Docker 29.7.2 available | `1d5c8f8` |
| 1 | 6.1.10 | Angular host: 22.19.0 / 10.9.3; Playwright target: Node 24 | 12 desktop/mobile characterization tests pass; legacy AOT defect recorded | `5585e72` |
| 2 | 6.1.10 | npm 6.13.4 on host Node 22.19.0 | Exact dependencies and npm v1 lockfile; lint, 3 unit tests, AOT build, and 12 Playwright tests pass | `369f0fb` |
| 3 | 7.2.16 | npm 6.13.4 on host Node 22.19.0 | Schematics reviewed; exact tree, lint, 3 unit tests, AOT build, and 12 Playwright tests pass | `77e65fd` |
| 4 | 8.2.14 | npm 6.13.4 on host Node 22.19.0 | Browser migration reviewed; exact tree, lint, 3 unit tests, AOT build, and 12 Playwright tests pass | `ce194ca` |
| 5 | 9.1.13 | npm 6.13.4 on host Node 22.19.0 | Ivy migration reviewed; exact tree, lint, 3 unit tests, AOT build, and 12 Playwright tests pass | This phase commit |

## Angular 6 Baseline

Recorded on 2026-08-31 before dependency normalization:

- Routes: `/` redirects to `/search`; `/search`, `/downloads`, and
  `/preferences` are eager routes; unknown routes render the not-found page.
- API: the development server proxies `/api/*` to `http://localhost:8080`.
- Environments: production replaces `src/environments/environment.ts` with
  `src/environments/environment.prod.ts`.
- Output: the legacy browser builder writes to `frontend/dist/frontend`.
- `npm ci --ignore-scripts` completes on the unsupported Node 22 host and reports
  144 known legacy dependency vulnerabilities (8 low, 30 moderate, 80 high, 26
  critical). These are addressed through the planned upgrades, not `npm audit fix`.
- TSLint passes on the Node 22 host.
- Karma runs three tests successfully on the Node 22 host when webpack 4 is
  given the temporary `NODE_OPTIONS=--openssl-legacy-provider` compatibility
  option.
- The production AOT build reaches template compilation with that compatibility
  option, then fails because
  `download-detail.component.html` binds `[mode]="determinate"` to an undeclared
  property. The Angular 6 normalization phase will correct this to a literal.
- Without the temporary OpenSSL option, build and test fail before compilation
  because webpack 4 uses an OpenSSL algorithm disabled by modern Node versions.
- Docker is usable, but Docker Hub Node image pulls are blocked by invalid daemon
  registry credentials. A cached Microsoft Node 24 image is available; exact
  historical runtime verification remains pending.
- The independent Playwright suite passes 12 tests across desktop and mobile
  Chromium. It covers navigation, preferences, direct torrent downloads,
  movie/series expansion, delayed keyup, polling order, completion, and
  notifications. Test screenshots are retained as ignored run artifacts.

## Angular 6 Normalized Checkpoint

- All direct dependencies are exact; Angular packages resolve to `6.1.10`, CLI
  to `6.2.9`, Material/CDK to `6.4.7`, and TypeScript to `2.9.2`.
- Unused `@angular/http` was removed after confirming there are no source imports.
- Sass is temporarily pinned to `1.32.13` for historical runtime compatibility.
- npm `6.13.4` generated lockfile version 1 and reproduced 1,323 installed packages
  with `npm ci --ignore-scripts`.
- `npm ls --depth=0`, TSLint, three Karma tests, production AOT build, and all 12
  characterization tests pass.
- The baseline AOT defect was corrected by making the progress-bar mode the
  literal `determinate` rather than binding an undeclared component property.
- Exact Node 8 verification remains unavailable on this ARM64 host. npm 6 ran
  under Node 22, and webpack commands used the temporary
  `NODE_OPTIONS=--openssl-legacy-provider` option. No production configuration
  was changed to retain that compatibility option.

## Angular 7 Checkpoint

- Core is `7.2.16`, CLI/build tooling is `7.3.10`/`0.13.10`,
  Material/CDK is `7.3.7`, TypeScript is `3.2.4`, and RxJS is `6.6.7`.
- CLI schematics removed obsolete reflect polyfill imports and enabled TypeScript
  import helpers. Direct dependencies remain exact.
- Codelyzer was aligned to `5.2.2`, and renamed metadata-property rules replaced
  three removed legacy rule names so lint runs without configuration warnings.
- `npm ls --depth=0`, lint, three Karma tests, production AOT build, and all 12
  characterization tests pass.
- The archived schematic's optional Node Sass installation cannot build on the
  modern ARM64 host; the application uses the separately pinned Dart Sass package,
  and clean installs are performed with lifecycle scripts disabled at historical
  checkpoints.

## Angular 8 Checkpoint

- Core is `8.2.14`, CLI/build tooling is `8.3.29`/`0.803.29`,
  Material/CDK is `8.2.3`, TypeScript is `3.5.3`, and zone.js is `0.9.1`.
- CLI schematics moved Browserslist configuration to the workspace root and
  updated TypeScript library targets for differential loading.
- `@angular/http` remains absent.
- `npm ls --depth=0`, lint, three Karma tests, production AOT differential
  builds, and all 12 characterization tests pass.

## Angular 9 Checkpoint

- Core is `9.1.13`, CLI/build tooling is `9.1.15`/`0.901.15`,
  Material/CDK is `9.2.4`, TypeScript is `3.8.3`, and zone.js is `0.10.3`.
- The archived updater attempted to bootstrap today's `latest` CLI even with an
  exact local CLI. Official Angular 9 migrations were therefore invoked directly
  from the exact installed core and CLI migration collections.
- Ivy compilation exposed global ambient TMDB DTOs. The affected DTOs now use
  explicit exports and imports without changing their API shapes.
- All Material root-barrel imports were replaced with supported component entry
  points, and deprecated `TestBed.get` calls now use `TestBed.inject`.
- `npm ls --depth=0`, warning-free lint, three Karma tests, production Ivy AOT
  differential builds, and all 12 characterization tests pass.
