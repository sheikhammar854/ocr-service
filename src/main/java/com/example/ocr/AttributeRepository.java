package com.example.ocr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface AttributeRepository extends JpaRepository<attributes, Long> {
    Iterable<attributes> findByDoc_Id(long doc_id);
}