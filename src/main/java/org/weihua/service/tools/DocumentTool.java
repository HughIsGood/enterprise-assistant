package org.weihua.service.tools;

import org.springframework.stereotype.Service;
import org.weihua.model.document.KbDocument;
import org.weihua.repository.KbDocumentRepository;

import java.util.stream.Collectors;

@Service
public class DocumentTool {

    private final KbDocumentRepository kbDocumentRepository;

    public DocumentTool(KbDocumentRepository kbDocumentRepository) {
        this.kbDocumentRepository = kbDocumentRepository;
    }

    public String listDocumentsByType(String documentType) {
        return kbDocumentRepository.findByDocumentType(documentType).stream()
                .map(d -> d.getName() + " (" + d.getDocumentType() + ")")
                .collect(Collectors.joining("\n"));
    }

    public String searchDocumentNames(String keyword) {
        return kbDocumentRepository.searchByKeyword(keyword).stream()
                .map(KbDocument::getName)
                .collect(Collectors.joining("\n"));
    }
}