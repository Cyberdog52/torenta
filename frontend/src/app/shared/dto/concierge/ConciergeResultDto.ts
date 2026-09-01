import { ConciergeMediaType } from './ConciergeMediaType';

export interface ConciergeResultDto {
  rank: number;
  mediaType: Exclude<ConciergeMediaType, 'ANY'>;
  id: number;
  title: string;
  overview: string;
  posterPath: string | null;
  releaseDate: string | null;
  rating: number;
  explanation: string;
}
