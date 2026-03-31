package org.weihua.service.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    private static final int DEFAULT_MAX_RESULTS = 3;
    private static final double DEFAULT_MIN_SCORE = 0.6;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public RetrievalService(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public List<EmbeddingMatch<TextSegment>> retrieve(String question) {
        return retrieve(question, DEFAULT_MAX_RESULTS, DEFAULT_MIN_SCORE);
    }

    public List<EmbeddingMatch<TextSegment>> retrieve(String question, int maxResults, double minScore) {
        log.info("Retrieval start: questionLength={}, maxResults={}, minScore={}",
                question == null ? 0 : question.length(), maxResults, minScore);
        var queryEmbedding = embeddingModel.embed(question).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();
        if (matches.isEmpty()) {
            log.warn("Retrieval result is empty");
            return matches;
        }

        double topScore = matches.get(0).score();
        String hitDocs = matches.stream()
                .map(this::formatHit)
                .collect(Collectors.joining("; "));
        log.info("Retrieval done: matchCount={}, topScore={}", matches.size(), topScore);
        log.info("Retrieval hits: {}", hitDocs);
        return matches;
    }

    private String formatHit(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        String docName = "unknown";
        if (segment != null && segment.metadata() != null) {
            String metaName = segment.metadata().getString("document_name");
            docName = (metaName == null || metaName.isBlank()) ? "unknown" : metaName;
        }
        return docName + "(score=" + String.format("%.4f", match.score()) + ")";
    }
}
