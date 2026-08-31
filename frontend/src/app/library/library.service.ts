import { Injectable, Signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { Series } from '../shared/dto/library/Series';

const BACKEND_URL = 'api/library';

@Injectable({ providedIn: 'root' })
export class LibraryService {
  seriesInLibraryResource(name: Signal<string | undefined>) {
    return httpResource<Series>(() => {
      const seriesName = name();
      return seriesName ? `${BACKEND_URL}/tv/${encodeURIComponent(seriesName)}` : undefined;
    });
  }
}
