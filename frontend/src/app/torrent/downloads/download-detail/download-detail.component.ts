import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { DownloadDto } from '../../../shared/dto/torrent/DownloadDto';
import { DownloadState } from '../../../shared/dto/torrent/DownloadState';
import {
  DownloadRequestDto,
  getDownloadTitle,
} from '../../../shared/dto/torrent/DownloadRequestDto';
import { backdropUrl } from '../../../shared/tmdb-images';

const BYTES_PER_SECOND_IN_MBIT = 125_000;

/** Label + icon shown on the status chip and the progress row for each state. */
const STATUS_META: Record<DownloadState, { label: string; icon: string }> = {
  [DownloadState.STARTED]: { label: 'Downloading', icon: 'downloading' },
  [DownloadState.FINISHED]: { label: 'Finished', icon: 'check_circle' },
  [DownloadState.CANCELLED]: { label: 'Cancelled', icon: 'cancel' },
};

@Component({
  selector: 'app-download-detail',
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './download-detail.component.scss',
  templateUrl: './download-detail.component.html',
})
export class DownloadDetailComponent {
  readonly downloadDto = input.required<DownloadDto>();

  protected readonly isRunning = computed(() => this.downloadDto().state === DownloadState.STARTED);

  /** CSS modifier class driving the state-dependent colors in the stylesheet. */
  protected readonly stateClass = computed(() => `state-${this.downloadDto().state.toLowerCase()}`);

  protected readonly status = computed(() => STATUS_META[this.downloadDto().state]);

  protected readonly title = computed(() => getDownloadTitle(this.downloadDto().downloadRequest));

  protected readonly backgroundImage = computed(() =>
    backdropUrl(backdropPathOf(this.downloadDto().downloadRequest)),
  );

  /** Drives the custom progress bar's fill width and its ARIA value. */
  protected readonly progressPercent = computed(() => this.downloadDto().progress * 100);

  protected readonly progressString = computed(() => {
    switch (this.downloadDto().state) {
      case DownloadState.FINISHED:
        return 'Successfully downloaded';
      case DownloadState.CANCELLED:
        return 'Cancelled';
      case DownloadState.STARTED:
        return `${this.progressPercent().toFixed(1)} %`;
      default:
        return this.downloadDto().state;
    }
  });

  protected readonly speed = computed(() => {
    const bytesPerSecond = this.downloadDto().downloadSpeedInBytesPerSecond;
    if (!this.isRunning() || bytesPerSecond == null || bytesPerSecond < 0.1) {
      return '0 Mbps';
    }
    return `${(bytesPerSecond / BYTES_PER_SECOND_IN_MBIT).toFixed(2)} Mbps`;
  });

  /**
   * Only meaningful while a download is actively running; the template only
   * reads this while `isRunning()` is true.
   */
  protected readonly estimatedTimeFinished = computed(() => {
    const download = this.downloadDto();
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

  /**
   * Full status text for the progress bar's `aria-valuetext`, read on every
   * focus/inspection of the bar (e.g. "42.3% downloaded, 00:12:30 remaining").
   */
  protected readonly progressValueText = computed(() => {
    switch (this.downloadDto().state) {
      case DownloadState.FINISHED:
        return 'Finished';
      case DownloadState.CANCELLED:
        return 'Cancelled';
      case DownloadState.STARTED:
        return `${this.progressPercent().toFixed(1)}% downloaded, ${this.estimatedTimeFinished()} remaining`;
      default:
        return this.downloadDto().state;
    }
  });

  /**
   * Rounded-to-10% text for the per-card live region. Rounding (rather than
   * the exact 1-second-polled percentage) means the string only actually
   * changes a handful of times over a whole download, so screen readers
   * announce meaningful milestones instead of being spammed every second.
   */
  protected readonly progressAnnouncement = computed(() => {
    const download = this.downloadDto();
    switch (download.state) {
      case DownloadState.FINISHED:
        return `${this.title()} finished downloading.`;
      case DownloadState.CANCELLED:
        return `${this.title()} download cancelled.`;
      case DownloadState.STARTED: {
        const roundedPercent = Math.floor(this.progressPercent() / 10) * 10;
        return `${this.title()}: ${roundedPercent}% downloaded.`;
      }
      default:
        return '';
    }
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
 * Formats a duration in seconds as `hh:mm:ss`, growing to `d:hh:mm:ss` past
 * 24 hours instead of silently wrapping the day count away.
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
