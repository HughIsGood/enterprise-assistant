package org.weihua.repository;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import org.springframework.stereotype.Repository;
import org.weihua.model.document.KbDocument;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class KbDocumentRepositoryImpl implements KbDocumentRepository {

    private static final String TABLE_NAME = "kb_document";

    @Override
    public int save(KbDocument kbDocument) {
        validateForCreate(kbDocument);

        Entity insertEntity = Entity.create(TABLE_NAME)
                .set("document_id", kbDocument.getDocumentId())
                .set("name", kbDocument.getName())
                .set("document_type", kbDocument.getDocumentType())
                .set("created_by", kbDocument.getCreatedBy())
                .set("updated_by", kbDocument.getUpdatedBy());

        try {
            return Db.use().insert(insertEntity);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save kb_document", e);
        }
    }

    @Override
    public int updateByDocumentId(KbDocument kbDocument) {
        if (kbDocument == null || isBlank(kbDocument.getDocumentId())) {
            throw new IllegalArgumentException("documentId must not be blank when updating");
        }

        Entity updateEntity = Entity.create();
        if (!isBlank(kbDocument.getName())) {
            updateEntity.set("name", kbDocument.getName());
        }
        if (!isBlank(kbDocument.getDocumentType())) {
            updateEntity.set("document_type", kbDocument.getDocumentType());
        }
        if (!isBlank(kbDocument.getUpdatedBy())) {
            updateEntity.set("updated_by", kbDocument.getUpdatedBy());
        }

        if (updateEntity.isEmpty()) {
            return 0;
        }

        try {
            return Db.use().update(updateEntity,
                    Entity.create(TABLE_NAME).set("document_id", kbDocument.getDocumentId()));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update kb_document", e);
        }
    }

    @Override
    public int deleteByDocumentId(String documentId) {
        if (isBlank(documentId)) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        try {
            return Db.use().del(Entity.create(TABLE_NAME).set("document_id", documentId));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete kb_document", e);
        }
    }

    @Override
    public Optional<KbDocument> findByDocumentId(String documentId) {
        if (isBlank(documentId)) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        try {
            Entity entity = Db.use().get(Entity.create(TABLE_NAME).set("document_id", documentId));
            return Optional.ofNullable(entity).map(this::toKbDocument);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query kb_document by documentId", e);
        }
    }

    @Override
    public List<KbDocument> findByDocumentType(String documentType) {
        if (isBlank(documentType)) {
            throw new IllegalArgumentException("documentType must not be blank");
        }
        try {
            List<Entity> entities = Db.use().find(Entity.create(TABLE_NAME).set("document_type", documentType));
            return entities.stream().map(this::toKbDocument).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query kb_document by documentType", e);
        }
    }

    @Override
    public List<KbDocument> findByNameLike(String nameKeyword) {
        if (isBlank(nameKeyword)) {
            return findAll();
        }

        String keyword = "%" + escapeLike(nameKeyword.trim()) + "%";
        String sql = "select id, document_id, name, document_type, created_by, updated_by, create_time, update_time "
                + "from " + TABLE_NAME + " where name like ? order by update_time desc";

        try {
            List<Entity> entities = Db.use().query(sql, keyword);
            return entities.stream().map(this::toKbDocument).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fuzzy query kb_document by name", e);
        }
    }

    @Override
    public List<KbDocument> searchByKeyword(String keyword) {
        return findByNameLike(keyword);
    }

    @Override
    public List<KbDocument> findAll() {
        try {
            List<Entity> entities = Db.use().find(Entity.create(TABLE_NAME));
            return entities.stream().map(this::toKbDocument).collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query all kb_document records", e);
        }
    }

    private void validateForCreate(KbDocument kbDocument) {
        if (kbDocument == null) {
            throw new IllegalArgumentException("kbDocument must not be null");
        }
        if (isBlank(kbDocument.getDocumentId())) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (isBlank(kbDocument.getName())) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (isBlank(kbDocument.getDocumentType())) {
            throw new IllegalArgumentException("documentType must not be blank");
        }
    }

    private KbDocument toKbDocument(Entity entity) {
        KbDocument kbDocument = new KbDocument();
        kbDocument.setId(entity.getLong("id"));
        kbDocument.setDocumentId(entity.getStr("document_id"));
        kbDocument.setName(entity.getStr("name"));
        kbDocument.setDocumentType(entity.getStr("document_type"));
        kbDocument.setCreatedBy(entity.getStr("created_by"));
        kbDocument.setUpdatedBy(entity.getStr("updated_by"));
        kbDocument.setCreateTime(toLocalDateTime(entity.get("create_time")));
        kbDocument.setUpdateTime(toLocalDateTime(entity.get("update_time")));
        return kbDocument;
    }

    private LocalDateTime toLocalDateTime(Object dateValue) {
        if (dateValue == null) {
            return null;
        }
        if (dateValue instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (dateValue instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (dateValue instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime();
        }
        return null;
    }

    private String escapeLike(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}