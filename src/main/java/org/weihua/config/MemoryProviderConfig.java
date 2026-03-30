package org.weihua.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.weihua.repository.ChatMemoryRepository;

@Configuration
public class MemoryProviderConfig {

    @Bean
    ChatMemoryProvider chatMySqlMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(new ChatMemoryRepository()).build();
    }

    @Bean
    ChatMemoryProvider routerMySqlMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .chatMemoryStore(new ChatMemoryRepository()).build();
    }
}
