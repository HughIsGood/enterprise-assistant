package org.weihua.repository;

import dev.langchain4j.data.document.Document;
import org.weihua.model.document.DocumentType;

public interface DocumentRepository {

    void ingest(Document document);
}