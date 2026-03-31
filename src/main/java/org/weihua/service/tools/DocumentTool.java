package org.weihua.service.tools;

import dev.langchain4j.agent.tool.Tool;
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

    @Tool("按文档类型列出文档")
    public String listDocumentsByType(String documentType) {
        return kbDocumentRepository.findByDocumentType(documentType).stream()
                .map(d -> d.getName() + " (" + d.getDocumentType() + ")")
                .collect(Collectors.joining("\n"));
    }

    @Tool("按关键字搜索文档名称")
    public String searchDocumentNames(String keyword) {
        return kbDocumentRepository.searchByKeyword(keyword).stream()
                .map(KbDocument::getName)
                .collect(Collectors.joining("\n"));
    }

    @Tool("根据 documentId 查询文档详情")
    public String getDocumentDetail(String documentId) {
        return kbDocumentRepository.findByDocumentId(documentId)
                .map(doc -> "documentId=" + doc.getDocumentId() + "\n"
                        + "name=" + doc.getName() + "\n"
                        + "documentType=" + doc.getDocumentType())
                .orElse("Document not found: " + documentId);
    }
}
