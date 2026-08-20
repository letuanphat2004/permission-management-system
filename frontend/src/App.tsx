import { useCallback, useEffect, useState } from 'react'
import { AlertCircle, CheckCircle2, X } from 'lucide-react'
import { executeImport, getImportErrors, getImportJob, getImportResult, uploadImport } from './api/importApi'
import { AppShell } from './components/AppShell'
import { ErrorTable } from './components/ErrorTable'
import { ExecutionPanel } from './components/ExecutionPanel'
import { JobSummary } from './components/JobSummary'
import { UploadPanel } from './components/UploadPanel'
import type { ImportErrorItem, ImportJob, ImportResult, PageResponse } from './types/import'

const ERROR_PAGE_SIZE = 100

export default function App() {
  const [file, setFile] = useState<File | null>(null)
  const [job, setJob] = useState<ImportJob | null>(null)
  const [errors, setErrors] = useState<PageResponse<ImportErrorItem> | null>(null)
  const [errorPage, setErrorPage] = useState(0)
  const [result, setResult] = useState<ImportResult | null>(null)
  const [uploading, setUploading] = useState(false)
  const [loadingErrors, setLoadingErrors] = useState(false)
  const [executing, setExecuting] = useState(false)
  const [message, setMessage] = useState<{ type: 'error' | 'success'; text: string } | null>(null)

  useEffect(() => {
    const queryId = new URLSearchParams(window.location.search).get('importId')
    const savedId = window.localStorage.getItem('activeImportId')
    const importId = Number(queryId || savedId)
    if (!Number.isInteger(importId) || importId <= 0) return

    getImportJob(importId)
      .then((restored) => {
        setJob(restored)
        window.localStorage.setItem('activeImportId', String(restored.id))
      })
      .catch(() => {
        window.localStorage.removeItem('activeImportId')
        window.history.replaceState({}, '', window.location.pathname)
      })
  }, [])

  const refreshJob = useCallback(async (id: number) => {
    const latest = await getImportJob(id)
    setJob(latest)
    return latest
  }, [])

  useEffect(() => {
    if (!job || !['PARSING', 'EXECUTING'].includes(job.status)) return
    const timer = window.setInterval(() => {
      refreshJob(job.id).catch((error: Error) => setMessage({ type: 'error', text: error.message }))
    }, 1500)
    return () => window.clearInterval(timer)
  }, [job?.id, job?.status, refreshJob])

  useEffect(() => {
    if (!job || job.status !== 'INVALID') {
      setErrors(null)
      return
    }
    let active = true
    setLoadingErrors(true)
    getImportErrors(job.id, errorPage, ERROR_PAGE_SIZE)
      .then((page) => active && setErrors(page))
      .catch((error: Error) => active && setMessage({ type: 'error', text: error.message }))
      .finally(() => active && setLoadingErrors(false))
    return () => { active = false }
  }, [job?.id, job?.status, errorPage])

  useEffect(() => {
    if (!job || !job.executedAt || !['COMPLETED', 'FAILED'].includes(job.status)) return
    getImportResult(job.id)
      .then(setResult)
      .catch((error: Error) => setMessage({ type: 'error', text: error.message }))
  }, [job?.id, job?.status, job?.executedAt])

  const selectFile = (selected: File) => {
    setFile(selected)
    setJob(null)
    setErrors(null)
    setResult(null)
    setErrorPage(0)
    setMessage(null)
    window.localStorage.removeItem('activeImportId')
    window.history.replaceState({}, '', window.location.pathname)
  }

  const handleUpload = async () => {
    if (!file) return
    setUploading(true)
    setMessage(null)
    try {
      const created = await uploadImport(file)
      setJob(created)
      window.localStorage.setItem('activeImportId', String(created.id))
      window.history.replaceState({}, '', `${window.location.pathname}?importId=${created.id}`)
      setFile(null)
      setErrorPage(0)
      setMessage({ type: 'success', text: 'Đã tải file lên. Hệ thống đang kiểm tra dữ liệu.' })
    } catch (error) {
      setMessage({ type: 'error', text: error instanceof Error ? error.message : 'Không thể upload file.' })
    } finally {
      setUploading(false)
    }
  }

  const handleExecute = async () => {
    if (!job?.executable) return
    setExecuting(true)
    setMessage(null)
    try {
      const updated = await executeImport(job.id)
      setJob(updated)
      const executionResult = await getImportResult(job.id)
      setResult(executionResult)
      setMessage({ type: 'success', text: 'Permission Engine đã xử lý xong change set.' })
    } catch (error) {
      setMessage({ type: 'error', text: error instanceof Error ? error.message : 'Không thể thực thi import.' })
      await refreshJob(job.id).catch(() => undefined)
    } finally {
      setExecuting(false)
    }
  }

  return (
    <AppShell>
      <div className="page-header">
        <div>
          <span className="breadcrumb">Quản lý quyền / Import</span>
          <h1>Import phân quyền</h1>
          <p>Tải file ACL, kiểm tra dữ liệu và gửi các thay đổi hợp lệ sang Permission Engine.</p>
        </div>
      </div>

      {message && (
        <div className={`toast-message ${message.type}`}>
          {message.type === 'error' ? <AlertCircle size={18} /> : <CheckCircle2 size={18} />}
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} aria-label="Đóng"><X size={16} /></button>
        </div>
      )}

      <div className="content-stack">
        <UploadPanel
          file={file}
          job={job}
          uploading={uploading}
          onFile={selectFile}
          onUpload={handleUpload}
          onClear={() => setFile(null)}
        />
        {job && <JobSummary job={job} />}
        {job?.status === 'INVALID' && <ErrorTable data={errors} loading={loadingErrors} onPage={setErrorPage} />}
        {job && ['READY', 'EXECUTING', 'COMPLETED'].includes(job.status) && (
          <ExecutionPanel job={job} result={result} executing={executing} onExecute={handleExecute} />
        )}
      </div>
    </AppShell>
  )
}
