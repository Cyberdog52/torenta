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
| 1 | 6.1.10 | Angular host: 22.19.0 / 10.9.3; Playwright target: Node 24 | 12 desktop/mobile characterization tests pass; legacy AOT defect recorded | This phase commit |

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
