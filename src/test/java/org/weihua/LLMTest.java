package org.weihua;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LLMTest {
    @Autowired
    private AnthropicChatModel anthropicChatModel;

    @Test
    public void helloWorld() {
        String answer = anthropicChatModel.chat("你好");
        System.out.println(answer); // Hello World
    }
}
