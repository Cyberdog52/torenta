import {MoviesRootDirectoryDto} from "./MoviesRootDirectoryDto";
import {SeriesRootDirectoryDto} from "./SeriesRootDirectoryDto";

export interface FileHierarchyDto {

  moviesRootDirectoryDto: MoviesRootDirectoryDto;
  seriesRootDirectoryDto: SeriesRootDirectoryDto;
  absolutePath: string;
}
