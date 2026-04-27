package com.dsc.sharededitor.repository;

import com.dsc.sharededitor.domain.Document;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DocumentRepository {

    private final Map<String, Document> store = new ConcurrentHashMap<>();

    public void save(Document document) {
        store.put(document.getDocId(), document);
    }

    public Optional<Document> findById(String docId) {
        return Optional.ofNullable(store.get(docId));
    }

    public Collection<Document> findAll() {
        return store.values();
    }
}
