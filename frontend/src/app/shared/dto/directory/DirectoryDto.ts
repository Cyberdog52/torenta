import {FileDto} from "./FileDto";

export interface DirectoryDto {
  name: string;
  files: FileDto[];
  directories: DirectoryDto[];
  absolutePath: string;
}
