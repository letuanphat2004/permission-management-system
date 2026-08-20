import { AlertCircle, ChevronLeft, ChevronRight } from 'lucide-react'
import type { ImportErrorItem, PageResponse } from '../types/import'

interface ErrorTableProps {
  data: PageResponse<ImportErrorItem> | null
  loading: boolean
  onPage: (page: number) => void
}

export function ErrorTable({ data, loading, onPage }: ErrorTableProps) {
  return (
    <section className="card errors-card">
      <div className="card-heading error-heading">
        <div>
          <span className="eyebrow danger">Cần xử lý</span>
          <h2>Danh sách lỗi trong file</h2>
          <p>Vị trí dòng tính theo file nguồn, bao gồm dòng tiêu đề.</p>
        </div>
        <span className="error-total"><AlertCircle size={16} />{(data?.totalElements ?? 0).toLocaleString('vi-VN')} lỗi</span>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Dòng</th>
              <th>Cột</th>
              <th>ACE</th>
              <th>Giá trị gốc</th>
              <th>Lỗi</th>
              <th>Gợi ý</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6}><div className="table-state"><span className="spinner dark" /> Đang tải lỗi...</div></td></tr>
            ) : data?.content.length ? data.content.map((error) => (
              <tr key={error.id}>
                <td><span className="row-number">{error.rowNumber}</span></td>
                <td>{error.columnName}</td>
                <td>{error.aceIndex ?? '—'}</td>
                <td><code title={error.rawValue ?? ''}>{error.rawValue || '—'}</code></td>
                <td><strong className="error-message">{error.errorMessage}</strong><small className="error-code">{error.errorCode}</small></td>
                <td>{error.suggestion || '—'}</td>
              </tr>
            )) : (
              <tr><td colSpan={6}><div className="table-state">Không có lỗi để hiển thị.</div></td></tr>
            )}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 0 && (
        <div className="pagination">
          <span>Trang {data.page + 1} / {data.totalPages.toLocaleString('vi-VN')}</span>
          <div>
            <button className="page-button" disabled={data.first || loading} onClick={() => onPage(data.page - 1)}><ChevronLeft size={17} /></button>
            <button className="page-button" disabled={data.last || loading} onClick={() => onPage(data.page + 1)}><ChevronRight size={17} /></button>
          </div>
        </div>
      )}
    </section>
  )
}
