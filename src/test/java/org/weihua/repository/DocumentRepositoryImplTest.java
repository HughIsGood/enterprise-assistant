package org.weihua.repository;

import dev.langchain4j.data.document.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentRepositoryImplTest {
    @Autowired
    private DocumentRepository documentRepository;

    /**
     * 验证向量数据库上传是否成功
     */
    @Test
    void ingest() {
        Document document = Document.from("666");
        documentRepository.ingest(document);
    }
}