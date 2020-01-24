package ch.andreskonrad.torenta.library.dto;

import java.util.ArrayList;
import java.util.List;

public class TvLibrary {

    private final List<SeriesEntry> seriesEntries;

    public TvLibrary() {
        seriesEntries = new ArrayList<>();
    }

    public List<SeriesEntry> getSeriesEntries() {
        return seriesEntries;
    }

    public void addSeries(SeriesEntry seriesEntry) {
        seriesEntries.add(seriesEntry);
    }
}
