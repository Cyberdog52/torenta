const TMDB_IMAGE_BASE = 'https://image.tmdb.org/t/p';
// Absolute so it also resolves correctly if this ever renders from a nested
// route (relative URLs resolve against the current path, not `<base href>`).
const FALLBACK_IMAGE = '/tvnotfound.png';

/**
 * Poster-sized TMDB image, or the local fallback when TMDB has none.
 * `w342` is the smallest TMDB poster size that still looks sharp at 2x
 * pixel density for the ~150px-wide thumbnails this app renders.
 */
export function posterUrl(path: string | null | undefined): string {
  return path ? `${TMDB_IMAGE_BASE}/w342/${path}` : FALLBACK_IMAGE;
}

/**
 * Backdrop-sized TMDB image, or the local fallback when TMDB has none.
 * `w1280` comfortably covers the detail panel's backdrop at any supported
 * viewport width without downloading the multi-megabyte `original` size.
 */
export function backdropUrl(path: string | null | undefined): string {
  return path ? `${TMDB_IMAGE_BASE}/w1280/${path}` : FALLBACK_IMAGE;
}
