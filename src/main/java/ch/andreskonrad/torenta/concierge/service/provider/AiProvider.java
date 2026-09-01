package ch.andreskonrad.torenta.concierge.service.provider;

import ch.andreskonrad.torenta.concierge.dto.CandidateRanking;
import ch.andreskonrad.torenta.concierge.dto.MediaCandidate;
import ch.andreskonrad.torenta.concierge.dto.SearchIntent;

import java.util.List;

public interface AiProvider {

    AiProviderType type();

    SearchIntent extractIntent(String prompt);

    List<CandidateRanking> rank(String prompt, SearchIntent intent, List<MediaCandidate> candidates);
}
