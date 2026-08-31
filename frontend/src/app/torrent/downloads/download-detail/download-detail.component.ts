import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { DownloadDto } from '../../../shared/dto/torrent/DownloadDto';
import { DownloadState } from '../../../shared/dto/torrent/DownloadState';
import {
  DownloadRequestDto,
  getDownloadTitle,
} from '../../../shared/dto/torrent/DownloadRequestDto';
import { backdropUrl } from '../../../shared/tmdb-images';

const BYTES_PER_SECOND_IN_MBIT = 125_000;

@Component({
  selector: 'app-download-detail',
  imports: [MatIconModule, MatProgressBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './download-detail.component.scss',
  templateUrl: './download-detail.component.html',
})
export class DownloadDetailComponent {
  readonly downloadDto = input.required<DownloadDto>();

  protected readonly isRunning = computed(() => this.downloadDto().state === DownloadState.STARTED);

  protected readonly title = computed(() => getDownloadTitle(this.downloadDto().downloadRequest));

  protected readonly backgroundImage = computed(() =>
    backdropUrl(backdropPathOf(this.downloadDto().downloadRequest)),
  );

  protected readonly progressString = computed(() => {
    const download = this.downloadDto();
    switch (download.state) {
      case DownloadState.FINISHED:
        return 'Successfully downloaded';
      case DownloadState.CANCELLED:
        return 'Cancelled';
      case DownloadState.STARTED:
        return `${(download.progress * 100).toFixed(1)} %`;
      default:
        return download.state;
    }
  });

  protected readonly speed = computed(() => {
    const bytesPerSecond = this.downloadDto().downloadSpeedInBytesPerSecond;
    if (!this.isRunning() || bytesPerSecond == null || bytesPerSecond < 0.1) {
      return '0 Mbps';
    }
    return `${(bytesPerSecond / BYTES_PER_SECOND_IN_MBIT).toFixed(2)} Mbps`;
  });

  protected readonly estimatedTimeFinished = computed(() => {
    const download = this.downloadDto();
    if (!this.isRunning()) {
      return 'Finished';
    }
    const bytesPerSecond = download.downloadSpeedInBytesPerSecond;
    if (bytesPerSecond == null || bytesPerSecond < 0.1) {
      return 'Never';
    }
    const secondsLeft = (download.totalBytes * (1 - download.progress)) / bytesPerSecond;
    if (!Number.isFinite(secondsLeft) || secondsLeft < 0) {
      return 'Unknown';
    }
    return formatDuration(secondsLeft);
  });

  protected readonly peers = computed(() => {
    const connectedPeers = this.downloadDto().connectedPeers;
    return !connectedPeers ? 'No connections' : `${connectedPeers} sources`;
  });
}

function backdropPathOf(downloadRequest: DownloadRequestDto): string | null {
  return (
    downloadRequest.seriesDetail?.backdrop_path ??
    downloadRequest.movieDetail?.backdrop_path ??
    null
  );
}

/**
 * Formats a duration in seconds as `hh:mm:ss`, growing to `d:hh:mm:ss` past 24
 * hours instead of silently wrapping (the previous `Date#toISOString` hack
 * threw the day count away, so a 30-hour ETA rendered as `06:00:00`).
 */
function formatDuration(totalSeconds: number): string {
  const seconds = Math.floor(totalSeconds % 60);
  const minutes = Math.floor(totalSeconds / 60) % 60;
  const hours = Math.floor(totalSeconds / 3600) % 24;
  const days = Math.floor(totalSeconds / 86_400);

  const pad = (value: number) => String(value).padStart(2, '0');
  const hhmmss = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
  return days > 0 ? `${days}:${hhmmss}` : hhmmss;
}
