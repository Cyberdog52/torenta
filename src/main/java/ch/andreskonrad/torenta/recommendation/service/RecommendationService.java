package ch.andreskonrad.torenta.recommendation.service;

import ch.andreskonrad.torenta.CustomCacheConfig;
import ch.andreskonrad.torenta.directory.service.DirectoryService;
import ch.andreskonrad.torenta.library.dto.AirStatus;
import ch.andreskonrad.torenta.library.dto.DownloadStatus;
import ch.andreskonrad.torenta.library.dto.Episode;
import ch.andreskonrad.torenta.library.dto.Season;
import ch.andreskonrad.torenta.library.dto.Series;
import ch.andreskonrad.torenta.library.service.LibraryService;
import ch.andreskonrad.torenta.recommendation.dto.RecommendedEpisodeDto;
import ch.andreskonrad.torenta.recommendation.dto.SeriesRecommendationDto;
import ch.andreskonrad.torenta.tmdb.dto.TmdbEpisodeDto;
import ch.andreskonrad.torenta.tmdb.service.TmdbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = { CustomCacheConfig.RECOMMENDATION_CACHE_NAME })
public class RecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationService.class);

    public static final int DEFAULT_WEEKS_BACK = 2;
    private static final int RECOMMENDED_EPISODE_COUNT = 3;

    private final DirectoryService directoryService;
    private final LibraryService libraryService;
    private final TmdbService tmdbService;

    @Autowired
    public RecommendationService(DirectoryService directoryService, LibraryService libraryService, TmdbService tmdbService) {
        this.directoryService = directoryService;
        this.libraryService = libraryService;
        this.tmdbService = tmdbService;
    }

    @Cacheable
    public List<SeriesRecommendationDto> getRecommendations(int weeksBack) {
        List<String> seriesNames = directoryService.getSeriesNamesModifiedWithin(Duration.ofDays(weeksBack * 7L));

        List<SeriesRecommendationDto> recommendations = new ArrayList<>();
        for (String seriesName : seriesNames) {
            SeriesRecommendationDto recommendation = getRecommendationForSeries(seriesName);
            if (recommendation != null) {
                recommendations.add(recommendation);
            }
        }

        recommendations.sort(Comparator.comparing(SeriesRecommendationDto::getSeriesName, String.CASE_INSENSITIVE_ORDER));
        return recommendations;
    }

    private SeriesRecommendationDto getRecommendationForSeries(String seriesName) {
        Series series;
        try {
            series = libraryService.getSeriesInLibrary(seriesName);
        } catch (Exception e) {
            LOGGER.warn("Could not resolve series in library for recommendations: {}", seriesName);
            return null;
        }

        List<RecommendedEpisodeDto> missingEpisodes = getMissingAiredEpisodes(series);
        if (missingEpisodes.isEmpty()) {
            return null;
        }

        return new SeriesRecommendationDto(
                seriesName,
                series.getSeriesDetail().getId(),
                series.getSeriesDetail().getPoster_path(),
                missingEpisodes);
    }

    private List<RecommendedEpisodeDto> getMissingAiredEpisodes(Series series) {
        List<RecommendedEpisodeDto> missingEpisodes = series.getSeasonList().stream()
                .sorted(Comparator.comparingInt(Season::getSeasonNumber))
                .flatMap(season -> season.getEpisodeList().stream())
                .filter(episode -> episode.getAirStatus() == AirStatus.AIRED
                        && episode.getDownloadStatus() == DownloadStatus.NOT_DOWNLOADED)
                .limit(RECOMMENDED_EPISODE_COUNT)
                .map(this::toRecommendedEpisodeDto)
                .collect(Collectors.toCollection(ArrayList::new));

        if (missingEpisodes.size() < RECOMMENDED_EPISODE_COUNT) {
            missingEpisodes.addAll(getMissingEpisodesFromNextUnstartedSeason(series, RECOMMENDED_EPISODE_COUNT - missingEpisodes.size()));
        }

        return missingEpisodes;
    }

    /**
     * A season that has aired but was never downloaded at all has no local folder, so
     * {@link LibraryService} never picks it up. Look at the season right after the last
     * known local season and recommend its aired episodes too, so a series isn't dropped
     * from recommendations just because the next season was never started.
     */
    private List<RecommendedEpisodeDto> getMissingEpisodesFromNextUnstartedSeason(Series series, int remainingCount) {
        int lastLocalSeasonNumber = series.getSeasonDirectoriesBySeasonNumber().keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1);
        int nextSeasonNumber = lastLocalSeasonNumber + 1;

        boolean nextSeasonExistsOnTmdb = series.getSeriesDetail().getSeasons() != null
                && series.getSeriesDetail().getSeasons().stream()
                        .anyMatch(season -> season.getSeason_number() == nextSeasonNumber);
        if (nextSeasonNumber <= 0 || !nextSeasonExistsOnTmdb) {
            return List.of();
        }

        TmdbEpisodeDto[] episodes = tmdbService.getEpisodes(series.getSeriesDetail().getId(), nextSeasonNumber);
        if (episodes == null) {
            return List.of();
        }

        List<RecommendedEpisodeDto> result = new ArrayList<>();
        for (TmdbEpisodeDto episode : episodes) {
            if (result.size() >= remainingCount) {
                break;
            }
            if (hasAired(episode)) {
                result.add(toRecommendedEpisodeDto(episode));
            }
        }
        return result;
    }

    private boolean hasAired(TmdbEpisodeDto episode) {
        if (episode == null || episode.getAir_date() == null) {
            return false;
        }
        return !LocalDate.parse(episode.getAir_date()).isAfter(LocalDate.now());
    }

    private RecommendedEpisodeDto toRecommendedEpisodeDto(Episode episode) {
        return toRecommendedEpisodeDto(episode.getTmdbEpisodeDto());
    }

    private RecommendedEpisodeDto toRecommendedEpisodeDto(TmdbEpisodeDto tmdbEpisodeDto) {
        return new RecommendedEpisodeDto(
                tmdbEpisodeDto.getSeason_number(),
                tmdbEpisodeDto.getEpisode_number(),
                episodeString(tmdbEpisodeDto.getSeason_number(), tmdbEpisodeDto.getEpisode_number()),
                tmdbEpisodeDto.getName(),
                tmdbEpisodeDto.getAir_date(),
                tmdbEpisodeDto.getStill_path());
    }

    private String episodeString(int seasonNumber, int episodeNumber) {
        StringBuilder s = new StringBuilder().append("S");
        if (seasonNumber < 10) s.append(0);
        s.append(seasonNumber);
        s.append("E");
        if (episodeNumber < 10) s.append(0);
        s.append(episodeNumber);
        return s.toString();
    }
}
