package org.weihua.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.weihua.service.DocumentService;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        documentService.uploadAndTrunkFile(file);
        return ResponseEntity.ok(Map.of(
                "message", "Document uploaded and ingested successfully",
                "fileName", file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename()
        ));
    }
}
