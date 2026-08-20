import { AlertTriangle, CheckCircle2, Clock3, FileStack, ListChecks, LoaderCircle, ShieldAlert, SkipForward } from 'lucide-react'
import type { ImportJob, ImportStatus } from '../types/import'

const statusMeta: Record<ImportStatus, { label: string; className: string }> = {
  PARSING: { label: 'Đang kiểm tra', className: 'processing' },
  INVALID: { label: 'Có lỗi', className: 'invalid' },
  READY: { label: 'Sẵn sàng', className: 'ready' },
  EXECUTING: { label: 'Đang thực thi', className: 'processing' },
  COMPLETED: { label: 'Hoàn thành', className: 'ready' },
  FAILED: { label: 'Thất bại', className: 'invalid' },
}

export function JobSummary({ job }: { job: ImportJob }) {
  const status = statusMeta[job.status]
  const processing = job.status === 'PARSING' || job.status === 'EXECUTING'

  return (
    <section className="card summary-card">
      <div className="summary-header">
        <div>
          <span className="eyebrow">Bước 2</span>
          <h2>Kết quả kiểm tra dữ liệu</h2>
        </div>
        <span className={`status-badge ${status.className}`}>
          {processing ? <LoaderCircle className="spin" size={15} /> : job.status === 'READY' || job.status === 'COMPLETED' ? <CheckCircle2 size={15} /> : <ShieldAlert size={15} />}
          {status.label}
        </span>
      </div>

      {processing && <div className="progress-track"><span /></div>}

      <div className="stats-grid">
        <Stat icon={<FileStack />} label="Dòng nguồn" value={job.totalSourceRows} />
        <Stat icon={<ListChecks />} label="ACE đã đọc" value={job.totalPermissionEntries} />
        <Stat icon={<CheckCircle2 />} label="ACE hợp lệ" value={job.validPermissionEntries} tone="success" />
        <Stat icon={<SkipForward />} label="ACE bỏ qua" value={job.skippedPermissionEntries} />
        <Stat icon={<AlertTriangle />} label="Lỗi" value={job.errorCount} tone={job.errorCount > 0 ? 'danger' : undefined} />
      </div>

      <div className={`job-message ${status.className}`}>
        {job.status === 'PARSING' && <><Clock3 size={18} /><span>Backend đang đọc file. Bạn có thể giữ nguyên màn hình này; dữ liệu sẽ tự cập nhật.</span></>}
        {job.status === 'INVALID' && <><AlertTriangle size={18} /><span>File chưa hợp lệ. Hãy xem danh sách lỗi bên dưới, sửa file gốc rồi import lại.</span></>}
        {job.status === 'READY' && <><CheckCircle2 size={18} /><span>Không phát hiện lỗi. File đã sẵn sàng để gửi sang Permission Engine.</span></>}
        {job.status === 'EXECUTING' && <><LoaderCircle className="spin" size={18} /><span>Đang gửi các thay đổi hợp lệ sang Permission Engine.</span></>}
        {job.status === 'COMPLETED' && <><CheckCircle2 size={18} /><span>Quá trình thực thi đã hoàn thành.</span></>}
        {job.status === 'FAILED' && <><ShieldAlert size={18} /><span>{job.failureMessage || 'Không thể hoàn thành import.'}</span></>}
      </div>
    </section>
  )
}

function Stat({ icon, label, value, tone }: { icon: React.ReactNode; label: string; value: number; tone?: string }) {
  return (
    <div className={`stat-item${tone ? ` ${tone}` : ''}`}>
      <span className="stat-icon">{icon}</span>
      <span><small>{label}</small><strong>{value.toLocaleString('vi-VN')}</strong></span>
    </div>
  )
}
