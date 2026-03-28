package org.weihua.repository;

import dev.langchain4j.data.document.Document;

public interface DocumentRepository {

    void ingest(Document document);
}
