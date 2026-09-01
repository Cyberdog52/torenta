import { TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { DownloadsComponent } from './downloads.component';
import { TorrentService } from '../torrent.service';
import { NotificationService } from '../../shared/notification/notification.service';
import { DownloadDto } from '../../shared/dto/torrent/DownloadDto';
import { DownloadState } from '../../shared/dto/torrent/DownloadState';
import { Notification } from '../../shared/dto/notification/Notification';
import { MatDialog } from '@angular/material/dialog';

const mockDto = {
  id: 'download-1',
  displayTitle: 'test S01E01',
  failureKind: null,
  capabilities: {
    canPause: true,
    canRestart: false,
    canStopAndDelete: true,
    canRemove: false,
  },
  downloadRequest: {
    tmdbEpisode: { season_number: 1, episode_number: 1 },
    seriesDetail: { name: 'test' },
    torrentEntry: {},
    movieDetail: null,
  },
  progress: 1,
  startTimeInMs: 100,
} as unknown as DownloadDto;

describe('DownloadsComponent', () => {
  let downloads$: Subject<DownloadDto[]>;
  let notifications: Notification[];
  let pauseCalls: string[];

  function createComponent(): DownloadsComponent {
    return TestBed.runInInjectionContext(() => new DownloadsComponent());
  }

  beforeEach(() => {
    downloads$ = new Subject<DownloadDto[]>();
    notifications = [];
    pauseCalls = [];

    TestBed.configureTestingModule({
      providers: [
        {
          provide: TorrentService,
          useValue: {
            downloads$,
            pauseTorrent: (id: string) => {
              pauseCalls.push(id);
              return of(undefined);
            },
            restartTorrent: () => of(undefined),
            stopAndDeleteTorrent: () => of(undefined),
            removeTorrentTile: () => of(undefined),
          },
        },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(true) }) },
        },
        {
          provide: NotificationService,
          useValue: { notify: (n: Notification) => notifications.push(n) },
        },
      ],
    });
  });

  it('creates a working test setup', () => {
    expect(createComponent()).toBeTruthy();
    expect(TestBed.inject(TorrentService)).toBeTruthy();
    expect(TestBed.inject(NotificationService)).toBeTruthy();
  });

  it('tracks the downloads emitted by the torrent service', () => {
    const component = createComponent() as unknown as { downloads: () => DownloadDto[] };

    expect(component.downloads()).toHaveLength(0);

    downloads$.next([{ ...mockDto, state: DownloadState.STARTED }]);
    expect(component.downloads()).toHaveLength(1);
    expect(component.downloads()[0].state).toBe(DownloadState.STARTED);

    downloads$.next([{ ...mockDto, state: DownloadState.FINISHED }]);
    expect(component.downloads()).toHaveLength(1);
    expect(component.downloads()[0].state).toBe(DownloadState.FINISHED);
  });

  it('notifies once a download transitions to FINISHED', () => {
    createComponent();

    downloads$.next([{ ...mockDto, state: DownloadState.STARTED }]);
    expect(notifications).toHaveLength(0);

    downloads$.next([{ ...mockDto, state: DownloadState.FINISHED }]);
    expect(notifications).toHaveLength(1);
    expect(notifications[0].content).toContain('successfully downloaded');
  });

  it('does not notify again while the state stays FINISHED', () => {
    createComponent();

    downloads$.next([{ ...mockDto, state: DownloadState.STARTED }]);
    downloads$.next([{ ...mockDto, state: DownloadState.FINISHED }]);
    downloads$.next([{ ...mockDto, state: DownloadState.FINISHED }]);

    expect(notifications).toHaveLength(1);
  });

  it('runs a requested action and notifies on success', () => {
    const component = createComponent() as unknown as {
      requestAction: (download: DownloadDto, action: 'pause') => void;
    };
    const download = { ...mockDto, state: DownloadState.STARTED };

    component.requestAction(download, 'pause');

    expect(pauseCalls).toEqual(['download-1']);
    expect(notifications.at(-1)?.content).toContain('paused');
  });
});
