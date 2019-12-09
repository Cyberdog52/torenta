import {DownloadState} from "./DownloadState";
import {DownloadRequest} from "./DownloadRequest";

export interface DownloadDto {
  id: number;
  state: DownloadState;
  progress: number;
  downloadRequest: DownloadRequest;
  startTimeInMs: number;
}
