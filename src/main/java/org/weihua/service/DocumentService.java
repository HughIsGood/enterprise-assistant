package org.weihua.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.weihua.model.document.DocumentType;
import org.weihua.repository.DocumentRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void uploadAndTrunkFile(MultipartFile file, DocumentType documentType) {
        validateFile(file);
        validateDocumentType(documentType);

        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }

        if (text.isBlank()) {
            throw new IllegalArgumentException("Uploaded file content is empty");
        }

        Document document = Document.from(text, Metadata.from(Map.of("document_type", documentType.getDesc())));
        documentRepository.ingest(document);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }
    }

    private void validateDocumentType(DocumentType documentType) {
        if (documentType == null) {
            throw new IllegalArgumentException("documentType must not be null");
        }
    }
}