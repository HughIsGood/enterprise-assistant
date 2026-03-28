package org.weihua.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfig {

    private final EmbeddingModel embeddingModel;
    private final String pineConeApiKey;

    public EmbeddingStoreConfig(EmbeddingModel embeddingModel,
                                @Value("${pine.cone.api.key}") String pineConeApiKey) {
        this.embeddingModel = embeddingModel;
        this.pineConeApiKey = pineConeApiKey;
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        PineconeEmbeddingStore embeddingStore = PineconeEmbeddingStore.builder()
                .apiKey(pineConeApiKey)
                .index("enterprise-index")
                .nameSpace("enterprise-namespace")
                .createIndex(PineconeServerlessIndexConfig.builder()
                        .cloud("AWS")
                        .region("us-east-1")
                        .dimension(embeddingModel.dimension())
                        .build())
                .build();
        return embeddingStore;
    }
}
