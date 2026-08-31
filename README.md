# torenta

## Setup

#### Java
Install Java OpenJDK 25 (the build uses the Java 25 toolchain configured in `build.gradle`).
Make sure it is also in the system's path, because gradlew will use the java version that is on the path.
To see which java version is used, type the following in the terminal:

```
Windows: 
for %i in (java.exe) do @echo.   %~$PATH:i

Linux/Unix/Mac OS X: 
which java
```


#### IntelliJ
Install new version of IntelliJ Idea (minimum 2018.3) from [here](https://www.jetbrains.com/idea/download/).
The enterprise version is recommended.

#### TMDB-Key
First, you need to get an API key from [TMDB](https://developers.themoviedb.org/3/getting-started/introduction). Create an `application.properties` from the template and add the key. Do not check in the `application.properties` file.

#### NodeJs
This frontend depends on an Angular 6 toolchain and must be run with **Node.js 10-16** (pinned to 16 in `.nvmrc`).

```bash
nvm use
```

If you do not have the required runtime installed yet:

```bash
nvm install 16
nvm use 16
```

Install newest version of NodeJs from [here](https://nodejs.org/en/download/).
Restart IntelliJ and execute the following code:
```
cd frontend
npm install
```

## Run

#### Run Backend:



Execute TorentaApplication through RunConfigurations

Will start at http://localhost:8080/

#### Run Frontend:

```
cd frontend
npm start
```

Will start at http://localhost:4200/

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
