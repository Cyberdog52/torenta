const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p';
const FALLBACK_IMAGE = 'tvnotfound.png';

/** Poster-sized TMDB image, or the local fallback when TMDB has none. */
export function posterUrl(path: string | null | undefined): string {
  return path ? `${TMDB_IMAGE_BASE}/w500/${path}` : FALLBACK_IMAGE;
}

/** Full-size TMDB backdrop, or the local fallback when TMDB has none. */
export function backdropUrl(path: string | null | undefined): string {
  return path ? `${TMDB_IMAGE_BASE}/original/${path}` : FALLBACK_IMAGE;
}
