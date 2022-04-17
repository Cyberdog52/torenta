import {DownloadState} from "./DownloadState";
import {DownloadRequestDto} from "./DownloadRequestDto";

export interface DownloadDto {
  id: number;
  state: DownloadState;
  progress: number;
  downloadRequest: DownloadRequestDto;
  startTimeInMs: number;
  connectedPeers: number;
  totalBytes: number;
  downloadSpeedInBytesPerSecond: number;
}
