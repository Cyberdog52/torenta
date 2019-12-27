import {DirectoryDto} from "./DirectoryDto";

export interface MoviesRootDirectoryDto {
  movies: DirectoryDto[];
  absolutePath: string;
}
