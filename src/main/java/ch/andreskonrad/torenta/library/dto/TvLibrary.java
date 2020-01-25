package ch.andreskonrad.torenta.library.dto;

import java.util.ArrayList;
import java.util.List;

public class TvLibrary {

    private final List<Series> series;

    public TvLibrary() {
        series = new ArrayList<>();
    }

    public List<Series> getSeries() {
        return series;
    }

    public void addSeries(Series series) {
        this.series.add(series);
    }
}
