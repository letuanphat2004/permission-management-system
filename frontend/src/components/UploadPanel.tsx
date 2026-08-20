import { useRef, useState, type DragEvent } from 'react'
import { FileSpreadsheet, FileUp, RefreshCw, UploadCloud, X } from 'lucide-react'
import type { ImportJob } from '../types/import'

interface UploadPanelProps {
  file: File | null
  job: ImportJob | null
  uploading: boolean
  onFile: (file: File) => void
  onUpload: () => void
  onClear: () => void
}

const allowedExtensions = ['csv', 'xls', 'xlsx']

export function UploadPanel({
  file,
  job,
  uploading,
  onFile,
  onUpload,
  onClear,
}: UploadPanelProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)
  const [localError, setLocalError] = useState('')

  const acceptFile = (candidate?: File) => {
    if (!candidate) return
    const extension = candidate.name.split('.').pop()?.toLowerCase() ?? ''
    if (!allowedExtensions.includes(extension)) {
      setLocalError('Chỉ hỗ trợ file CSV, XLS hoặc XLSX.')
      return
    }
    setLocalError('')
    onFile(candidate)
  }

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    setDragging(false)
    acceptFile(event.dataTransfer.files[0])
  }

  const chooseAgain = () => {
    if (inputRef.current) inputRef.current.value = ''
    inputRef.current?.click()
  }

  const displayName = file?.name ?? job?.fileName
  const displaySize = file?.size ?? job?.fileSizeBytes

  return (
    <section className="card upload-card">
      <div className="card-heading">
        <div>
          <span className="eyebrow">Bước 1</span>
          <h2>Chọn file phân quyền</h2>
          <p>Hỗ trợ báo cáo CSV hoặc Excel có cấu trúc ACL của hệ thống.</p>
        </div>
        {displayName && (
          <button className="text-button" onClick={chooseAgain} disabled={uploading}>
            <RefreshCw size={15} /> Chọn file khác
          </button>
        )}
      </div>

      <input
        ref={inputRef}
        type="file"
        accept=".csv,.xls,.xlsx"
        hidden
        onChange={(event) => acceptFile(event.target.files?.[0])}
      />

      {!displayName ? (
        <div
          className={`drop-zone${dragging ? ' dragging' : ''}`}
          onClick={chooseAgain}
          onDragEnter={(event) => { event.preventDefault(); setDragging(true) }}
          onDragOver={(event) => event.preventDefault()}
          onDragLeave={() => setDragging(false)}
          onDrop={handleDrop}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => event.key === 'Enter' && chooseAgain()}
        >
          <span className="upload-icon"><UploadCloud size={28} /></span>
          <strong>Kéo thả file vào đây hoặc nhấn để chọn</strong>
          <span>CSV, XLS, XLSX · tối đa 200 MB</span>
        </div>
      ) : (
        <div className="selected-file">
          <span className="file-icon"><FileSpreadsheet size={26} /></span>
          <div className="file-details">
            <strong title={displayName}>{displayName}</strong>
            <span>{formatFileSize(displaySize ?? 0)}</span>
          </div>
          {file && !uploading && (
            <button className="icon-button subtle" onClick={onClear} aria-label="Bỏ file">
              <X size={18} />
            </button>
          )}
          {file && (
            <button className="primary-button" onClick={onUpload} disabled={uploading}>
              {uploading ? <><span className="spinner" /> Đang tải lên</> : <><FileUp size={17} /> Tải lên và kiểm tra</>}
            </button>
          )}
        </div>
      )}
      {localError && <p className="inline-error">{localError}</p>}
    </section>
  )
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
