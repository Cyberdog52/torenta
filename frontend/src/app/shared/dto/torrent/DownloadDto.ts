import { DownloadState } from './DownloadState';
import { DownloadRequestDto } from './DownloadRequestDto';

export interface DownloadDto {
  id: string;
  state: DownloadState;
  failureKind: DownloadFailureKind | null;
  displayTitle: string | null;
  progress: number;
  downloadRequest: DownloadRequestDto | null;
  startTimeInMs: number;
  connectedPeers: number;
  totalBytes: number;
  downloadSpeedInBytesPerSecond: number;
  errorMessage?: string;
  capabilities: DownloadActionCapabilities;
}

export interface DownloadActionCapabilities {
  canPause: boolean;
  canRestart: boolean;
  canStopAndDelete: boolean;
  canRemove: boolean;
}

export enum DownloadFailureKind {
  RESTARTABLE = 'RESTARTABLE',
  CLEANUP_ONLY = 'CLEANUP_ONLY',
}
