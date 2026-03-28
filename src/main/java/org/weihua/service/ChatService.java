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
import org.springframework.stereotype.Service;
import org.weihua.assistant.ChatAssistant;

@Service
@Slf4j
public class ChatService {
    private final EmbeddingModel embeddingModel;

    private final EmbeddingStore embeddingStore;

    private final ChatAssistant chatAssistant;

    public ChatService(ChatAssistant chatAssistant, EmbeddingModel embeddingModel, EmbeddingStore embeddingStore) {
        this.chatAssistant = chatAssistant;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    public String chat(String userId, String message) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        // 知识检索
        // 1.把问题转成向量
        Response<Embedding> embeddingResponse = embeddingModel.embed(message);
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder().queryEmbedding(embeddingResponse.content()).maxResults(1).minScore(0.6).build();
        // 2.去向量库里找最相关的几个 chunk
        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(embeddingSearchRequest);
        String context = "";
        if (search.matches().isEmpty()) {
            log.error("匹配的向量数据库是空的");
        } else {
            EmbeddingMatch<TextSegment> textSegmentEmbeddingMatch = search.matches().get(0);
            Double score = textSegmentEmbeddingMatch.score();
            context = textSegmentEmbeddingMatch.embedded().text();
            log.info("score: {}, context:{}", score, context);
        }

        // 3.把找到的 chunk 文本拼起来 4.这个拼起来的大字符串，就是 context
        String prompt = """
                以下是与问题相关的知识片段：
                %s

                用户问题：
                %s
                请你仅依据以上知识片段回答。
                如果知识片段不足以支持答案，请明确说明无法确认。
                """.formatted(context, message);
        return chatAssistant.chat(userId, prompt);
    }
}