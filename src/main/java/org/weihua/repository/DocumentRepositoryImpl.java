package org.weihua.repository;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class DocumentRepositoryImpl implements DocumentRepository {

    private final EmbeddingStore embeddingStore;
    private final EmbeddingModel embeddingModel;

    public DocumentRepositoryImpl(EmbeddingStore embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

//    @Override
//    public void ingest(Document document) {
//        log.info("begin to ingest file text");
//
//        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
//                .documentSplitter(DocumentSplitters.recursive(100, 30))
//                .embeddingModel(embeddingModel)
//                .embeddingStore(embeddingStore)
//                .build();
//
//        ingestor.ingest(document);
//        log.info("ingest file text success");
//    }

    @Override
    public void ingest(Document document) {
        log.info("begin to ingest file text");

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);
        List<TextSegment> segments = splitter.split(document);

        List<TextSegment> finalSegments = new ArrayList<>();

        String documentName = document.metadata() != null
                ? document.metadata().getString("document_name")
                : "unknown_name";

        String documentType = document.metadata() != null
                ? document.metadata().getString("document_type")
                : "UNKNOWN";

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);

            Metadata metadata = Metadata.from(Map.of(
                    "document_name", documentName != null ? documentName : "null",
                    "document_type", documentType != null ? documentType : "null",
                    "chunk_index", String.valueOf(i)
            ));

            finalSegments.add(TextSegment.from(segment.text(), metadata));
        }

        List<Embedding> embeddings = embeddingModel.embedAll(finalSegments).content();
        embeddingStore.addAll(embeddings, finalSegments);

        log.info("ingest file text success, segmentCount={}", finalSegments.size());
    }
}