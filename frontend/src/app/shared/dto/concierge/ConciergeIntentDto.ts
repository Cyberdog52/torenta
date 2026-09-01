import { ConciergeMediaType } from './ConciergeMediaType';

export type FilterOperator = 'EQ' | 'GTE' | 'LTE';
export type FilterMatch = 'ALL' | 'ANY';
export type FilterPolarity = 'INCLUDE' | 'EXCLUDE';

export type NumericFilterKey =
  | 'YEAR'
  | 'PRIMARY_RELEASE_YEAR'
  | 'FIRST_AIR_DATE_YEAR'
  | 'VOTE_AVERAGE'
  | 'VOTE_COUNT'
  | 'RUNTIME'
  | 'PAGE';

export type DateFilterKey = 'PRIMARY_RELEASE_DATE' | 'RELEASE_DATE' | 'FIRST_AIR_DATE' | 'AIR_DATE';

export type TextFilterKey =
  | 'LANGUAGE'
  | 'ORIGINAL_LANGUAGE'
  | 'ORIGIN_COUNTRY'
  | 'REGION'
  | 'WATCH_REGION'
  | 'TIMEZONE'
  | 'CERTIFICATION'
  | 'CERTIFICATION_GTE'
  | 'CERTIFICATION_LTE'
  | 'CERTIFICATION_COUNTRY';

export type BooleanFilterKey =
  'INCLUDE_ADULT' | 'INCLUDE_VIDEO' | 'INCLUDE_NULL_FIRST_AIR_DATES' | 'SCREENED_THEATRICALLY';

export type NamedFilterKey =
  'GENRE' | 'CAST' | 'CREW' | 'PEOPLE' | 'COMPANY' | 'KEYWORD' | 'NETWORK' | 'WATCH_PROVIDER';

export type EnumFilterKey =
  'SORT_BY' | 'RELEASE_TYPE' | 'WATCH_MONETIZATION_TYPE' | 'TV_STATUS' | 'TV_TYPE';

export interface NumericFilterCriterion {
  key: NumericFilterKey;
  operator: FilterOperator;
  value: number;
  evidence: string;
}

export interface DateFilterCriterion {
  key: DateFilterKey;
  operator: FilterOperator;
  value: string;
  evidence: string;
}

export interface TextFilterCriterion {
  key: TextFilterKey;
  value: string;
  evidence: string;
}

export interface BooleanFilterCriterion {
  key: BooleanFilterKey;
  value: boolean;
  evidence: string;
}

export interface NamedFilterCriterion {
  key: NamedFilterKey;
  names: string[];
  polarity: FilterPolarity;
  matching: FilterMatch;
  evidence: string;
}

export interface EnumFilterCriterion {
  key: EnumFilterKey;
  values: string[];
  matching: FilterMatch;
  evidence: string;
}

export interface ConciergeIntentDto {
  mediaType: ConciergeMediaType;
  moods: string[];
  similarTo: string | null;
  numericFilters: NumericFilterCriterion[];
  dateFilters: DateFilterCriterion[];
  textFilters: TextFilterCriterion[];
  booleanFilters: BooleanFilterCriterion[];
  namedFilters: NamedFilterCriterion[];
  enumFilters: EnumFilterCriterion[];
}
