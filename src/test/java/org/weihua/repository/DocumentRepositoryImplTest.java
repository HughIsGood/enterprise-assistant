package org.weihua.repository;

import dev.langchain4j.data.document.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.weihua.model.document.DocumentType;

@SpringBootTest
class DocumentRepositoryImplTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void ingest() {
        Document document = Document.from("666");
        documentRepository.ingest(document);
    }
}