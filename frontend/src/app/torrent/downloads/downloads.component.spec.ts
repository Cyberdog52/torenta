import { TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { DownloadsComponent } from './downloads.component';
import { TorrentService } from '../torrent.service';
import { NotificationService } from '../../shared/notification/notification.service';
import { DownloadDto } from '../../shared/dto/torrent/DownloadDto';
import { DownloadState } from '../../shared/dto/torrent/DownloadState';
import { Notification } from '../../shared/dto/notification/Notification';

const mockDto = {
  id: 1,
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

  function createComponent(): DownloadsComponent {
    return TestBed.runInInjectionContext(() => new DownloadsComponent());
  }

  beforeEach(() => {
    downloads$ = new Subject<DownloadDto[]>();
    notifications = [];

    TestBed.configureTestingModule({
      providers: [
        { provide: TorrentService, useValue: { downloads$ } },
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
});
