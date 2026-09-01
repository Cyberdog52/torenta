import { TestBed } from '@angular/core/testing';
import { DownloadDto } from '../../../shared/dto/torrent/DownloadDto';
import { DownloadState } from '../../../shared/dto/torrent/DownloadState';
import { DownloadAction, DownloadDetailComponent } from './download-detail.component';

const download: DownloadDto = {
  id: 'abc123',
  state: DownloadState.PAUSED,
  failureKind: null,
  displayTitle: 'Example S01E02',
  progress: 0.42,
  downloadRequest: null,
  startTimeInMs: 1,
  activeDownloadTimeInMs: 0,
  connectedPeers: 0,
  totalBytes: 100,
  downloadSpeedInBytesPerSecond: 0,
  errorMessage: undefined,
  capabilities: {
    canPause: false,
    canRestart: true,
    canStopAndDelete: true,
    canRemove: false,
  },
};

describe('DownloadDetailComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DownloadDetailComponent],
    }).compileComponents();
  });

  it('renders paused progress and only the backend-approved actions', () => {
    const fixture = TestBed.createComponent(DownloadDetailComponent);
    fixture.componentRef.setInput('downloadDto', download);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    expect(host.textContent).toContain('Paused');
    expect(host.textContent).toContain('42.0 % paused');
    expect(host.querySelector('[aria-label="Restart download"]')).not.toBeNull();
    expect(host.querySelector('[aria-label="Stop and delete files"]')).not.toBeNull();
    expect(host.querySelector('[aria-label="Pause download"]')).toBeNull();
    expect(host.querySelector('[aria-label="Remove from downloads"]')).toBeNull();
  });

  it('emits the requested action and disables controls while busy', () => {
    const fixture = TestBed.createComponent(DownloadDetailComponent);
    fixture.componentRef.setInput('downloadDto', download);
    fixture.componentRef.setInput('busy', true);
    const actions: DownloadAction[] = [];
    fixture.componentInstance.actionRequested.subscribe((action) => actions.push(action));
    fixture.detectChanges();
    const restart = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '[aria-label="Restart download"]',
    );

    expect(restart?.disabled).toBe(true);
    restart?.click();
    expect(actions).toEqual([]);
  });

  it('shows Not started instead of Never remaining before data starts', () => {
    const fixture = TestBed.createComponent(DownloadDetailComponent);
    fixture.componentRef.setInput('downloadDto', {
      ...download,
      state: DownloadState.STARTED,
      capabilities: { ...download.capabilities, canPause: true },
    });
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent;

    expect(text).toContain('Not started');
    expect(text).not.toContain('Never remaining');
  });

  it('keeps stats visible while paused and summarizes finished performance', () => {
    const fixture = TestBed.createComponent(DownloadDetailComponent);
    fixture.componentRef.setInput('downloadDto', download);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No source information');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No speed information');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'No time estimate while paused',
    );

    fixture.componentRef.setInput('downloadDto', {
      ...download,
      state: DownloadState.FINISHED,
      progress: 1,
      totalBytes: 125_000_000,
      activeDownloadTimeInMs: 100_000,
      capabilities: {
        canPause: false,
        canRestart: false,
        canStopAndDelete: false,
        canRemove: true,
      },
    });
    fixture.detectChanges();
    const finishedText = (fixture.nativeElement as HTMLElement).textContent;

    expect(finishedText).toContain('10.00 Mbps average');
    expect(finishedText).toContain('Downloaded in 00:01:40');
  });
});
