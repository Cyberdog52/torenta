# Angular 6 to Angular 22 Upgrade Plan

Source: [`upgrade.md`](./upgrade.md)  
Matrix frozen: 2026-08-31  
Overall risk: High  
Status: In progress

## Checklist Conventions

- [ ] A checkbox is complete only when its work and verification both pass.
- [ ] Complete phases in order unless a phase explicitly says it can run independently.
- [ ] Keep dependency upgrades separate from application modernization wherever possible.
- [ ] Record commands, versions, generated migrations, failures, and decisions in an upgrade log.
- [ ] Do not use `--force`, `--legacy-peer-deps`, `--allow-dirty`, `npm audit fix`, or unrestricted `latest` ranges.
- [ ] Do not continue past unresolved peer dependencies, mixed Angular majors, skipped migrations, or unexplained test regressions.

## Target Versions

- [ ] Confirm Node.js `24.20.0` LTS as the final production runtime.
- [ ] Confirm npm `11.19.0` as the final package manager.
- [ ] Confirm Angular core `22.1.4`.
- [ ] Confirm Angular CLI `22.1.6`.
- [ ] Confirm Angular Material and CDK `22.1.4`.
- [ ] Confirm TypeScript `6.0.3`; do not use TypeScript 7 until Angular officially supports it.
- [ ] Confirm RxJS `7.8.2`.
- [ ] Confirm `zone.js` will be removed only after zoneless compatibility is proven.
- [ ] Recheck the Angular compatibility matrix and package availability if execution starts significantly after 2026-08-31.

## Stop And Fallback Rule

- [ ] Create a clean Angular 22 reference application using the approved target versions.
- [ ] Keep the reference application outside the production source tree or in an explicitly temporary directory.
- [ ] Compare generated Angular 22 configuration, scripts, builders, TypeScript settings, and bootstrap code with the migrated application.
- [ ] Stop the in-place ladder if an archived migration requires force flags, unresolved peers, skipped schematics, or package-internal edits.
- [ ] If the ladder stops, port the 14 declarations, six services, routes, styles, assets, and tests into the clean Angular 22 reference application.
- [ ] Apply the same final verification and acceptance gates to either migration route.

## Phase 0: Decisions And Reproducibility

- [x] Record the approved target matrix and the npm registry used for the migration.
- [x] Decide whether package tarballs and checksums must be mirrored for long-term reproducibility.
- [ ] Confirm Angular 22's May 2026 browser Baseline with product stakeholders.
- [ ] Document supported desktop and mobile browser versions.
- [x] Identify how the current `dist/frontend` output is packaged and deployed.
- [x] Identify every repository consumer that must be updated if the output changes to `dist/frontend/browser`; no automated repository consumer currently exists.
- [x] Decide where disposable migration environments and baseline artifacts will be stored.
- [x] Ensure EOL Node runtimes run only in isolated disposable environments without credentials or production access.
- [x] Define one commit or tag boundary for each green Angular major.

## Phase 1: Behavioral Baseline

- [ ] Record the current Node, npm, Angular CLI, TypeScript, RxJS, Material, CDK, and Sass versions.
- [ ] Archive the current `package-lock.json` and record that it uses lockfile version 3.
- [ ] Record the exact installed dependency tree with `npm ls` and preserve any known errors.
- [ ] Produce and archive the current production build, or document why it cannot be built.
- [ ] Record all current lint, unit-test, and Protractor results without hiding pre-existing failures.
- [ ] Document the current routes, API endpoints, proxy behavior, environment files, and build output layout.
- [ ] Create a Playwright harness that runs independently under Node 24 rather than inside the Angular 6 dependency tree.
- [ ] Add deterministic API mocks for repeatable browser tests.
- [ ] Add a separate optional API-backed smoke suite for environments with credentials.
- [ ] Add characterization coverage for search.
- [ ] Add characterization coverage for movie and series expansion.
- [ ] Add characterization coverage for preferences load and save.
- [ ] Add characterization coverage for starting a torrent.
- [ ] Add characterization coverage for torrent polling and downloads.
- [ ] Add characterization coverage for notifications.
- [ ] Add characterization coverage for routing and direct navigation.
- [ ] Add characterization coverage for delayed keyup behavior.
- [ ] Capture desktop and mobile screenshots of Material-heavy screens.
- [ ] Verify the baseline Playwright suite against the Angular 6 application.

## Phase 2: Normalize Angular 6

Runtime: Node `8.17.0`, npm `6.13.4` in an isolated migration environment.

- [ ] Pin Angular core packages to `6.1.10`.
- [ ] Pin Angular CLI to `6.2.9`.
- [ ] Pin Angular Material and CDK to `6.4.7`.
- [ ] Pin TypeScript to `2.9.2`.
- [ ] Pin all other direct dependencies exactly by removing `^` and `~` ranges.
- [ ] Temporarily pin Dart Sass to `1.32.13` so it supports the historical runtime.
- [ ] Confirm whether `@angular/http` has any imports or runtime use.
- [ ] Remove `@angular/http` before Angular 8 if it is unused; otherwise migrate usage to `HttpClient` first.
- [ ] Generate a canonical npm 6 lockfile for the normalized checkpoint.
- [ ] Verify a clean install from the canonical lockfile.
- [ ] Verify `npm ls --depth=0` has no invalid or missing direct dependencies.
- [ ] Verify the production build.
- [ ] Verify the existing unit tests.
- [ ] Run lint and legacy e2e only if their current scripts are functional; otherwise retain the recorded baseline failure.
- [ ] Run the independent Playwright characterization suite.
- [ ] Tag or commit the green Angular 6 checkpoint.

## Standard Major-Upgrade Procedure

Apply this procedure to each Angular major in Phases 3 through 18.

- [ ] Activate the exact Node and npm versions assigned to the transition.
- [ ] Start from the preceding clean, green checkpoint.
- [ ] Upgrade Angular CLI and core with the exact target versions.
- [ ] Review all schematic output, source changes, warnings, and generated TODOs.
- [ ] Resolve compile, peer-dependency, and migration errors without force flags.
- [ ] Upgrade Material and CDK to the same target major and exact planned patches.
- [ ] Review Material migration output separately from core/CLI migration output.
- [ ] Pin all direct dependency versions exactly.
- [ ] Regenerate the lockfile using the checkpoint's assigned npm version.
- [ ] Remove installed dependencies and verify the checkpoint with a clean `npm ci`.
- [ ] Run `npx ng version` and verify there are no mixed Angular majors.
- [ ] Run `npm ls --depth=0` and resolve invalid or missing peers.
- [ ] Run lint if a supported lint target exists at this checkpoint.
- [ ] Run unit tests in non-watch mode.
- [ ] Run the production build.
- [ ] Run repository e2e tests if a supported e2e target exists at this checkpoint.
- [ ] Run the independent Playwright characterization suite.
- [ ] Review desktop and mobile screenshots for Material regressions.
- [ ] Confirm there are no unreviewed migration TODOs, skipped tests, or focused tests.
- [ ] Record output paths, bundle sizes, warnings, and known deviations.
- [ ] Tag or commit the green checkpoint before starting the next major.

Command pattern; substitute exact versions from the checkpoint:

```bash
npx @angular/cli@<exact-cli> update \
  @angular/cli@<exact-cli> \
  @angular/core@<exact-core>

npx @angular/cli@<exact-cli> update \
  @angular/material@<exact-material>
```

## Phase 3: Angular 7

Runtime: Node `8.17.0`, npm `6.13.4`.

- [ ] Upgrade Angular core to `7.2.16`.
- [ ] Upgrade Angular CLI to `7.3.10`.
- [ ] Upgrade Material and CDK to `7.3.7`.
- [ ] Upgrade TypeScript to `3.2.4`.
- [ ] Apply and review CLI workspace migrations.
- [ ] Complete the standard major-upgrade procedure.

## Phase 4: Angular 8

Runtime: Node `10.24.1`, npm `6.14.12`.

- [ ] Upgrade Angular core to `8.2.14`.
- [ ] Upgrade Angular CLI to `8.3.29`.
- [ ] Upgrade Material and CDK to `8.2.3`.
- [ ] Upgrade TypeScript to `3.5.3`.
- [ ] Confirm `@angular/http` is absent.
- [ ] Review CLI polyfill and browser-target migrations.
- [ ] Complete the standard major-upgrade procedure.

## Phase 5: Angular 9

Runtime: Node `10.24.1`, npm `6.14.12`.

- [ ] Upgrade Angular core to `9.1.13`.
- [ ] Upgrade Angular CLI to `9.1.15`.
- [ ] Upgrade Material and CDK to `9.2.4`.
- [ ] Upgrade TypeScript to `3.8.3`.
- [ ] Validate Ivy compilation and runtime behavior.
- [ ] Replace imports from the `@angular/material` root barrel with component entry points.
- [ ] Replace deprecated `TestBed.get` calls.
- [ ] Complete the standard major-upgrade procedure.

## Phase 6: Angular 10

Runtime: Node `10.24.1`, npm `6.14.12`.

- [ ] Upgrade Angular core to `10.2.5`.
- [ ] Upgrade Angular CLI to `10.2.4`.
- [ ] Upgrade Material and CDK to `10.2.7`.
- [ ] Upgrade TypeScript to `4.0.8`.
- [ ] Resolve newly surfaced TypeScript and template type errors without weakening checks globally.
- [ ] Inventory ambient DTO declarations that need explicit exports and imports.
- [ ] Complete the standard major-upgrade procedure.

## Phase 7: Angular 11

Runtime: Node `10.24.1`, npm `6.14.12`.

- [ ] Upgrade Angular core to `11.2.14`.
- [ ] Upgrade Angular CLI to `11.2.19`.
- [ ] Upgrade Material and CDK to `11.2.13`.
- [ ] Upgrade TypeScript to `4.1.6`.
- [ ] Pin RxJS to `6.6.7`.
- [ ] Resolve remaining typing and ambient DTO issues required by this checkpoint.
- [ ] Complete the standard major-upgrade procedure.

## Phase 8: Angular 12

Runtime: Node `12.22.12`, npm `6.14.16`.

- [ ] Upgrade Angular core to `12.2.17`.
- [ ] Upgrade Angular CLI to `12.2.18`.
- [ ] Upgrade Material and CDK to `12.2.13`.
- [ ] Upgrade TypeScript to `4.3.5`.
- [ ] Keep RxJS at `6.6.7` until the Angular 12 checkpoint is green.
- [ ] Complete the standard major-upgrade procedure with RxJS `6.6.7`.
- [ ] Replace TSLint and Codelyzer with Angular ESLint as an isolated tooling change.
- [ ] Verify lint behavior and resolve configuration or rule-conversion gaps.
- [ ] Upgrade RxJS separately from `6.6.7` to `7.8.2`.
- [ ] Resolve RxJS compatibility and type errors without changing application behavior.
- [ ] Repeat clean install, lint, unit, build, and Playwright checks after the RxJS upgrade.
- [ ] Tag or commit the separate green RxJS checkpoint.

## Phase 9: Angular 13

Runtime: Node `14.21.3`, npm `6.14.18`.

- [ ] Upgrade Angular core to `13.4.0`.
- [ ] Upgrade Angular CLI to `13.3.11`.
- [ ] Upgrade Material and CDK to `13.3.9`.
- [ ] Upgrade TypeScript to `4.6.4`.
- [ ] Remove the stale Protractor project and dependencies.
- [ ] Make Playwright the supported e2e suite and expose it through a documented script.
- [ ] Complete the standard major-upgrade procedure.

## Phase 10: Angular 14

Runtime: Node `16.20.2`, npm `8.19.4`.

- [ ] Upgrade Angular core to `14.3.0`.
- [ ] Upgrade Angular CLI to `14.2.13`.
- [ ] Upgrade Material and CDK to `14.2.7`.
- [ ] Upgrade TypeScript to `4.8.4`.
- [ ] Remove obsolete tilde-prefixed Material Sass imports.
- [ ] Remove unused legacy Material Sass variables.
- [ ] Retain the indigo-pink prebuilt theme to avoid an unrelated visual redesign.
- [ ] Complete the standard major-upgrade procedure.

## Phase 11: Angular 15

Runtime: Node `16.20.2`, npm `8.19.4`.

- [ ] Upgrade Angular core to `15.2.10`.
- [ ] Upgrade Angular CLI to `15.2.11`.
- [ ] Upgrade Material and CDK to `15.2.9`.
- [ ] Upgrade TypeScript to `4.9.5`.
- [ ] Run and review the Material MDC migration.
- [ ] Resolve every generated `TODO(mdc-migration)`.
- [ ] Review chips, form fields, snackbars, progress components, tables, and expansion panels manually.
- [ ] Replace `::ng-deep` and private Material class selectors with supported APIs or component-owned CSS.
- [ ] Add Material component harness coverage where it reduces selector fragility.
- [ ] Approve desktop and mobile visual snapshots before continuing.
- [ ] Complete the standard major-upgrade procedure.

## Phase 12: Angular 16

Runtime: Node `16.20.2`, npm `8.19.4`.

- [ ] Upgrade Angular core to `16.2.12`.
- [ ] Upgrade Angular CLI to `16.2.16`.
- [ ] Upgrade Material and CDK to `16.2.14`.
- [ ] Upgrade TypeScript to `5.1.6`.
- [ ] Complete the dependency upgrade and standard gate before modernization.
- [ ] Introduce `takeUntilDestroyed` only for necessary imperative subscriptions.
- [ ] Introduce signal/RxJS interoperability only in behavior-preserving changes.
- [ ] Keep HTTP orchestration, cancellation, retry, and polling in RxJS.
- [ ] Repeat tests after each subscription or interop refactor.

## Phase 13: Angular 17

Runtime: Node `18.20.8`, npm `10.8.2`.

- [ ] Upgrade Angular core to `17.3.12`.
- [ ] Upgrade Angular CLI to `17.3.17`.
- [ ] Upgrade Material and CDK to `17.3.10`.
- [ ] Upgrade TypeScript to `5.4.5`.
- [ ] Complete the dependency upgrade and standard gate before template modernization.
- [ ] Run the built-in control-flow migration.
- [ ] Convert remaining `*ngIf` and `*ngFor` usage to `@if` and `@for` where appropriate.
- [ ] Review every `@for` tracking expression and prefer stable domain identifiers over `$index`.
- [ ] Repeat unit, build, Playwright, and visual checks after control-flow changes.

## Phase 14: Angular 18

Runtime: Node `20.20.2`, npm `10.8.2`.

- [ ] Upgrade Angular core to `18.2.14`.
- [ ] Upgrade Angular CLI to `18.2.21`.
- [ ] Upgrade Material and CDK to `18.2.14`.
- [ ] Upgrade TypeScript to `5.5.4`.
- [ ] Complete the dependency upgrade and standard gate before changing builders.
- [ ] Run and review the application-builder migration.
- [ ] Replace the obsolete browser builder with Angular's application builder.
- [ ] Verify assets, styles, proxy behavior, environment replacements, and source maps.
- [ ] Verify the production output path, including the expected `dist/frontend/browser` directory.
- [ ] Update documented packaging or deployment consumers for the new output path.
- [ ] Repeat clean install, build, Playwright, artifact, and deployment smoke checks.

## Phase 15: Angular 19

Runtime: Node `22.23.2`, npm `10.9.8`.

- [ ] Upgrade Angular core to `19.2.25`.
- [ ] Upgrade Angular CLI to `19.2.27`.
- [ ] Upgrade Material and CDK to `19.2.19`.
- [ ] Upgrade TypeScript to `5.8.3`.
- [ ] Complete the dependency upgrade and standard gate before architecture changes.
- [ ] Run the standalone-component migration in its recommended stages.
- [ ] Convert root bootstrap to `bootstrapApplication`.
- [ ] Replace `AppModule` and `MaterialModule` with standalone component imports.
- [ ] Replace module router and HTTP setup with `provideRouter` and `provideHttpClient`.
- [ ] Lazy-load the `/search`, `/downloads`, and `/preferences` route components.
- [ ] Run the signal-input migration in safe mode.
- [ ] Convert required inputs to `input.required()` where behavior and API contracts remain clear.
- [ ] Repeat lint, unit, build, Playwright, routing, and visual checks after architecture changes.

## Phase 16: Angular 20

Runtime: Node `22.23.2`, npm `10.9.8`.

- [ ] Upgrade Angular core to `20.3.30`.
- [ ] Upgrade Angular CLI to `20.3.35`.
- [ ] Upgrade Material and CDK to `20.2.14`.
- [ ] Upgrade TypeScript to `5.9.3`.
- [ ] Complete the dependency upgrade and standard gate before change-detection work.
- [ ] Adopt `ChangeDetectionStrategy.OnPush` incrementally.
- [ ] Use writable signals for synchronous local UI state where they simplify ownership.
- [ ] Use `computed()` for derived state.
- [ ] Avoid effects that copy or propagate application state.
- [ ] Expose long-lived observable state through `toSignal` or `AsyncPipe` where appropriate.
- [ ] Prove zoneless compatibility while zone.js is still available as a rollback point.
- [ ] Repeat unit, fake-timer, Playwright, and visual checks under zoneless-compatible configuration.

## Phase 17: Angular 21

Runtime: Node `22.23.2`, npm `10.9.8`.

- [ ] Upgrade Angular core to `21.2.22`.
- [ ] Upgrade Angular CLI to `21.2.22`.
- [ ] Upgrade Material and CDK to `21.2.14`.
- [ ] Keep TypeScript at `5.9.3` unless the exact Angular package peer range requires another compatible patch.
- [ ] Finish deprecated provider and bootstrap cleanup.
- [ ] Remove zone.js only after the zoneless suite passes.
- [ ] Verify timers, events, dialogs, HTTP completion, notifications, and Material overlays without zone.js.
- [ ] Complete the standard major-upgrade procedure.

## Phase 18: Angular 22

Migration runtime: Node `22.23.2`, npm `10.9.8`.  
Final runtime: Node `24.20.0`, npm `11.19.0`.

- [ ] Upgrade Angular core to `22.1.4`.
- [ ] Upgrade Angular CLI to `22.1.6`.
- [ ] Upgrade Material and CDK to `22.1.4`.
- [ ] Upgrade TypeScript to `6.0.3`.
- [ ] Keep RxJS at `7.8.2`.
- [ ] Confirm TypeScript remains in Angular's supported `>=6.0.0 <6.1.0` range.
- [ ] Complete the standard major-upgrade procedure under the migration runtime.
- [ ] Switch to Node `24.20.0` and npm `11.19.0`.
- [ ] Update `package.json` engines to the approved final runtime.
- [ ] Update the devcontainer and development documentation to the approved final runtime.
- [ ] Regenerate and commit the final lockfile using npm `11.19.0`.
- [ ] Repeat the complete gate from a clean install under the final runtime.

## Phase 19: Polling And Subscription Corrections

Complete this phase after framework dependencies are stable so behavior changes remain isolated.

- [ ] Replace `setInterval` and nested subscriptions in `TorrentService` with an RxJS `timer` pipeline.
- [ ] Use `exhaustMap` so a slow HTTP request cannot overlap the next poll.
- [ ] Define retry and error handling so one failed request does not permanently stop polling.
- [ ] Sort a copied response array instead of mutating the HTTP response.
- [ ] Remove unowned subscriptions in `DownloadsComponent`.
- [ ] Remove unowned subscriptions in `TorrentSuggestionsComponent`.
- [ ] Fix `DelayedKeyupDirective` so its debounce interval is configured after input binding.
- [ ] Add fake-timer tests for polling cadence.
- [ ] Add tests for request overlap prevention.
- [ ] Add tests for teardown and subscription disposal.
- [ ] Add tests for debounce configuration and cancellation.
- [ ] Add tests for polling error recovery.
- [ ] Run lint, unit, build, Playwright, and API-backed smoke checks.

## Phase 20: Strictness And Quality Tooling

- [ ] Add production bundle budgets based on measured baseline and approved tolerances.
- [ ] Make production build configuration explicit.
- [ ] Fix ambient DTOs with explicit exports and imports before isolated compilation.
- [ ] Enable TypeScript strictness incrementally and fix errors rather than using broad suppressions.
- [ ] Enable `strictTemplates` incrementally and fix template errors.
- [ ] Enable null checking and fix unsafe assumptions.
- [ ] Enable applicable Angular extended diagnostics and review each finding.
- [ ] Keep Karma/Jasmine until the application builder and Angular 22 test suite are stable.
- [ ] Migrate Karma/Jasmine to Angular's Vitest builder as an isolated tooling change.
- [ ] Review the current support status of the existing-project Vitest migration before running it.
- [ ] Verify test discovery, setup, fake timers, DOM behavior, and coverage after the Vitest migration.
- [ ] Remove obsolete Karma/Jasmine configuration and dependencies only after parity is proven.
- [ ] Add CI with exact Node and npm versions.
- [ ] Add CI steps for `npm ci`, dependency validation, lint, unit tests, Playwright, production build, and artifact inspection.
- [ ] Add CI checks that reject focused or skipped tests.
- [ ] Document the optional credentialed API smoke job separately from deterministic CI.

## Phase 21: Material Cleanup

- [ ] Confirm all Material imports use supported component entry points.
- [ ] Remove unused Material imports and exports rather than recreating a catch-all module.
- [ ] Confirm no `::ng-deep` selectors remain.
- [ ] Confirm no application styles depend on private Material implementation classes.
- [ ] Confirm no `TODO(mdc-migration)` markers remain.
- [ ] Verify Material harness tests pass.
- [ ] Approve desktop and mobile visual snapshots.
- [ ] Keep the current indigo-pink visual identity unless a separate product-approved Material 3 redesign is scheduled.

## Phase 22: Final Deterministic Replay

- [ ] Start from a clean clone or disposable worktree.
- [ ] Use exactly Node `24.20.0` and npm `11.19.0`.
- [ ] Install only from the committed lockfile with `npm ci`.
- [ ] Verify `npx ng version` reports the approved Angular, CLI, Material/CDK, TypeScript, and RxJS versions.
- [ ] Verify `npm ls --depth=0` reports no invalid or missing peers.
- [ ] Verify there are no mixed Angular majors.
- [ ] Run lint.
- [ ] Run all unit tests in non-watch mode.
- [ ] Run the production build.
- [ ] Run Playwright e2e tests on desktop and mobile projects.
- [ ] Run visual regression checks.
- [ ] Run the API-backed smoke suite where credentials are available.
- [ ] Inspect the production artifact and confirm the deployment consumer uses the correct output directory.
- [ ] Compare final routes, API behavior, screenshots, output paths, and bundle sizes with the approved baseline.
- [ ] Confirm a second clean install resolves to the same dependency tree.
- [ ] Review `npm audit` findings manually without auto-applying dependency changes.
- [ ] Record the final inventory, migration decisions, accepted deviations, and follow-up work.

## Final Acceptance

- [ ] Node is exactly `24.20.0` and npm is exactly `11.19.0` in development, CI, and production definitions.
- [ ] Angular core is `22.1.4` and Angular CLI is `22.1.6`.
- [ ] Angular Material and CDK are both `22.1.4`.
- [ ] TypeScript is `6.0.3` and RxJS is `7.8.2`.
- [ ] A clean `npm ci` succeeds with no invalid or missing peer dependencies.
- [ ] The application uses Angular's application builder.
- [ ] Bootstrap and application components use the approved standalone architecture.
- [ ] TSLint, Codelyzer, Protractor, legacy control flow, Material barrel imports, old zone paths, and `@angular/http` are absent.
- [ ] The application is OnPush-compatible and runs without zone.js.
- [ ] Long-lived UI state uses signals, `AsyncPipe`, or explicit signal/RxJS interoperability with clear ownership.
- [ ] Polling cannot overlap requests and recovers from transient failures.
- [ ] Lint, unit tests, Playwright tests, visual snapshots, and browser/mobile smoke tests pass.
- [ ] The production build passes approved bundle budgets.
- [ ] Deployment tooling consumes the verified build output directory.
- [ ] Product stakeholders approve browser support and Material visual behavior.
- [ ] The upgrade log and final version inventory are complete.

## Deferred Follow-Ups

- [ ] Upgrade Node 24 to Node 26 only after Node 26 enters LTS, Angular supports it, and the full qualification suite passes.
- [ ] Upgrade TypeScript 6 to TypeScript 7 only after Angular's compatibility table and compiler peer range allow it.
- [ ] Configure Renovate or Dependabot to submit exact-version lockfile updates.
- [ ] Group Angular framework updates together and Material/CDK updates together.
- [ ] Treat a Material 3 visual redesign as a separate product change.
