import type {
  ApiError,
  ImportErrorItem,
  ImportJob,
  ImportResult,
  PageResponse,
} from '../types/import'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, options)
  if (!response.ok) {
    let error: ApiError = {}
    try {
      error = (await response.json()) as ApiError
    } catch {
      // The fallback below is used when a proxy returns a non-JSON response.
    }
    throw new Error(error.message || `Yêu cầu thất bại (${response.status}).`)
  }
  return response.json() as Promise<T>
}

export async function uploadImport(file: File): Promise<ImportJob> {
  const form = new FormData()
  form.append('file', file)
  return request<ImportJob>('/api/imports', {
    method: 'POST',
    headers: { 'X-Actor': 'admin' },
    body: form,
  })
}

export function getImportJob(id: number): Promise<ImportJob> {
  return request<ImportJob>(`/api/imports/${id}`)
}

export function getImportErrors(
  id: number,
  page: number,
  size: number,
): Promise<PageResponse<ImportErrorItem>> {
  return request<PageResponse<ImportErrorItem>>(
    `/api/imports/${id}/errors?page=${page}&size=${size}`,
  )
}

export function executeImport(id: number): Promise<ImportJob> {
  return request<ImportJob>(`/api/imports/${id}/execute`, { method: 'POST' })
}

export function getImportResult(
  id: number,
  page = 0,
  size = 100,
): Promise<ImportResult> {
  return request<ImportResult>(`/api/imports/${id}/result?page=${page}&size=${size}`)
}
