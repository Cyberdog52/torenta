# 10 — Backend Upgrade Assessment (Java / Gradle)

This document assesses every versioned part of the backend build — the Gradle/Java
toolchain plus every dependency declared in `build.gradle` — chooses compatible stable
upgrade targets, flags abandoned or replaced components, and groups the work into
risk-based backlog units. **No upgrade is applied here**; this is assessment only. Later
tasks execute the units below.

## Assessment date and sources

- **Assessment (reference) date: 2026-08-31** — the single reference date recorded in
  [`00-baseline.md`](./00-baseline.md). Every **latest-stable** value in this document is
  the newest stable (non-alpha/beta/RC/milestone/snapshot) release available **as of
  2026-08-31**, not "today".
- **Sources for latest-stable** (queried on the assessment date): Maven Central
  (`repo1.maven.org/maven2/.../maven-metadata.xml`) for library artifacts, and
  `services.gradle.org/versions/all` for Gradle.
- **Baseline reference:** the backend build/test baseline is **GREEN on the CI-target JDK
  17** (and on JDK 11): `./gradlew build` runs 42 tests, 0 failures, 4 intentionally
  ignored (see `00-baseline.md`). The only red backend condition is
  **[BACKEND-JDK21]** — the build fails on JDK 21 because Lombok 1.18.22 and Gradle 7.2
  predate JDK 21. That prerequisite blocks only upgrades that raise the JDK **past 17**;
  every target chosen below deliberately stays **≤ JDK 17** to remain inside the green
  baseline envelope.

## Chosen target stack

The coherent, well-supported modern stack targeted below is **Spring Boot 3.5.x + Java 17
+ Gradle 8.x + springdoc-openapi 2.x**. Rationale:

- Spring Boot **3.5.16** is the latest release of the last Spring Boot **3.x** line; it
  keeps the Java baseline at **17**, matching the current CI `build` job and the green
  baseline. Spring Boot **4.x** (latest stable 4.1.1) is intentionally **not** targeted
  here — it raises the platform and Java baselines further and is a larger migration; see
  the decision note under Unit 1.
- Java **17** is the Spring Boot 3 minimum and the toolchain CI already builds/tests with,
  so it introduces no new baseline risk. Going to JDK 21+ is blocked by [BACKEND-JDK21].
- Gradle **8.14.5** (latest 8.x) is required because the Spring Boot 3 Gradle plugin needs
  Gradle ≥ 7.5; Gradle 9.x is available but is not needed for this stack and widens risk.

## Assessment table

Every versioned part in `build.gradle`, `gradle/wrapper/gradle-wrapper.properties`, and
`.github/workflows/gradle.yml`. "Latest stable" is as of **2026-08-31**.

| # | Part | Source of truth | Current | Latest stable (2026-08-31) | Target (Spring Boot 3.5 / Java 17 / Gradle 8.x) | Action |
|---|---|---|---|---|---|---|
| 1 | Gradle wrapper | `gradle-wrapper.properties` | 7.2 | 9.7.1 | **8.14.5** (latest 8.x; Boot 3 & JDK 17 compatible) | **Upgrade** — Unit 1 |
| 2 | Java `sourceCompatibility` | `build.gradle` | 11 | 25 (LTS, 2025-09); 21 prior LTS | **17** (Boot 3 minimum; stays in green baseline) | **Upgrade** 11→17 — Unit 1 |
| 3 | CI JDK — `build` job | `gradle.yml` | 17 | 25 (LTS) | **17** (already at target) | **No-action** — no unit (already at compatible target) |
| 4 | CI JDK — `dependency-submission` job | `gradle.yml` | 11 | 25 (LTS) | **17** (must match source level once it moves to 17) | **Upgrade** 11→17 — Unit 1 |
| 5 | Spring Boot plugin | `build.gradle` | 2.2.1.RELEASE | 4.1.1 | **3.5.16** (last 3.x line; Java-17 baseline) | **Upgrade** (major 2→3) — Unit 1 |
| 6 | `io.spring.dependency-management` | `build.gradle` | 1.0.8.RELEASE | 1.1.7 | **1.1.7** | **Upgrade** — Unit 1 |
| 7 | `io.springfox:springfox-swagger2` | `build.gradle` | 2.9.2 | 3.0.0 (**abandoned**, last release 2020-07) | **Replace** → `org.springdoc:springdoc-openapi-starter-webmvc-ui` 2.9.0 | **Replace** — Unit 1 |
| 8 | `io.springfox:springfox-swagger-ui` | `build.gradle` | 2.9.2 | 3.0.0 (**abandoned**, last release 2020-07) | **Replace** → folded into `springdoc-openapi-starter-webmvc-ui` 2.9.0 (bundles Swagger UI) | **Replace** — Unit 1 |
| 9 | `org.projectlombok:lombok` | `build.gradle` | 1.18.22 | 1.18.46 | **1.18.46** | **Upgrade** — Unit 2 (also unblocks [BACKEND-JDK21]) |
| 10 | `org.easymock:easymock` | `build.gradle` | 4.1 | 5.7.0 | **5.7.0** (test-only) | **Upgrade** — Unit 2 |
| 11 | `org.apache.httpcomponents:httpclient` | `build.gradle` | 4.5.13 | 4.5.14 (**final 4.x**); `httpclient5` 5.6.4 (5.x line) | **4.5.14** now (drop-in); **5.6.4** deferred (package/API break) | **Upgrade** patch 4.5.13→4.5.14 — Unit 2; **flag** 4→5 — Unit 6 (decision) |
| 12 | `org.jsoup:jsoup` | `build.gradle` | 1.12.1 | 1.23.2 | **1.23.2** | **Upgrade** — Unit 2 |
| 13 | `com.google.inject:guice` | `build.gradle` | 3.0 | 7.0.0 | **6.0.0** (supports both `javax.inject` & `jakarta.inject`; 7.0 is jakarta-only) | **Upgrade** — Unit 3 |
| 14 | `com.google.inject.extensions:guice-multibindings` | `build.gradle` | 3.0 | 4.2.3 (**final**; artifact retired) | **Remove** — merged into `guice` core ≥ 4.2 | **Replace/remove** — Unit 3 |
| 15 | `org.slf4j:slf4j-api` | `build.gradle` | 1.7.29 | 2.0.18 (2.1.0-alpha1 is pre-release) | **2.0.18** | **Upgrade** — Unit 4 |
| 16 | `org.yaml:snakeyaml` | `build.gradle` | 1.8 | 2.7 | **2.7** | **Upgrade** — Unit 5 |
| 17 | Vendored `bt` library (`src/main/java/bt`) | source tree | forked snapshot of `atomashpolskiy/bt` (© 2016–2017) — **no version pin** | upstream `bt` 1.10 (last release, 2019) | **Decision-needed** (re-sync vs. maintain fork) | **Flag** — Unit 7 (decision) |

## Abandoned / replaced / retired components

None of these may be silently dropped — each carries a replacement or an explicit
decision:

- **springfox-swagger2 / springfox-swagger-ui (rows 7–8) — ABANDONED.** Last release
  3.0.0 (July 2020); no Spring Boot 3 / Jakarta support and no maintenance. **Replacement:
  springdoc-openapi** (`springdoc-openapi-starter-webmvc-ui`, latest 2.x = 2.9.0 for
  Spring Boot 3.x; 3.1.0 targets Spring Boot 4.x). Migration touches
  `src/main/java/ch/andreskonrad/torenta/SwaggerConfig.java` (`@EnableSwagger2` + `Docket`
  → springdoc `GroupedOpenApi`/auto-config; `javax`→`jakarta`). Capability preserved:
  OpenAPI JSON at `/v3/api-docs` and Swagger UI at `/swagger-ui/index.html`.
- **guice-multibindings (row 14) — RETIRED artifact.** `Multibinder`/`MapBinder` were
  folded into **guice core** in 4.2 (2018); the standalone
  `guice-multibindings` artifact stops at 4.2.3 and is not published for guice 5/6/7.
  **Replacement:** drop the dependency and rely on `com.google.inject.multibindings.*`
  now shipped in guice core. The vendored `bt` modules
  (`bt/module/ProtocolModule.java`, `ServiceModule.java`, and their `*Extender`s) use
  `Multibinder`/`MapBinder`, so this must move together with the guice bump (Unit 3).
- **Apache HttpClient 4.x → 5.x (row 11) — package/API break, DECISION-NEEDED.** 4.x is
  end-of-line at 4.5.14; the maintained line is `httpclient5` (`org.apache.hc.client5.*`),
  an incompatible package/API. The drop-in **4.5.13 → 4.5.14** patch is taken now
  (Unit 2); the **4 → 5** migration is a separate **decision ticket** (Unit 6) because it
  rewrites call sites in
  `src/main/java/ch/andreskonrad/torenta/torrent/api/PirateBayDto.java` and `TorrentQuery.java`
  and is **not** forced by Spring Boot 3.
- **Vendored `bt` library (row 17) — DECISION-NEEDED.** A hand-vendored fork with no
  version marker whose transitive deps (guice, snakeyaml, slf4j) are pinned manually in
  `build.gradle`. Because there is no clean "latest stable" for a fork, its disposition is
  a decision ticket (Unit 7): re-sync to a maintained upstream `bt` release, or keep the
  fork and validate each transitive bump against it. No capability is dropped either way.

## No-action parts (no upgrade unit created)

- **CI `build` job JDK 17 (row 3)** is already at the chosen compatible target (17); it
  needs no change and gets no upgrade unit. (The separate `dependency-submission` job at
  JDK 11, row 4, *does* move — it must match the raised source level — and is folded into
  Unit 1.)

## Risk-based backlog units

Coupling rule: parts appear in the same unit **only when they must move together**;
independent parts are separated so they can ship on their own schedules. Baseline
execution-readiness is stated per unit against `00-baseline.md`.

### Unit 1 — Spring Boot 3 platform migration (COUPLED · high risk)

- **Members:** Gradle 7.2→8.14.5 · Java source 11→17 · CI `dependency-submission` JDK
  11→17 · Spring Boot 2.2.1→3.5.16 · io.spring.dependency-management 1.0.8→1.1.7 ·
  springfox-swagger2/-ui 2.9.2 → springdoc-openapi 2.9.0.
- **Outcome:** the app builds, tests, and runs on Spring Boot 3.5.x / Java 17 with
  springdoc-served API docs, on Gradle 8.x, with CI consistent across both jobs.
- **Scope (files):** `build.gradle` (plugin + deps), `gradle-wrapper.properties`,
  `.github/workflows/gradle.yml` (both jobs → JDK 17), `SwaggerConfig.java`, and every
  `javax.*` → `jakarta.*` import in `src/main/java/ch/andreskonrad/**`.
- **Prerequisites:** green backend baseline on JDK 17 (satisfied — see `00-baseline.md`);
  Gradle raised to 8.x **before/with** the Boot 3 plugin (the plugin refuses Gradle < 7.5).
- **Observable completion criteria:** `./gradlew build` passes on JDK 17 with all currently
  non-ignored tests green (42 tests, 4 ignored, 0 failures — the baseline count); the app
  starts; `/v3/api-docs` returns an OpenAPI document and `/swagger-ui/index.html` loads;
  both CI jobs use JDK 17 and are green.
- **Risks:** `javax`→`jakarta` breakage across Spring web/actuator/cache; property and
  auto-config renames between Boot 2.2 and 3.5; springfox→springdoc annotation/config
  differences; Gradle 7→8 plugin/config-cache deprecations; Spring Security test config
  changes.
- **Why coupled (cannot proceed independently):** Spring Boot 3 has a hard **Java 17**
  baseline and the `javax`→`jakarta` namespace switch, so the Boot bump and the Java-17
  bump are one atomic move. Boot 3 **drops springfox** (springfox 3.0.0 is Boot-2/`javax`
  only and abandoned), so API-doc tooling must migrate in the same step or documentation
  breaks. `io.spring.dependency-management` supplies the Boot-aligned dependency BOM and
  must track the Boot version. Gradle 7.2 predates the Boot 3 plugin and must be raised
  with it. Splitting any of these ships a non-building intermediate state.
- **Baseline / readiness:** **execution-ready** — the backend baseline is green on JDK 17
  and this unit targets exactly JDK 17. (If it instead targeted JDK 21+, it would be
  **blocked on [BACKEND-JDK21]** until Lombok and Gradle gain JDK-21 support.)
- **Decision note:** Spring Boot 4.x (latest stable 4.1.1) is deferred, not chosen, because
  it raises the platform/Java baseline beyond the green JDK-17 envelope; revisit after
  Unit 1 lands and a JDK-21 baseline is restored.

### Unit 2 — Low-risk drop-in library refresh (INDEPENDENT batch · low risk)

- **Members:** lombok 1.18.22→1.18.46 · easymock 4.1→5.7.0 · jsoup 1.12.1→1.23.2 ·
  httpclient 4.5.13→4.5.14 (final-4.x patch).
- **Outcome:** these libraries sit at their latest compatible stable release with no API
  migration required.
- **Scope:** `build.gradle` version strings only (no source changes expected).
- **Prerequisites:** none beyond the green JDK-17 baseline.
- **Observable completion criteria:** `./gradlew build` stays green (42 tests) after each
  bump; jsoup-based scraping in `PirateBayDto`/`TorrentQuery` still parses; EasyMock-based
  tests still pass.
- **Risks:** low. EasyMock 5 requires Java 11+ (satisfied); jsoup parser tightening could
  affect PirateBay HTML parsing — verify against a sample page.
- **Coupling note:** these parts are **mutually independent** and do **not** have to move
  together; they are grouped only as a single low-risk scheduling batch and each may ship
  on its own. Lombok's bump additionally unblocks **[BACKEND-JDK21]** (Lombok ≥ 1.18.30
  supports newer JDKs), but that JDK bump is out of scope here.

### Unit 3 — Guice modernization + guice-multibindings removal (COUPLED · moderate risk)

- **Members:** guice 3.0→6.0.0 · guice-multibindings 3.0→**removed**.
- **Outcome:** the vendored `bt` DI wiring runs on a maintained guice with multibindings
  supplied by guice core.
- **Scope:** `build.gradle` (bump guice, delete the guice-multibindings dependency); verify
  imports in `bt/module/ProtocolModule.java`, `ServiceModule.java`, and the `*Extender`
  classes.
- **Prerequisites:** none beyond the green JDK-17 baseline; conceptually gated by the Unit 7
  fork decision (whether to bump the fork's deps in place).
- **Observable completion criteria:** `./gradlew build` green; the `bt` runtime constructs
  its Guice injector and the torrent client wiring resolves (the `bt` bencoding tests and
  app startup exercise this).
- **Risks:** guice 6/7 dropped Java-7 support and changed some SPI; guice **7.0** is
  `jakarta.inject`-only while `bt/net/portmapping/impl/PortMappingInitializer.java` imports
  `javax.inject.*` — so **6.0.0** (dual javax/jakarta) is the safe target; go to 7.0 only
  after `bt` migrates to `jakarta.inject`.
- **Why coupled (cannot proceed independently):** `guice-multibindings` was merged into
  guice core in 4.2 and is not published for guice ≥ 5. Bumping guice past 4.2 **requires**
  removing the separate artifact (its `Multibinder`/`MapBinder` now live in core); removing
  the artifact without bumping guice deletes classes the `bt` modules need. Neither move is
  valid alone.
- **Baseline / readiness:** **execution-ready** (green JDK-17 baseline; target stays ≤ 17).

### Unit 4 — slf4j-api 1.7.29 → 2.0.18 (INDEPENDENT · low–moderate risk)

- **Outcome:** SLF4J API at the 2.0 line used by the vendored `bt` logging calls.
- **Scope:** `build.gradle` version only; no source change expected (2.0 keeps the 1.7
  `Logger`/`LoggerFactory` API source-compatible).
- **Prerequisites:** none beyond the green baseline.
- **Observable completion criteria:** `./gradlew build` green; `bt` logging still emits (no
  `NOP` provider warning regression) via the Spring Boot-managed Logback binding.
- **Risks:** SLF4J 2.0 changes the provider-binding mechanism (`ServiceLoader` vs. 1.7's
  static binder); confirm exactly one binding is on the runtime classpath after the Boot 3
  BOM (Unit 1) is in play.
- **Coupling note:** independent of every other unit; ordered after Unit 1 only to validate
  against the Boot-3-managed logging stack, not because it must move with it.
- **Baseline / readiness:** **execution-ready**.

### Unit 5 — snakeyaml 1.8 → 2.7 (INDEPENDENT · moderate risk)

- **Outcome:** SnakeYAML at 2.x under the vendored `bt` YAML model loader.
- **Scope:** `build.gradle` version; verify
  `bt/bencoding/model/YamlBEObjectModelLoader.java` and `bt/metainfo/MetadataService.java`
  (`YamlBEObjectModelLoaderTest` covers the loader — 4 tests).
- **Prerequisites:** none beyond the green baseline.
- **Observable completion criteria:** `./gradlew build` green with
  `YamlBEObjectModelLoaderTest` passing.
- **Risks:** SnakeYAML 2.x tightened `SafeConstructor`/global-tag handling and changed some
  constructor signatures between the 1.x and 2.x lines; the `bt` loader must be checked
  against the stricter default.
- **Coupling note:** independent; groups with no other part.
- **Baseline / readiness:** **execution-ready**.

### Unit 6 — Apache HttpClient 4 → 5 migration (DECISION · moderate–high risk)

- **Outcome:** decide whether to migrate app HTTP calls from HttpClient 4.5.x to the
  maintained `httpclient5` (5.6.4) line.
- **Scope:** `build.gradle` (swap artifact) plus rewriting
  `src/main/java/ch/andreskonrad/torenta/torrent/api/PirateBayDto.java` and `TorrentQuery.java`
  from `org.apache.http.*` to `org.apache.hc.client5.http.*`.
- **Prerequisites:** none technically; **not** required by Spring Boot 3 (Unit 1 ships the
  drop-in 4.5.14 patch via Unit 2).
- **Observable completion criteria (if approved):** `./gradlew build` green; PirateBay
  scraping requests still succeed against a live/recorded response.
- **Risks:** full package rename and API redesign (request builders, response handling,
  connection management); behavioural differences in redirect/timeout defaults.
- **Why a decision, not a scheduled upgrade:** 4.x still receives the drop-in 4.5.14 patch
  and works; 5.x is an incompatible rewrite with no forcing function, so a human should
  decide cost vs. benefit rather than it being auto-scheduled. Kept separate from Unit 2
  precisely because it is **not** a drop-in.
- **Baseline / readiness:** execution-ready if approved (green JDK-17 baseline), but
  **blocked on a product decision** first.

### Unit 7 — Vendored `bt` library disposition (DECISION · gates Units 3–5)

- **Outcome:** decide how the hand-vendored `atomashpolskiy/bt` fork under
  `src/main/java/bt` is maintained.
- **Scope:** the whole `bt` source tree plus its manually pinned transitive deps in
  `build.gradle` (guice, guice-multibindings, slf4j, snakeyaml).
- **Prerequisites:** none.
- **Observable completion criteria:** a recorded decision — either (a) re-sync to a
  maintained upstream `bt` release (and drop the manual transitive pins in favour of the
  library's own), or (b) keep the fork and validate each transitive bump (Units 3–5)
  against it — with `./gradlew build` green under the chosen path.
- **Risks:** the fork has no upstream version marker, so transitive bumps (guice/snakeyaml/
  slf4j) are applied blind; a re-sync may pull API changes across the large `bt` surface.
- **Why a decision:** there is no clean "latest stable" for a fork; capability must be
  preserved, so the choice between re-sync and in-place maintenance is a human decision
  that gates the risk framing of Units 3, 4, and 5.
- **Baseline / readiness:** decision task; no build change on its own.

## Baseline linkage summary

- **Backend baseline is GREEN on JDK 17** (`00-baseline.md`), so **every unit above is
  execution-ready** — none is blocked on baseline restoration — because all targets stay
  **≤ JDK 17**.
- The only backend restoration prerequisite, **[BACKEND-JDK21]** (build red on JDK 21 due
  to Lombok 1.18.22 + Gradle 7.2), blocks nothing chosen here; it would block only a future
  unit that raises the JDK past 17. Lombok's bump in Unit 2 and Gradle's bump in Unit 1 are
  the first two steps that would later lift that block.
- Units 6 and 7 are **decision** tickets: technically execution-ready against the green
  baseline but gated on a human decision before any code moves.
