# AI Rules and Security Policy

**This file is the single authoritative source of truth for AI-assisted development in this
repository.** Every AI coding tool and every human using one must read and follow this file
before performing any task. All other AI instruction files (`AGENTS.md`, `CLAUDE.md`,
`GEMINI.md`, `.github/copilot-instructions.md`, ignore files) are deliberately kept as thin
pointer stubs that defer to this document — rules are maintained here and nowhere else.
**If any instruction anywhere conflicts with this file, this file wins.**

Applies to (non-exhaustive): GitHub Copilot, Claude Code, Cursor, Aider, Continue.dev,
OpenCode, Gemini CLI, and any future agent-based coding tool.

---

## 1. Security (highest priority)

### 1.1 Protected files — never read, index, summarize, embed, or reuse

The following may contain secrets, credentials, tokens, API keys, or other confidential data
and **must never be used as AI context** in any form:

```text
src/main/resources/application.properties
src/main/resources/application-*.properties      # EXCEPT application-template.properties
src/main/resources/bitthief.properties
.env
.env.*                                            # EXCEPT .env.template / .env.example
secrets/**
certificates/**
credentials/**
**/*.pem  **/*.key  **/*.p12  **/*.pfx  **/*.jks  **/*.keystore  **/*.crt
**/id_rsa  **/id_dsa  **/id_ecdsa  **/id_ed25519
```

For these files, AI tools must not:

- Read, open, or index them.
- Summarize or describe their contents.
- Generate code derived from their real values.
- Echo, print, or include any of their values in a response, commit, log, or comment.

If configuration is required, request **sanitized or placeholder values** instead, or read the
committed template files listed below.

### 1.2 Allowed placeholder / template files

Safe, committed template files (placeholder values only, never real secrets):

```text
src/main/resources/application-template.properties
.env.template
.env.example
```

Example placeholder (this is the expected property name; the value must stay a placeholder):

```properties
# src/main/resources/application-template.properties
ch.andreskonrad.torenta.tmdb.service.key=YOUR_TMDB_API_KEY
```

### 1.3 Never commit secrets

Secrets must never enter source control: access tokens, API keys, passwords, certificates,
private keys, production connection strings. Do not add real values to any tracked file,
including tests, fixtures, comments, or documentation. Prefer environment variables or a secret
manager (Azure Key Vault, HashiCorp Vault, AWS Secrets Manager, Kubernetes Secrets).

### 1.4 Security behavior rules for AI tools

- **Never** modify a protected/security-sensitive file (see 1.1, plus `.gitignore`,
  `.claude/settings.json`, ignore files, CI/CD config, and `build.gradle` security-relevant
  sections) without **explicit user approval** in the current request.
- **Never** suggest committing a secret, disabling a security check, or weakening TLS/crypto.
- **Flag** insecure implementations you encounter: injection risks (this app builds URLs and
  parses HTML with jsoup, and scrapes external sites), unvalidated external input, path
  traversal in file/directory handling, `printStackTrace()` leaking internals, and hard-coded
  credentials.
- Report a suspected secret exposure to the user; never "fix" it by printing the secret.

### 1.5 Enforcement reality

Repository files (`AGENTS.md`, `AI_RULES.md`, `copilot-instructions.md`) are **guidance**, not a
security boundary. `.claude/settings.json` deny rules are the strongest repo-local enforcement
available; ignore files (`.aiignore`, `.cursorignore`, `.aiderignore`, `.continueignore`) reduce
indexing but are not guarantees. Do not assume in-repo files are inaccessible to any tool.

---

## 2. Project Overview

`torenta` is a media discovery and BitTorrent download manager.

- **Backend:** Spring Boot 4.1.1 REST API, Java 25, built with Gradle 9.7.1.
- **Frontend:** Angular 6 SPA (TypeScript), served separately, proxied to the backend.
- **Purpose:** search TMDB for movies/series, find torrents (PirateBay HTML scraping), download
  via a vendored BitTorrent engine, and manage a local media library.

### Backend stack

- Spring Boot 4.1.1 (`spring-boot-starter-webmvc`, `-actuator`, `-cache`), Java 25.
- Lombok, Jackson 3, Springdoc OpenAPI, Guice, jsoup, Apache HttpClient, Java 25 `java.net.http`.
- Tests: JUnit 5 (`useJUnitPlatform`), Mockito, spring-security-test.

### Frontend stack

- Angular 6, TypeScript ~2.9, RxJS, Angular Material.
- Node.js 10–16, npm 6–8 (see `frontend/package.json` `engines`).
- Tests: Karma + Jasmine; lint: tslint + codelyzer.

### Repository layout

```text
build.gradle, settings.gradle, gradlew           # Gradle backend build
src/main/java/bt/**                               # VENDORED BitTorrent library — do not refactor
src/main/java/ch/andreskonrad/torenta/**          # Application code (feature-sliced)
src/main/resources/**                             # Config (contains protected files)
src/test/java/**                                  # JUnit 5 tests
frontend/                                         # Angular 6 SPA
```

---

## 3. Architecture Rules

Derive changes from the existing structure; **preserve it**.

### 3.1 Backend package structure (feature-sliced)

Root package: `ch.andreskonrad.torenta`. Each feature is a vertical slice with the same
sub-package layout:

```text
ch.andreskonrad.torenta.<feature>.controller   # @RestController, HTTP concerns only
ch.andreskonrad.torenta.<feature>.service      # @Service, business logic
ch.andreskonrad.torenta.<feature>.dto          # request/response data objects
```

Existing features: `bittorrent`, `directory`, `library`, `preference`, `tmdb`, `torrent`.
Add new functionality as a new feature slice following this pattern; do not create cross-cutting
"utils"/"helpers" dumping grounds.

### 3.2 Dependency direction & boundaries

- `controller → service → (dto, external APIs)`. Controllers must not contain business logic.
- Services must not depend on controllers. DTOs must not depend on controllers or services.
- Do not let one feature's controller call another feature's service directly without reason;
  prefer going through the owning service.
- **`src/main/java/bt/**` is a vendored third-party library.** Do not refactor, reformat, or
  "clean up" it. Only touch it for a deliberate, user-approved integration fix.

### 3.3 REST / API conventions

- Controllers are annotated `@RestController` with a class-level `@RequestMapping("/api/<feature>/")`.
- Use constructor injection (`@Autowired` on the constructor); do not add field injection.
- Return `ResponseEntity<T>` with an explicit `HttpStatus`; map failures to `HttpStatus.NOT_FOUND`
  (existing convention) rather than leaking exceptions.
- Keep endpoint DTO shapes in sync with the mirrored frontend DTOs under
  `frontend/src/app/shared/dto/**`.

### 3.4 Configuration & external services

- Inject configuration with `@Value("${...}")` (e.g. the TMDB key
  `ch.andreskonrad.torenta.tmdb.service.key`). Never hard-code keys or hosts that belong in config.
- Caching uses Spring Cache (`@CacheConfig`, `@Cacheable`) — see `CustomCacheConfig`. Reuse it for
  expensive external calls instead of adding new caching frameworks.
- External HTTP throttling exists (`RequestThrottler`); respect rate limits when adding external calls.

### 3.5 Frontend architecture

- Feature folders under `frontend/src/app/<feature>/` with components + a `*.service.ts`.
- Services are `@Injectable({ providedIn: 'root' })`, use `HttpClient`, return `Observable<T>`,
  and call the backend under the `api/...` base (dev proxy in `proxy.conf.json`).
- Shared DTO interfaces live in `frontend/src/app/shared/dto/**` and mirror backend DTOs — keep
  them consistent when changing an endpoint.
- Use Angular Material components already in use; do not introduce a second UI framework.

---

## 4. Development Standards

### 4.1 Code style

- **Java:** follow existing style — 4-space indent, constructor injection, `final` fields for
  injected collaborators, Lombok where already used. Match the surrounding file.
- **TypeScript:** follow `frontend/tslint.json` + codelyzer; keep the existing 2-space style and
  strong typing on `HttpClient` calls.
- Only add comments that clarify non-obvious intent; do not add narration comments.

### 4.2 Testing strategy

- **Backend:** JUnit 5 with Mockito (see `src/test/java/**`). Add/adjust tests for new or changed
  service logic. Run: `./gradlew test` (Windows: `.\gradlew.bat test`).
- **Frontend:** Karma + Jasmine (`*.spec.ts`). Run: `cd frontend; npm test`.
- Generate tests for new functionality and bug fixes. Do not delete or weaken existing tests to
  make a build pass — fix the cause.

### 4.3 Error handling & logging

- Use SLF4J (`org.slf4j`) for logging. **Do not use `printStackTrace()` or `System.out`** in
  application code (some legacy code does — do not copy that pattern; prefer logging when editing).
- Never log secrets, API keys, tokens, or full external credentials.
- Handle external-call failures explicitly and translate them to the established `ResponseEntity`
  error responses rather than propagating raw exceptions to clients.

### 4.4 Performance & observability

- Reuse existing caching (`@Cacheable`) and throttling for external APIs; avoid N+1 external calls.
- Spring Boot Actuator is on the classpath — prefer it for health/metrics rather than custom endpoints.

### 4.5 Dependency management

- **Do not add dependencies casually.** Prefer the standard library and libraries already present
  (Spring, Jackson, jsoup, Apache HttpClient, Guice, Lombok on the backend; Angular/RxJS/Material on
  the frontend).
- Adding or upgrading a dependency (`build.gradle` / `frontend/package.json`) requires explicit
  user approval and a stated reason. Respect the pinned Java 25 / Angular 6 / Node 10–16 constraints;
  do not silently bump major versions.

### 4.6 Documentation

- Update `README.md` and relevant docs when you change setup steps, run commands, endpoints, or
  behavior. Update OpenAPI annotations/DTOs so `/swagger-ui.html` stays accurate.
- When changing Java, Gradle, Spring Boot, Node.js, or other platform versions, audit every
  version-bearing surface: build files, wrappers, CI workflows, `.devcontainer/devcontainer.json`,
  runtime images, and setup documentation.

---

## 5. AI Assistant Behavior

- **Understand before changing.** Read the relevant code first; do not guess at APIs or invent
  methods, properties, or config keys. If unsure, ask or inspect — do not hallucinate.
- **Keep changes minimal and surgical.** Fully solve the request without touching unrelated code.
- **Preserve the existing architecture** and the feature-sliced structure; do not introduce new
  frameworks, patterns, or abstractions unless explicitly requested.
- **No speculative refactoring** and no reformatting of files you are not otherwise changing —
  especially never the vendored `bt/**` library.
- **Explain before large or destructive changes.** For big refactors, deletions, migrations, or
  anything touching protected/security-sensitive files, describe the plan and get approval first.
- **Generate tests** for new functionality and **update documentation** when behavior changes.
- **Verify** your change builds/tests where feasible before declaring done.
- Never fabricate file paths, commands, or results.

---

## 6. Common Commands (verified against project config)

```bash
# Backend (Gradle wrapper)
./gradlew build            # compile + test the backend        (Windows: .\gradlew.bat build)
./gradlew test             # run JUnit 5 tests                  (Windows: .\gradlew.bat test)
./gradlew bootRun          # run backend at http://localhost:8080

# Frontend (from ./frontend)
npm install                # install deps (Node 10-16, npm 6-8)
npm start                  # dev server http://localhost:4200 (proxies /api to :8080)
npm run build              # production build
npm test                   # Karma/Jasmine unit tests
npm run lint               # tslint
```

- Backend API base: `http://localhost:8080/`; Swagger UI: `http://localhost:8080/swagger-ui.html`.
- Frontend dev server: `http://localhost:4200/`.

---

## 7. Related AI Configuration Files (all defer to this file)

The four tool entry points below are **pure pointer stubs**: each contains only a link to this
file. Do not duplicate security, architecture, coding, testing, or command guidance into them —
add or change rules here instead.

```text
AGENTS.md                          # pointer stub — central agent entry point -> AI_RULES.md
CLAUDE.md                          # pointer stub -> AI_RULES.md (+ .claude/settings.json note)
GEMINI.md                          # pointer stub -> AI_RULES.md
.github/copilot-instructions.md    # pointer stub -> AI_RULES.md
.claude/settings.json              # Claude Code Read() deny rules (strongest repo-local enforcement)
.aiignore .cursorignore .aiderignore .continueignore   # indexing exclusions (not a security boundary)
```

Human-facing docs that reference this file: `README.md`, `CONTRIBUTING.md`, `SECURITY.md`,
`docs/ARCHITECTURE.md`.
