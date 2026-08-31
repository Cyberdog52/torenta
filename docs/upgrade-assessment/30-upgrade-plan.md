# 30 — Upgrade Plan (synthesis: targets, ordering, backlog integrity)

This document reconciles the backend ([`10-backend-assessment.md`](./10-backend-assessment.md))
and frontend ([`20-frontend-assessment.md`](./20-frontend-assessment.md)) assessments into a
single, mutually-compatible upgrade plan. It records the newest mutually-compatible **stable
target set** (with the newer releases that were excluded and why), raises **decision tickets**
for the blockers that have no viable stable path, marks the units those decisions **block**,
lays out the **numbered execution stages** (with parallelizable stages and per-stage rationale),
and closes with the **backlog-integrity summary table**. **No upgrade is applied here** — this is
planning only; the individual units are executed by later tasks.

## Reference date and inputs

- **Reference date: 2026-08-31** — the single snapshot recorded in
  [`00-baseline.md`](./00-baseline.md). Every "latest stable" and "newest viable" value below is
  taken as of this date, so the whole plan is chosen against one consistent ecosystem snapshot.
- **Baseline envelope:** backend is **GREEN** on the CI-target JDK 17 (and JDK 11); frontend is
  **RED** on the only available Node (v24.16.0). The restoration prerequisites are
  **[BACKEND-JDK21]** (build red on JDK 21), **[FRONTEND-NODE]** (no green `ng build`/`ng test`),
  and **[FRONTEND-NVM]** (`nvm`/`.nvmrc` workflow not reproducible). See `00-baseline.md`.
- **Two independent ecosystems.** The backend (Java/Gradle) and frontend (Angular/npm) share **no
  dependency, no runtime, and no lockfile** — the Java toolchain is used only by the backend and
  Node only by the frontend. Cross-ecosystem "mutual compatibility" therefore reduces to two
  facts: (1) each ecosystem's chosen versions are internally consistent (the version-matrix work
  below), and (2) both ecosystems are pinned to the same 2026-08-31 snapshot. There is **no**
  cross-ecosystem version conflict to reconcile, which is also why backend and frontend units can
  run fully in parallel (see the stage plan).

---

## 1. Newest mutually-compatible stable target set

The coherent modern stack targeted across both ecosystems, staying inside each ecosystem's
baseline envelope:

| Ecosystem | Target |
|---|---|
| Backend platform | **Spring Boot 3.5.16** + **Java 17** + **Gradle 8.14.5** + **springdoc-openapi 2.9.0** + `io.spring.dependency-management` **1.1.7** |
| Backend libraries | lombok **1.18.46** · easymock **5.7.0** · jsoup **1.23.2** · httpclient **4.5.14** (interim, final 4.x) · guice **6.0.0** (guice-multibindings **removed**) · slf4j-api **2.0.18** · snakeyaml **2.7** |
| Frontend platform | **Angular 22.1.4/22.1.6** + **TypeScript 6.0.3** + **RxJS 7.8.2** + **zone.js 0.16.2** on **Node 24 LTS** (`.nvmrc` created), npm **≥ 8** |
| Frontend tooling | **angular-eslint 22.2.0** (replaces TSLint + Codelyzer) · jasmine-core **~6.3.0** · karma **~6.4.0** · sass **1.103.1** · `@types/node` **^24**; `@angular/http` and `core-js` **removed** |

### Excluded newer releases (chosen older-but-viable, and why)

Where the *individually newest* release conflicts with the mutually-compatible combination, the
newest **viable** version is chosen and the newer release is excluded here with its reason.

| Part | Newest at 2026-08-31 | Chosen | Excluded newer — reason |
|---|---|---|---|
| Spring Boot | 4.1.1 | **3.5.16** | Boot 4 raises the platform/Java baseline **past JDK 17**, leaving the green JDK-17 envelope and colliding with **[BACKEND-JDK21]**; it is also a larger migration. 3.5.x is the last Java-17-baseline line. |
| Java source/toolchain | 25 (LTS); 21 (prior LTS) | **17** | JDK 21+ is blocked by **[BACKEND-JDK21]** (Lombok 1.18.22 + Gradle 7.2 predate JDK 21); Spring Boot 3's baseline is 17. |
| Gradle | 9.7.1 | **8.14.5** | The Boot 3 plugin only needs Gradle ≥ 7.5; Gradle 9 is unnecessary for this stack and widens migration risk. |
| springdoc-openapi | 3.1.0 | **2.9.0** | springdoc 3.x targets **Spring Boot 4**; the Boot-3.5 line pairs with springdoc **2.x**. Chosen to match the chosen Boot line. |
| guice | 7.0.0 | **6.0.0** | guice 7.0 is **`jakarta.inject`-only**; the vendored `bt` code imports `javax.inject`. 6.0.0 supports both javax and jakarta. Revisit after `bt` migrates to jakarta (gated by the fork decision). |
| Apache HttpClient | httpclient5 5.6.4 | **4.5.14** (interim) | 5.x is an **incompatible package/API rewrite** (`org.apache.hc.client5.*`), not forced by Boot 3; 4.5.14 is the maintained final-4.x drop-in. The 4→5 migration is deferred to a decision ticket. |
| slf4j-api | 2.1.0-alpha1 | **2.0.18** | 2.1.0 is a **pre-release** (alpha); 2.0.18 is the newest stable. |
| TypeScript | 7.0.2 | **6.0.3** | Angular 22's compiler peer is `typescript >=6.0 <6.1`; TS 7 would break `ng build`. |
| jasmine-core | 7.0.2 | **~6.3.0** | The Angular 22 karma builder scaffolds jasmine-core `~6.3.0`; jasmine 7 is not validated against it. |
| Node runtime | 26.8.1 | **24 LTS (≥ 24.15.0)** | Node 26 is matrix-*allowed* but Node 24 is LTS-stable and already runs on the baseline host (v24.16.0). **Node 23/25 excluded** — odd, non-LTS, with gaps in Angular 22's engine matrix. |

**Obsolete parts removed (no successor version — capability preserved another way):**
`@angular/http` (removed in Angular 8; the app already uses `HttpClient` from
`@angular/common/http`) and `core-js` (modern Angular polyfills need only `zone.js`; the sole
`core-js/es7/reflect` import is obsolete on evergreen browsers). Neither drops a used capability.

---

## 2. Decision tickets (blockers with no viable stable path) and blocked units

A **decision ticket** is raised for every blocker that has **no viable stable upgrade path** — an
abandoned component with **no drop-in successor**, or a fork with **no upstream "latest stable"**.
Each affected upgrade unit is marked **blocked until the decision is resolved**. Until a decision
resolves, its dependent units are **not** given an executable position — they wait, rather than
being force-ordered.

### No unresolved cycle exists

Dependency analysis of the reconciled units found **no cyclic or contradictory prerequisites**:

- The vendored-`bt` decision (**DEC-BT**) gates backend units BE-U3/BE-U4/BE-U5 one-directionally
  (those units bump the fork's manually-pinned transitive deps); the gated units do not in turn
  gate the decision.
- **BE-U1** (Spring Boot 3) touches only `ch/andreskonrad/**` (javax→jakarta) and the build/CI
  files — **not** the `bt` tree — so it is independent of DEC-BT and carries no back-edge.
- The one *apparent* frontend cycle — **FE-U1** is "blocked on baseline restoration" yet **is
  itself** the restoration — is resolved (not left cyclic) by adopting restoration **option (b)**:
  the Angular 6→22 upgrade on Node 24 *is* the frontend baseline restoration (Angular 22 supports
  the host's Node v24.16.0), and creating `.nvmrc` clears [FRONTEND-NVM]. With option (b) chosen,
  FE-U1 has a definite first position; no arbitrary order is claimed across an unresolved cycle.

Because there is no unresolved cycle, the numbered stages in §3 are a genuine order, not an
arbitrary linearization.

### DEC-BT — Vendored `bt` fork disposition · **security-relevant** · gates BE-U3, BE-U4, BE-U5

- **Blocker type:** hand-vendored fork of `atomashpolskiy/bt` under `src/main/java/bt` with **no
  version pin and no clean "latest stable"** — its transitive deps (guice, guice-multibindings,
  slf4j, snakeyaml) are pinned manually in `build.gradle`, so any bump to them is applied "blind"
  against the fork.
- **Decision:** (a) re-sync to a maintained upstream `bt` release (adopting the library's own
  transitive pins, which may subsume BE-U3/4/5), **or** (b) keep the fork and validate each
  transitive bump against it.
- **Why security-relevant:** BE-U5 (snakeyaml 1.8 → 2.7) closes the SnakeYAML deserialization RCE
  class (CVE-2022-1471, affecting all 1.x ≤ 1.33) — but snakeyaml is a `bt`-pinned transitive dep,
  so this decision **bounds the earliest position** the security fix can land. It is therefore
  prioritized among the decisions (resolve during Stage 1) so BE-U5 can execute at Stage 3.
- **Blocked until resolved:** **BE-U3** (guice), **BE-U4** (slf4j), **BE-U5** (snakeyaml).

### DEC-E2E — Protractor retirement / e2e tool choice · gates FE-U3

- **Blocker type:** Protractor is **end-of-life (final 7.0.0) with no drop-in successor**; Angular
  dropped it from new projects at v12/13. The replacement is a product/tooling choice, not an
  automatable bump.
- **Decision:** choose Cypress, Playwright, or WebdriverIO — or drop e2e entirely — and reproduce
  the single existing e2e spec on the chosen tool (or record its removal). `@types/jasminewd2` and
  the e2e `ts-node` exist only to serve the Protractor/Jasmine stack and are disposed of with it.
- **Blocked until resolved:** **FE-U3** (e2e replacement).

### DEC-HTTPCLIENT — Apache HttpClient 4 → 5 migration · gates BE-U6 (optional)

- **Blocker type:** HttpClient 4.x is end-of-line at **4.5.14**; the maintained line is
  `httpclient5` (5.6.4) with an **incompatible package/API** (`org.apache.hc.client5.*`). There
  **is** a viable stable interim (4.5.14 ships in BE-U2), so this decision blocks **only** the
  optional 4→5 migration and nothing else.
- **Decision:** whether to rewrite `PirateBayDto.java` / `TorrentQuery.java` onto `httpclient5`
  (cost vs. benefit), given the 4.5.14 drop-in already keeps 4.x patched and Boot 3 does not force
  the move.
- **Blocked until resolved:** **BE-U6** (HttpClient 4→5) — optional; if declined, no work.

---

## 3. Recommended implementation order (numbered stages)

Ordering rules applied: **baseline-restoration and prerequisites precede dependents**;
**higher-risk changes are isolated** (not bundled with other changes to the same build, so a
failure is attributable); **security-driven upgrades are placed at the earliest position their
prerequisites allow**; and **independent units that can run concurrently share one stage** (no
false dependencies, no arbitrary serial order). Backend and frontend units share stages freely
because the two ecosystems are independent (see the inputs section).

Legend: **[BE]** backend · **[FE]** frontend · **[DEC]** decision (no code).

### Stage 1 — Baseline restoration + no-prerequisite security drop-ins (parallel)

Runs concurrently, and the decision tickets open here so they resolve before their Stage-2/3
dependents:

- **[FE] FE-U1 — Angular 6 → 22 stepwise migration + Node 24 + `.nvmrc`** *(high risk, isolated
  on the frontend side).* This unit **is the frontend baseline restoration** (option (b)) and is a
  hard prerequisite for every other frontend unit, so it must come first. It is the only frontend
  work in this stage — the high-risk migration is isolated from FE-U2/3/4 so a break is
  attributable to the framework journey, not to lint/e2e/sass changes.
- **[BE] BE-U2 — low-risk drop-in library refresh** (lombok 1.18.46, easymock 5.7.0, **jsoup
  1.23.2**, httpclient **4.5.14**) *(low risk).* Placed earliest on the backend side because it has
  **no prerequisites** and carries the **security-driven** drop-in patches: jsoup 1.12.1 → 1.23.2
  closes the XSS class **CVE-2022-36033** (affects jsoup < 1.15.3), and httpclient 4.5.13 → 4.5.14
  keeps the 4.x line patched. Doing these trivial, safe bumps first also avoids re-validating them
  after the disruptive Boot-3 migration.
- **[DEC] DEC-BT**, **DEC-E2E**, **DEC-HTTPCLIENT** open now (decision-only, mutually independent —
  no order among them). DEC-BT is prioritized because it gates the SnakeYAML security fix (BE-U5).

*Rationale:* the only red baseline (frontend) is restored first via FE-U1; the security-relevant,
prerequisite-free backend drop-ins land at the earliest possible position (BE-U2); the two
high-risk/large items in flight (FE-U1) and the low-risk batch (BE-U2) are in **disjoint
ecosystems**, so they parallelize without entangling attribution.

### Stage 2 — Platform migration + post-restoration frontend units (parallel)

- **[BE] BE-U1 — Spring Boot 3 platform migration** (Gradle 8.14.5 · Java 11→17 · CI both jobs →
  JDK 17 · Boot 2.2.1→3.5.16 · dependency-management 1.1.7 · springfox → springdoc 2.9.0) *(high
  risk, isolated).* Its only prerequisite (a green JDK-17 baseline) is already satisfied, so its
  earliest safe position is right after the low-risk drop-ins. It is **isolated in its own backend
  stage** — separated from BE-U2 so a Boot-3/jakarta failure is not entangled with drop-in bumps,
  and it addresses the largest security exposure (Spring Boot 2.2.x is long **EOL** and receives no
  security patches). This is the earliest position risk-isolation allows.
- **[FE] FE-U2 — lint: TSLint + Codelyzer → angular-eslint 22.2.0** *(moderate).* Prerequisite:
  FE-U1 (angular-eslint 22 needs Angular 22; the TSLint builder is gone from modern
  `build-angular`).
- **[FE] FE-U4 — sass 1.63.6 → 1.103.1** *(low).* Prerequisite: a green frontend build (FE-U1) to
  *verify* against; the bump itself is Angular-independent.

FE-U2 and FE-U4 are **mutually independent** (lint config vs. Sass compiler) and share this stage
with no false dependency. BE-U1 is independent of all frontend work and runs concurrently.

*Rationale:* prerequisites precede dependents (FE-U1 before FE-U2/U4); the second high-risk item
(BE-U1) is isolated in its own backend stage and placed as early as isolation permits; independent
units share the stage.

### Stage 3 — Fork-gated backend library bumps (parallel, after DEC-BT + BE-U1)

- **[BE] BE-U3 — guice 3.0 → 6.0.0 + remove guice-multibindings** *(moderate).*
- **[BE] BE-U4 — slf4j-api 1.7.29 → 2.0.18** *(low–moderate).* Ordered after BE-U1 to validate the
  single binding against the Boot-3-managed Logback stack.
- **[BE] BE-U5 — snakeyaml 1.8 → 2.7** *(moderate, **security-driven**).* Closes SnakeYAML RCE
  **CVE-2022-1471** (affects 1.x ≤ 1.33). Placed at the **earliest position its prerequisites
  allow**: it depends on DEC-BT (the fix touches a `bt`-pinned transitive dep) and lands after
  BE-U1 (whose Boot-3 BOM also manages snakeyaml 2.x, so the explicit pin must align with it).
  DEC-BT is prioritized in Stage 1 precisely so this security fix is not delayed further.

BE-U3/U4/U5 are **mutually independent** and share this stage. All three are gated by **DEC-BT**
(they bump the vendored fork's manually-pinned transitive deps) and are marked **blocked** until it
resolves — they enter Stage 3 only once DEC-BT is decided and BE-U1 has landed.

*Rationale:* the fork decision must precede its dependents; the security-driven snakeyaml bump is
scheduled as early as that gate and the Boot BOM alignment permit; the three independent library
bumps share one stage.

### Stage 4 — Conditional / decision-gated units

- **[FE] FE-U3 — retire Protractor, adopt chosen e2e runner** *(moderate).* Prerequisites: **FE-U1**
  (modern CLI) **and DEC-E2E**. Executes only after the e2e-tool decision; blocked until then.
- **[BE] BE-U6 — Apache HttpClient 4 → 5 migration** *(moderate–high, **optional**).* Prerequisites:
  **DEC-HTTPCLIENT approved** and BE-U2 (which shipped the 4.5.14 interim). If the decision declines
  the rewrite, this unit produces no work. Isolated because it is not a drop-in.

*Rationale:* both units are gated by an open decision and therefore cannot be assigned an executable
position until their decision resolves; they are placed last and only conditionally.

### Stage dependency summary

```
Stage 1 (parallel):  FE-U1  ||  BE-U2   [+ open DEC-BT, DEC-E2E, DEC-HTTPCLIENT]
Stage 2 (parallel):  BE-U1  ||  FE-U2  ||  FE-U4
Stage 3 (parallel):  BE-U3  ||  BE-U4  ||  BE-U5        (gated by DEC-BT, after BE-U1)
Stage 4 (gated):     FE-U3  (needs DEC-E2E)  ||  BE-U6  (needs DEC-HTTPCLIENT; optional)
```

---

## 4. Backlog-integrity summary

Every proposed ticket, with its identifier, target outcome, prerequisites, blockers, and stage
number. Identifiers `BE-U*`/`FE-U*` map to the units in `10-backend-assessment.md` /
`20-frontend-assessment.md`; `DEC-*` are the decision tickets from §2.

| Ticket | Target outcome | Prerequisites | Blockers | Stage |
|---|---|---|---|---|
| **BE-U2** | Drop-in refresh: lombok 1.18.46, easymock 5.7.0, jsoup 1.23.2 (**CVE-2022-36033**), httpclient 4.5.14 | Green JDK-17 baseline (met) | — | 1 |
| **FE-U1** | Angular 22.1.x / TS 6.0.3 / RxJS 7.8.2 / zone.js 0.16.2 on Node 24 (+ `.nvmrc`); removes `@angular/http`, `core-js`; **restores frontend baseline** | Restoration path chosen (option **b**: this unit is the restoration) | resolves [FRONTEND-NODE]/[FRONTEND-NVM] | 1 |
| **DEC-BT** | Decision: re-sync vs. maintain vendored `bt` fork | — | no upstream "latest stable" (fork) | 1 (decide) |
| **DEC-E2E** | Decision: Cypress / Playwright / WebdriverIO / drop e2e | — | abandoned Protractor, no drop-in successor | 1 (decide) |
| **DEC-HTTPCLIENT** | Decision: adopt httpclient5 5.6.4 or stay on 4.5.14 | — | incompatible 4→5 package/API rewrite | 1 (decide) |
| **BE-U1** | Spring Boot 3.5.16 / Java 17 / Gradle 8.14.5 / springdoc 2.9.0; both CI jobs on JDK 17 (Boot 2.2.x is **EOL**) | Green JDK-17 baseline (met); Gradle raised with the Boot 3 plugin | — | 2 |
| **FE-U2** | `ng lint` on angular-eslint 22.2.0; TSLint + Codelyzer removed | FE-U1 | — | 2 |
| **FE-U4** | sass 1.103.1; `.scss` still compiles | FE-U1 (to verify a green build) | — | 2 |
| **BE-U3** | guice 6.0.0; guice-multibindings removed (folded into guice core) | BE-U1; **DEC-BT** | **DEC-BT** (touches vendored fork) | 3 |
| **BE-U4** | slf4j-api 2.0.18; single binding under Boot-3 Logback | BE-U1; **DEC-BT** | **DEC-BT** (touches vendored fork) | 3 |
| **BE-U5** | snakeyaml 2.7 (**CVE-2022-1471** RCE) | BE-U1 (Boot BOM alignment); **DEC-BT** | **DEC-BT** (touches vendored fork) | 3 |
| **FE-U3** | Retire Protractor; e2e coverage reproduced on chosen tool (or removal recorded) | FE-U1; **DEC-E2E** | **DEC-E2E** | 4 |
| **BE-U6** | *(optional)* HttpClient 4→5 on `httpclient5` 5.6.4 | BE-U2; **DEC-HTTPCLIENT** approved | **DEC-HTTPCLIENT** | 4 |

**Ticket specification completeness:** every proposed ticket above is **fully specified** — each
has a concrete target outcome, prerequisites, blockers (where any), and a stage number, drawn from
the execution-ready unit definitions in `10-`/`20-`. **No ticket is under-specified or missing.**
(If a future reconciliation surfaces a ticket that cannot be fully specified, it must be listed
here as *missing* with its partial definition preserved for retry rather than the table claiming
completeness.)

### Assessed items requiring no action

- **Backend — CI `build` job JDK 17** (`.github/workflows/gradle.yml`): already at the chosen
  compatible target (JDK 17); no unit created. (The separate `dependency-submission` job at JDK 11
  *does* move to 17 as part of **BE-U1**.)
- **Frontend — none.** Every versioned part assessed in `20-frontend-assessment.md` carries an
  action (upgrade, replace, remove, or widen) and is folded into FE-U1/U2/U3/U4; no frontend part
  is already at its target.

### Restoration prerequisites carried forward (not upgrade units)

- **[BACKEND-JDK21]** — build red on JDK 21 (Lombok 1.18.22 + Gradle 7.2). Blocks **nothing** in
  this plan (every backend target stays ≤ JDK 17). BE-U2's Lombok bump and BE-U1's Gradle bump are
  the first two steps that would later lift it, enabling a future JDK 21+ move (and Spring Boot 4).
- **[FRONTEND-NODE] / [FRONTEND-NVM]** — resolved **by FE-U1** under restoration option (b) (see §2
  and Stage 1).
