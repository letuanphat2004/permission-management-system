package com.windowauthorizer.permission.importjob.repository;

import com.windowauthorizer.permission.importjob.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {
}
