package org.weihua.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class KnowledgeRetriever {

    private static final int MAX_RESULTS = 3;
    private static final double MIN_SCORE = 0.6;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public KnowledgeRetriever(EmbeddingModel embeddingModel,
                              EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public RetrievedContext retrieve(String question) {
        Response<Embedding> embeddingResponse = embeddingModel.embed(question);

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingResponse.content())
                .maxResults(MAX_RESULTS)
                .minScore(MIN_SCORE)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        if (matches.isEmpty()) {
            log.warn("No matching vector result found");
            return new RetrievedContext("", 0.0, 0);
        }

        String context = matches.stream()
                .map(this::formatMatch)
                .collect(Collectors.joining("\n\n"));

        double topScore = matches.get(0).score();
        log.info("Retrieved {} matches, top score: {}", matches.size(), topScore);
        return new RetrievedContext(context, topScore, matches.size());
    }

    private String formatMatch(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();

        String documentName = segment.metadata() != null
                ? segment.metadata().getString("document_name")
                : "unknown_document";

        String chunkIndex = segment.metadata() != null
                ? segment.metadata().getString("chunk_index")
                : "?";

        return "[source: " + documentName + " | chunk: " + chunkIndex + "]\n" + segment.text();
    }

    public record RetrievedContext(String context, double topScore, int matchCount) {
    }
}