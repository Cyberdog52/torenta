# torenta

## Setup

#### Java (only needed for source builds)
Install Java OpenJDK 25 only if you build/run from source (the build uses the Java 25 toolchain configured in `build.gradle`).
Make sure it is also in the system's path, because gradlew will use the java version that is on the path.
To see which java version is used, type the following in the terminal:

```
Windows: 
for %i in (java.exe) do @echo.   %~$PATH:i

Linux/Unix/Mac OS X: 
which java
```


#### IntelliJ (optional)
IntelliJ IDEA is optional and only needed if you prefer running/debugging from the IDE.

#### TMDB-Key
First, you need to get an API key from [TMDB](https://developers.themoviedb.org/3/getting-started/introduction). Configure the key in the application's user preferences (Preferences in the UI).

#### NodeJs
The frontend runs on **Angular 22** and requires **Node.js 24 LTS** (>= 24.15.0).
The repository pins the version in `.nvmrc`:

```bash
nvm use
```

If you do not have the required runtime installed yet:

```bash
nvm install 24
nvm use 24
```

Then install the dependencies:

```bash
cd frontend
npm install
npx playwright install chromium
```

To build the portable application archive from the repository root:

```bash
./gradlew createPortableArchive
```

The Gradle task installs the locked frontend dependencies with `npm ci`, builds the Angular
application, and writes the ZIP to `build/distributions/`.

## Frontend stack

| | |
|---|---|
| Angular | 22 (standalone, signals, zoneless) |
| Angular Material | 22 (Material 3 theming) |
| TypeScript | 6.0 |
| RxJS | 7.8.2 |
| Unit tests | Vitest (`npm test`) |

## Run

#### Run prebuilt backend (no local Java required)

1. Download the latest `torenta-portable-<version>.zip` from GitHub Releases.
2. Unzip it.
3. Start the backend:
   - macOS/Linux: `./run-torenta.sh`
   - Windows: `run-torenta.bat`

The backend starts at http://localhost:8080/

#### Run Backend:



From source (requires Java 25), run the backend with Gradle:

```bash
./gradlew bootRun
```

Or execute `TorentaApplication` through IntelliJ run configurations.

Will start at http://localhost:8080/

#### Run Frontend:

```
cd frontend
npm start
```

Will start at http://localhost:4200/ and proxies `/api` to the backend on
http://localhost:8080/ (see `frontend/proxy.conf.json`).

## Download lifecycle and recovery

Torenta stores durable download records and partial payloads under
`<download-directory>/.torenta/downloads/`. Running downloads are recovered as paused after an
application restart and must be restarted manually. The Downloads page can pause or restart a
download, stop it and delete only its torrent-owned files, or remove a finished tile while keeping
the media files.

Torrent payloads download into an isolated hidden staging directory. On completion Torenta copies
them into the movie or season directory, overwriting files with the same paths, records the final
file manifest, and then removes staging. This crash-safe process can temporarily require twice the
torrent size. Changing the configured download directory does not move old records or partial
payloads; only the newly configured root is loaded.

#### Run End-to-End Test:

The Playwright test starts the Spring Boot backend and Angular dev server automatically. If either
server is already running locally, Playwright reuses it. Run these commands from `frontend/`.

Headless mode is the default and is suitable for CI or a quick local check:

```bash
npm run e2e
```

UI mode opens Playwright's interactive test runner for running, watching, and debugging the test:

```bash
npm run e2e:ui
```

The suite verifies that Angular renders and loads preferences from the backend through the frontend
proxy. Install the bundled Chromium browser first with `npx playwright install chromium` if it was
not installed during setup.

## Documentation

- [CONTRIBUTING.md](CONTRIBUTING.md) — setup, build/test commands, PR checklist
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — components, feature slices, request flow
- [AI_RULES.md](AI_RULES.md) — authoritative security & development rules (AI and humans)
- [SECURITY.md](SECURITY.md) — reporting vulnerabilities

## Development links

[Jira Backlog](https://andreskonrad.atlassian.net/jira/software/projects/TOR/boards/1/backlog)

[Swagger UI](http://localhost:8080/swagger-ui.html)

[OpenAPI Docs](http://localhost:8080/v3/api-docs)

[TMDB](https://developers.themoviedb.org/3/getting-started/introduction)

Designs:
[TV-Overview](https://hubmovies-a26fc.firebaseapp.com/movie/496243)


 

## Further reading:

https://pub.tik.ee.ethz.ch/students/2006-So/MA-2006-26.pdf

http://www.kristenwidman.com/blog/71/how-to-write-a-bittorrent-client-part-2/

https://wiki.theory.org/index.php/BitTorrentSpecification

https://github.com/atomashpolskiy/bittorrent

https://github.com/pmoor/bitthief

https://github.com/clamarque/HubMovies
