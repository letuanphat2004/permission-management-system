export type ImportStatus =
  | 'PARSING'
  | 'INVALID'
  | 'READY'
  | 'EXECUTING'
  | 'COMPLETED'
  | 'FAILED'

export interface ImportJob {
  id: number
  fileName: string
  fileSizeBytes: number
  status: ImportStatus
  totalSourceRows: number
  totalPermissionEntries: number
  validPermissionEntries: number
  skippedPermissionEntries: number
  errorCount: number
  executable: boolean
  failureMessage: string | null
  createdBy: string
  createdAt: string | null
  validatedAt: string | null
  executedAt: string | null
}

export interface ImportErrorItem {
  id: number
  rowNumber: number
  columnName: string
  aceIndex: number | null
  rawValue: string | null
  errorCode: string
  errorMessage: string
  suggestion: string | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface ExecutionItem {
  id: number
  rowNumber: number
  aceIndex: number | null
  resourcePath: string
  principalName: string
  previousPermission: 'NONE' | 'READ' | 'MODIFY' | 'FULL_CONTROL' | 'SPECIAL_PERMISSION' | null
  desiredPermission: 'NONE' | 'READ' | 'MODIFY' | 'FULL_CONTROL' | 'SPECIAL_PERMISSION'
  action: 'ADD' | 'UPDATE' | 'REMOVE' | 'SKIP'
  status: 'SUCCESS' | 'FAILED'
  engineRequestId: string | null
  errorCode: string | null
  message: string | null
}

export interface ImportResult {
  importId: number
  status: ImportStatus
  addCount: number
  updateCount: number
  removeCount: number
  skipCount: number
  failedCount: number
  items: PageResponse<ExecutionItem>
}

export interface ApiError {
  status?: number
  code?: string
  message?: string
}
