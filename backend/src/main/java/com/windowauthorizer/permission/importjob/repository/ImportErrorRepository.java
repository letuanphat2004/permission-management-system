package com.windowauthorizer.permission.importjob.repository;

import com.windowauthorizer.permission.importjob.entity.ImportError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportErrorRepository extends JpaRepository<ImportError, Long> {
    Page<ImportError> findByImportJobId(Long importJobId, Pageable pageable);

    void deleteByImportJobId(Long importJobId);
}
