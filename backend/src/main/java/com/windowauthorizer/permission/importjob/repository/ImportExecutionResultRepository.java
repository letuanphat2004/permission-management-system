package com.windowauthorizer.permission.importjob.repository;

import com.windowauthorizer.permission.importjob.entity.ImportExecutionResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportExecutionResultRepository extends JpaRepository<ImportExecutionResult, Long> {
    Page<ImportExecutionResult> findByImportJobId(Long importJobId, Pageable pageable);
}
