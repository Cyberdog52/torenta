import {Episode} from "../tmdb/Episode";
import {PirateBayEntry} from "../pirateBay/PirateBayEntry";
import {SeriesDetail} from "../tmdb/SeriesDetail";

export interface DownloadRequest {
  episode: Episode;
  seriesDetail: SeriesDetail;
  pirateBayEntry: PirateBayEntry;
}
