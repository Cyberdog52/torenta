# torenta

## Running Torenta from prebuilt archive
1. Download the latest `torenta-portable-<version>.zip` from a developer.
2. Unzip it.
3. Start the backend:
   - macOS/Linux: `./run-torenta.sh`
   - Windows: `run-torenta.bat`
4. Open the torenta.url shortcut or navigate to http://localhost:8080 in your browser.
5. Go to Settings and enter your TMDB API key.

### Well known issue on macOS
On macOS the run-torenta.sh script may open as a text file in the default editor. 
In that case, right-click on the file and select "Open With" -> "Other" -> "Utilities". In the dropdown select "All Applications" and then select "Terminal". Check the box "Always Open With" and click "Open".

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

#### AI Concierge

The `/search` page includes a natural-language AI Concierge. It uses **Ollama** by default.

##### Install Ollama

**Windows**

1. Download and run the official [Ollama installer for Windows](https://ollama.com/download/windows).
   Windows 10 22H2 or newer is required.
2. After installation, Ollama runs in the background and makes the `ollama` command available in a
   new PowerShell or Command Prompt window.

**macOS**

1. Download the official [Ollama DMG for macOS](https://ollama.com/download/mac). macOS 14 Sonoma
   or newer is required.
2. Open the DMG, drag Ollama into `Applications`, and launch it.
3. If prompted, allow Ollama to add its command-line tool to `/usr/local/bin`.

**Linux**

Run the official install script:

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

If Ollama is not already running as a service, start it in a separate terminal and leave that
terminal open:

```bash
ollama serve
```

See the official [Linux installation guide](https://docs.ollama.com/linux) for manual installation
and system service instructions.

##### Set up and use Ollama with Torenta

After Ollama is running, download Torenta's configured model once:

```bash
ollama pull qwen3:8b
```

Start Torenta, open `/search`, and use the AI Concierge. Torenta connects to Ollama at
`http://localhost:11434`; you do not need to run the model separately.

The tracked `src/main/resources/application.properties` contains the non-sensitive defaults for
Ollama at `http://localhost:11434` with `qwen3:8b`.

To use OpenAI instead, save the API key on the Preferences page and set
`app.ai.provider=OPENAI`. Never add the key to a tracked file. Provider models can be changed with
`app.ai.ollama.model` or `app.ai.openai.model`. The concierge uses typed, evidence-backed filters
covering the current TMDB movie/TV Discover API. Unresolved names remain ranking criteria rather
than becoming raw query parameters. TMDB supplies all candidate facts; the model only extracts,
ranks, and explains, and it never starts downloads.

Ollama and OpenAI calls time out after 120 seconds; the concierge UI stops waiting after 130
seconds and displays an error instead of remaining in its loading state.

For local diagnostics, set `app.ai.logging.enabled=true`. This logs the intent and ranking
prompts, structured AI responses, and TMDB requests/responses in execution order. TMDB API keys
are removed from logged URLs. Keep it disabled outside local development because prompts and
candidate metadata can contain user-provided or sensitive text.

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
- [docs/AI_CONCIERGE.md](docs/AI_CONCIERGE.md) — AI provider setup, architecture, and API examples
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
