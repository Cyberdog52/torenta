interface TmdbSearchResultDto {
  page: number;
  total_results: number;
  total_pages: number;
  results: TmdbSeriesOverviewDto[];

}
