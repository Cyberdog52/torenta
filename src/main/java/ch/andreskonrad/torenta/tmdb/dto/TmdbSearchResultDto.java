package ch.andreskonrad.torenta.tmdb.dto;

import java.util.List;
import java.util.Objects;

public class TmdbSearchResultDto {

    private int page;
    private int total_results;
    private int total_pages;
    private List<TmdbSeriesOverviewDto> results;

    public TmdbSearchResultDto() {
    }

    public int getPage() {
        return page;
    }

    public int getTotal_results() {
        return total_results;
    }

    public int getTotal_pages() {
        return total_pages;
    }

    public List<TmdbSeriesOverviewDto> getResults() {
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TmdbSearchResultDto that = (TmdbSearchResultDto) o;
        return page == that.page &&
                total_results == that.total_results &&
                total_pages == that.total_pages &&
                Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, total_results, total_pages, results);
    }
}
