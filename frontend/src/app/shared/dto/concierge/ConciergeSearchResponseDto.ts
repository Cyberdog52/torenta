import { ConciergeIntentDto } from './ConciergeIntentDto';
import { ConciergeResultDto } from './ConciergeResultDto';

export interface ConciergeSearchResponseDto {
  intent: ConciergeIntentDto;
  results: ConciergeResultDto[];
}
