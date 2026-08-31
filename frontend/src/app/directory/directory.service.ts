import { Injectable, Signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { DirectoryDto } from '../shared/dto/directory/DirectoryDto';

const BACKEND_URL = 'api/directory';

@Injectable({ providedIn: 'root' })
export class DirectoryService {
  seriesDirectoryResource(seriesName: Signal<string | undefined>) {
    return httpResource<DirectoryDto>(() => {
      const name = seriesName();
      return name ? `${BACKEND_URL}/series/${encodeURIComponent(name)}` : undefined;
    });
  }

  movieDirectoryResource(
    movieTitle: Signal<string | undefined>,
    releaseYear: Signal<number | undefined>,
  ) {
    return httpResource<DirectoryDto>(() => {
      const title = movieTitle();
      const year = releaseYear();
      if (!title || year == null || Number.isNaN(year)) {
        return undefined;
      }
      return {
        url: `${BACKEND_URL}/movie/${encodeURIComponent(title)}`,
        params: { releaseYear: year },
      };
    });
  }
}
