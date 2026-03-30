package org.weihua.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.weihua.model.document.DocumentType;
import org.weihua.model.document.KbDocument;
import org.weihua.repository.DocumentRepository;
import org.weihua.repository.KbDocumentRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KbDocumentRepository kbDocumentRepository;

    public DocumentService(DocumentRepository documentRepository,
                           KbDocumentRepository kbDocumentRepository) {
        this.documentRepository = documentRepository;
        this.kbDocumentRepository = kbDocumentRepository;
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

        String documentName = resolveDocumentName(file);
        KbDocument kbDocument = buildKbDocument(documentName, documentType);
        kbDocumentRepository.save(kbDocument);

        Document document = Document.from(text, Metadata.from(Map.of(
                "document_type", documentType.name(),
                "document_name", documentName
        )));
        documentRepository.ingest(document);
    }

    private KbDocument buildKbDocument(String documentName, DocumentType documentType) {
        KbDocument kbDocument = new KbDocument();
        kbDocument.setDocumentId(generateDocumentId());
        kbDocument.setName(documentName);
        kbDocument.setDocumentType(documentType.name());
        kbDocument.setCreatedBy("system");
        kbDocument.setUpdatedBy("system");
        return kbDocument;
    }

    private String resolveDocumentName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        return (originalFilename == null || originalFilename.isBlank()) ? "unknown" : originalFilename;
    }

    private String generateDocumentId() {
        return UUID.randomUUID().toString().replace("-", "");
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