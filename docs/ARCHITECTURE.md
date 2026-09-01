# Architecture

High-level overview of `torenta` — a media discovery and BitTorrent download manager. This
document complements [`AI_RULES.md`](../AI_RULES.md), which holds the authoritative security and
architecture **rules**; this file describes the runtime **structure and data flow**.

## Components

```
┌─────────────────────────┐        HTTP (/api/**, dev proxy)        ┌───────────────────────────┐
│  Angular 6 SPA          │  ─────────────────────────────────▶     │  Spring Boot 4.1 REST API │
│  frontend/ (:4200)      │                                         │  Java 25 (:8080)          │
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
| `recommendation` | Recommend the next un-downloaded aired episode(s) per series in the library | `RecommendationService`                     |

Cross-cutting config lives at the package root: `TorentaApplication` (entry point) and
`CustomCacheConfig` + `CacheCustomizer` (Spring Cache). Springdoc auto-configures OpenAPI.

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
   driven by the vendored `bt/**` engine; progress is exposed as `DownloadState`. Failures and
   downloads whose processing chain terminates before completion are logged and reported as
   `DownloadState.FAILED` with an `errorMessage` on `DownloadDto`. A download that finds no peers
   within `Download.PEER_DISCOVERY_TIMEOUT_IN_MS` is also reported as `FAILED` instead of sitting
   at 0 % forever. `BitTorrentService` sets the engine's acceptor address from
   `RoutableAddressResolver`, because the vendored default picks the first non-loopback IPv4
   address of any interface (often a Hyper-V/WSL/VPN adapter with no internet route).
4. **Library** — completed media is surfaced through `LibraryService`/`DirectoryService`.
5. **Recommend** — the Recommendations page calls `RecommendationController` →
   `RecommendationService`, which by default lists series folders under
   `<download-root>/Series` touched within the last 14 days (`days` query param; `0` scans the
   whole library instead). The filter checks *directory* mtimes, not file mtimes — BitTorrent
   downloads and archive extraction routinely preserve old file timestamps — via
   `DirectoryService.getSeriesNamesModifiedWithin`/`getAllSeriesNames`, trading completeness for
   speed on very large libraries. Each candidate series is resolved via `LibraryService`, and up
   to 3 missing aired episodes are returned per series (chronologically, spanning into the next
   TMDB season even if it has no local folder yet), each carrying the raw TMDB series/episode
   objects so the frontend can start a download without another round trip. The response
   (`RecommendationResultDto`) also reports how many series folders were scanned and lists any
   that could not be resolved to a TMDB show, so a user can tell "nothing missing" apart from
   "couldn't check this" from the UI alone. Results are cached
   (`CustomCacheConfig.RECOMMENDATION_CACHE_NAME`) and evicted every 5 minutes. Each recommended
   series card additionally has its own `SeriesTorrentsComponent`, which lazily searches
   `TorrentService` (same `<series> S0xE0y` query convention as the Search page) for the top 3
   trusted/VIP torrents by seeders and can start a download directly from the page.

## Frontend

Angular 6 SPA under `frontend/src/app/`. Feature folders each have components and a
`*.service.ts` (`@Injectable({ providedIn: 'root' })`) that calls the backend under `api/...`
(dev proxy in `frontend/proxy.conf.json`). Shared DTO interfaces in
`frontend/src/app/shared/dto/**` **mirror the backend DTOs** — keep them in sync when changing an
endpoint (`AI_RULES.md §3.5`).
