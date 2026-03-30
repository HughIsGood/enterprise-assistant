package org.weihua.repository;

import org.weihua.model.document.KbDocument;

import java.util.List;
import java.util.Optional;

public interface KbDocumentRepository {

    int save(KbDocument kbDocument);

    int updateByDocumentId(KbDocument kbDocument);

    int deleteByDocumentId(String documentId);

    Optional<KbDocument> findByDocumentId(String documentId);

    List<KbDocument> findByDocumentType(String documentType);

    List<KbDocument> findByNameLike(String nameKeyword);

    List<KbDocument> searchByKeyword(String keyword);

    List<KbDocument> findAll();
}