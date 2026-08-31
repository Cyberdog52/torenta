# Contributing to torenta

Thanks for contributing! This guide covers local setup, build/test commands, and the
pull-request workflow.

> **AI-assisted development:** [`AI_RULES.md`](./AI_RULES.md) is the single source of truth for
> all AI tooling and for the security/architecture rules that apply to everyone. Read it first;
> if anything here conflicts with it, `AI_RULES.md` wins.

## Prerequisites

| Tool    | Version                     | Notes                                             |
|---------|-----------------------------|---------------------------------------------------|
| JDK     | Java 25                     | `build.gradle` configures a Java 25 toolchain.    |
| Node.js | 16 (range `>=10 <17`)       | Pinned in `.nvmrc`; run `nvm use`.                |
| npm     | 6–8 (range `>=6 <9`)        | See `frontend/package.json` `engines`.            |

You do **not** need a local Gradle install — use the wrapper (`./gradlew`, `.\gradlew.bat`).

## First-time setup

1. **TMDB API key.** Copy `src/main/resources/application-template.properties` to
   `application.properties` and set `ch.andreskonrad.torenta.tmdb.service.key`. **Never commit
   `application.properties`** — it is a protected file (see `AI_RULES.md §1`).
2. **Frontend dependencies:**
   ```bash
   cd frontend
   nvm use
   npm install
   npx playwright install chromium
   ```

## Build, run & test

### Backend (from repository root)

```bash
./gradlew build      # compile + test           (Windows: .\gradlew.bat build)
./gradlew test       # JUnit 5 tests             (Windows: .\gradlew.bat test)
./gradlew bootRun    # run API at http://localhost:8080
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

### Frontend (from `frontend/`)

```bash
npm start            # dev server http://localhost:4200 (proxies /api to :8080)
npm test             # Karma/Jasmine unit tests
npm run lint         # tslint + codelyzer
npm run build        # production build
npm run e2e          # Playwright; starts backend and frontend
```

## Pull-request checklist

- [ ] `./gradlew test` passes and `cd frontend && npm test && npm run lint` pass locally.
- [ ] New or changed behavior has tests (JUnit 5/Mockito backend, Karma/Jasmine frontend,
      Playwright end-to-end).
- [ ] Backend DTO changes are mirrored in `frontend/src/app/shared/dto/**`, and OpenAPI
      annotations are updated.
- [ ] No secrets, credentials, or protected-file contents are added to any tracked file.
- [ ] All AI instruction entry points and repository `README.md` files reviewed after code changes;
      anything made inaccurate or incomplete is updated (`AI_RULES.md §4.6`).
- [ ] No unapproved dependency additions or major version bumps (see `AI_RULES.md §4.5`).
- [ ] The vendored `src/main/java/bt/**` library is left unrefactored.

CI (`.github/workflows/ci.yml`) runs the same backend and frontend checks on every PR.

## Reporting security issues

See [`SECURITY.md`](./SECURITY.md). Do not open a public issue for vulnerabilities.
