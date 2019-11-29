package ch.andreskonrad.torenta.tmdb.dto;

import java.util.List;
import java.util.Objects;

public class SearchResult {

    private int page;
    private int total_results;
    private int total_pages;
    private List<SeriesOverview> results;

    public SearchResult() {
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

    public List<SeriesOverview> getResults() {
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
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
