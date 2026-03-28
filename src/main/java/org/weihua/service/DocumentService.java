package org.weihua.service;

import dev.langchain4j.data.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.weihua.repository.DocumentRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void uploadAndTrunkFile(MultipartFile file) {
        validateFile(file);

        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        }

        if (text.isBlank()) {
            throw new IllegalArgumentException("Uploaded file content is empty");
        }

        Document document = Document.from(text);
        documentRepository.ingest(document);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }
    }
}
