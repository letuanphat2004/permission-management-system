package com.windowauthorizer.permission.importjob.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "import_errors")
public class ImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJob importJob;

    @Column(name = "source_row_number", nullable = false)
    private long sourceRowNumber;

    @Column(name = "column_name", nullable = false, length = 255)
    private String columnName;

    @Column(name = "ace_index")
    private Integer aceIndex;

    @Column(name = "raw_value", columnDefinition = "MEDIUMTEXT")
    private String rawValue;

    @Column(name = "error_code", nullable = false, length = 100)
    private String errorCode;

    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected ImportError() {
    }

    public ImportError(ImportJob importJob, long sourceRowNumber, String columnName, Integer aceIndex,
                       String rawValue, String errorCode, String errorMessage, String suggestion) {
        this.importJob = importJob;
        this.sourceRowNumber = sourceRowNumber;
        this.columnName = columnName;
        this.aceIndex = aceIndex;
        this.rawValue = rawValue;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.suggestion = suggestion;
    }

    public Long getId() { return id; }
    public ImportJob getImportJob() { return importJob; }
    public long getSourceRowNumber() { return sourceRowNumber; }
    public String getColumnName() { return columnName; }
    public Integer getAceIndex() { return aceIndex; }
    public String getRawValue() { return rawValue; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public String getSuggestion() { return suggestion; }
    public Instant getCreatedAt() { return createdAt; }
}
