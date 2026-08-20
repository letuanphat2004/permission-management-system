package com.windowauthorizer.permission.importjob.entity;

import com.windowauthorizer.permission.importjob.domain.ExecutionAction;
import com.windowauthorizer.permission.importjob.domain.ExecutionStatus;
import com.windowauthorizer.permission.importjob.domain.PermissionLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "import_execution_results")
public class ImportExecutionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_job_id", nullable = false)
    private ImportJob importJob;

    @Column(name = "source_row_number", nullable = false)
    private long sourceRowNumber;

    @Column(name = "ace_index")
    private Integer aceIndex;

    @Column(name = "resource_path", nullable = false, length = 2048)
    private String resourcePath;

    @Column(name = "principal_name", nullable = false, length = 512)
    private String principalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_permission", length = 32)
    private PermissionLevel previousPermission;

    @Enumerated(EnumType.STRING)
    @Column(name = "desired_permission", nullable = false, length = 32)
    private PermissionLevel desiredPermission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExecutionAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExecutionStatus status;

    @Column(name = "engine_request_id", length = 255)
    private String engineRequestId;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ImportExecutionResult() {
    }

    public ImportExecutionResult(ImportJob importJob, long sourceRowNumber, Integer aceIndex,
                                 String resourcePath, String principalName, PermissionLevel previousPermission,
                                 PermissionLevel desiredPermission, ExecutionAction action, ExecutionStatus status,
                                 String engineRequestId, String errorCode, String message) {
        this.importJob = importJob;
        this.sourceRowNumber = sourceRowNumber;
        this.aceIndex = aceIndex;
        this.resourcePath = resourcePath;
        this.principalName = principalName;
        this.previousPermission = previousPermission;
        this.desiredPermission = desiredPermission;
        this.action = action;
        this.status = status;
        this.engineRequestId = engineRequestId;
        this.errorCode = errorCode;
        this.message = message;
        this.completedAt = Instant.now();
    }

    public Long getId() { return id; }
    public ImportJob getImportJob() { return importJob; }
    public long getSourceRowNumber() { return sourceRowNumber; }
    public Integer getAceIndex() { return aceIndex; }
    public String getResourcePath() { return resourcePath; }
    public String getPrincipalName() { return principalName; }
    public PermissionLevel getPreviousPermission() { return previousPermission; }
    public PermissionLevel getDesiredPermission() { return desiredPermission; }
    public ExecutionAction getAction() { return action; }
    public ExecutionStatus getStatus() { return status; }
    public String getEngineRequestId() { return engineRequestId; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
