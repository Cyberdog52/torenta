import {TestBed} from "@angular/core/testing";
import {DownloadsComponent} from "./downloads.component";
import {TorrentService} from "../torrent.service";
import {NotificationService} from "../../shared/notification/notification.service";
import {DownloadRequestDto} from "../../shared/dto/torrent/DownloadRequestDto";
import {of, Subject} from 'rxjs';
import {DownloadDto} from "../../shared/dto/torrent/DownloadDto";
import {DownloadState} from "../../shared/dto/torrent/DownloadState";
import {Notification} from "../../shared/dto/notification/Notification";


const torrentServiceMock = {
  torrents: new Subject(),
  getDownloadDtosObservable: () => {
    return torrentServiceMock.torrents;
  },
  startTorrent: (downloadRequest: DownloadRequestDto) => {
    return of(true)
  }
};

const notificationServiceMock = {
  notifications: [],
  getNotificationsObservable: () => {
    return of(notificationServiceMock.notifications)
  },
  addNotifications: (notification: Notification) => {
    notificationServiceMock.notifications.push(notification)
  }
};

const mockDto = <DownloadDto>{
  id: 1,
  downloadRequest: {
    tmdbEpisode: {
      season_number: 1,
      episode_number: 1
    },
    seriesDetail: {
      name: 'test'
    },
    torrentEntry: {}
  },
  progress: 1,
  startTimeInMs: 100
};

describe('DownloadsComponent', () => {

  beforeEach(() =>  {
    notificationServiceMock.notifications = []
  });


  it('should create a working test setup', () => {
    TestBed.configureTestingModule({
      providers: [
        DownloadsComponent,
        {provide: TorrentService, useValue: torrentServiceMock},
        {provide: NotificationService, useValue: notificationServiceMock}
      ]
    });

    const downloadComponent = TestBed.get(DownloadsComponent);
    expect(downloadComponent).toBeTruthy();
    const notificationService = TestBed.get(NotificationService);
    expect(notificationService).toBeTruthy();
    const torrentService = TestBed.get(TorrentService);
    expect(torrentService).toBeTruthy();
  });

  it('should subscribe to torrentService onInit', () => {
    TestBed.configureTestingModule({
      providers: [
        DownloadsComponent,
        {provide: TorrentService, useValue: torrentServiceMock},
        {provide: NotificationService, useValue: notificationServiceMock}
      ]
    });

    const downloadComponent: DownloadsComponent = TestBed.get(DownloadsComponent);
    expect(downloadComponent.downloadDtos.length).toBe(0);

    downloadComponent.ngOnInit();
    torrentServiceMock.torrents.next([{
      ...mockDto,
      state: DownloadState.STARTED
    }]);

    expect(downloadComponent.downloadDtos.length).toBe(1);
    expect(downloadComponent.downloadDtos[0].state).toBe(DownloadState.STARTED);

    torrentServiceMock.torrents.next([{
        ...mockDto,
      state: DownloadState.FINISHED
    }]);

    expect(downloadComponent.downloadDtos.length).toBe(1);
    expect(downloadComponent.downloadDtos[0].state).toBe(DownloadState.FINISHED);
  });

  it('should add notification on finished download', () => {
    TestBed.configureTestingModule({
      providers: [
        DownloadsComponent,
        {provide: TorrentService, useValue: torrentServiceMock},
        {provide: NotificationService, useValue: notificationServiceMock}
      ]
    });

    const downloadComponent: DownloadsComponent = TestBed.get(DownloadsComponent);
    downloadComponent.ngOnInit();

    torrentServiceMock.torrents.next([{
      ...mockDto,
      state: DownloadState.STARTED
    }]);

    expect(notificationServiceMock.notifications.length).toBe(0);


    torrentServiceMock.torrents.next([{
      ...mockDto,
      state: DownloadState.FINISHED
    }]);

    expect(notificationServiceMock.notifications.length).toBeGreaterThan(0);

  });
});
