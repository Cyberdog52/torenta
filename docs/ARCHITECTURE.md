# Architecture

High-level overview of `torenta` — a media discovery and BitTorrent download manager. This
document complements [`AI_RULES.md`](../AI_RULES.md), which holds the authoritative security and
architecture **rules**; this file describes the runtime **structure and data flow**.

## Components

```
┌─────────────────────────┐        HTTP (/api/**, dev proxy)        ┌───────────────────────────┐
│  Angular 6 SPA          │  ─────────────────────────────────▶     │  Spring Boot 2.2 REST API │
│  frontend/ (:4200)      │                                         │  Java 11 (:8080)          │
│  Material, RxJS,        │  ◀─────────────────────────────────     │                           │
│  shared/dto mirrors     │            JSON DTOs                    │                           │
└─────────────────────────┘                                         └─────────────┬─────────────┘
                                                                                  │
                     ┌─────────────────────────────┬───────────────────┬──────────┴────────────┐
                     ▼                             ▼                   ▼                       ▼
              TMDB REST API              PirateBay (HTML scrape)   bt/** engine          local filesystem
              (movie/series             via jsoup                 (vendored BitTorrent   (media library,
               metadata)                                           library)               directories)
```

## Backend feature slices

Root package `ch.andreskonrad.torenta`. Each feature is a vertical slice
(`controller` → `service` → `dto`); see `AI_RULES.md §3`.

| Feature      | Responsibility                                                        | Notable classes                                  |
|--------------|-----------------------------------------------------------------------|--------------------------------------------------|
| `tmdb`       | Search/fetch movie & series metadata from TMDB; throttles + caches    | `TmdbService`, `RequestThrottler`                |
| `torrent`    | Find torrents by scraping PirateBay HTML                              | `TorrentService`, `PirateBayHtmlAPI`             |
| `bittorrent` | Start/track downloads via the vendored `bt` engine                   | `BitTorrentService`, `Download`                  |
| `library`    | Model & manage the local media library (series/seasons/episodes)     | `LibraryService`, `Series`/`Season`/`Episode`    |
| `directory`  | Browse local directories/files                                        | `DirectoryService`                               |
| `preference` | Persist user preferences                                             | `PreferenceService`, `UserPreference`            |

Cross-cutting config lives at the package root: `TorentaApplication` (entry point),
`CustomCacheConfig` + `CacheCustomizer` (Spring Cache), `SwaggerConfig` (Springfox).

## Vendored library

`src/main/java/bt/**` is a **third-party BitTorrent library** (see the references in
`README.md`). It is wired via Guice and consumed by the `bittorrent` feature. **Do not refactor,
reformat, or clean it up** — only touch it for a deliberate, user-approved integration fix
(`AI_RULES.md §3.2`).

## Typical request flow (download a series episode)

1. **Discover** — SPA calls `TmdbController` → `TmdbService` fetches metadata from TMDB
   (throttled by `RequestThrottler`, cached via `@Cacheable`).
2. **Find torrent** — SPA calls `TorrentController` → `TorrentService` → `PirateBayHtmlAPI`
   scrapes result pages with jsoup and returns `TorrentEntry` DTOs.
3. **Download** — SPA calls `BittorrentController` → `BitTorrentService` starts a `Download`
   driven by the vendored `bt/**` engine; progress is exposed as `DownloadState`.
4. **Library** — completed media is surfaced through `LibraryService`/`DirectoryService`.

## Frontend

Angular 6 SPA under `frontend/src/app/`. Feature folders each have components and a
`*.service.ts` (`@Injectable({ providedIn: 'root' })`) that calls the backend under `api/...`
(dev proxy in `frontend/proxy.conf.json`). Shared DTO interfaces in
`frontend/src/app/shared/dto/**` **mirror the backend DTOs** — keep them in sync when changing an
endpoint (`AI_RULES.md §3.5`).

## Configuration

Runtime config (TMDB key, etc.) is injected via `@Value("${...}")` from
`application.properties`, which is **protected and never committed**. Use
`application-template.properties` for placeholder values. See `AI_RULES.md §1` for the full list
of protected files.
