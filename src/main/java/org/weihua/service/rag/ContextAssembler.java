package org.weihua.service.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextAssembler {

    public String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches == null || matches.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int maxChars = 3000;

        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            String documentName = segment.metadata() != null
                    ? segment.metadata().getString("document_name")
                    : "未知文档";
            String chunkIndex = segment.metadata() != null
                    ? segment.metadata().getString("chunk_index")
                    : "?";

            String piece = """
                    [来源: %s | chunk: %s]
                    %s

                    """.formatted(documentName, chunkIndex, segment.text());

            if (sb.length() + piece.length() > maxChars) {
                break;
            }

            sb.append(piece);
        }

        return sb.toString();
    }
}
