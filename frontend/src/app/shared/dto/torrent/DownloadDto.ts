import {DownloadState} from "./DownloadState";

export interface DownloadDto {
  id: number;
  state: DownloadState;
  progress: number;
  magnetLink: string;
}
