import { ArrowRight, Check, CircleMinus, PencilLine, Play, ShieldCheck, XCircle } from 'lucide-react'
import type { ImportJob, ImportResult } from '../types/import'

interface ExecutionPanelProps {
  job: ImportJob
  result: ImportResult | null
  executing: boolean
  onExecute: () => void
}

export function ExecutionPanel({ job, result, executing, onExecute }: ExecutionPanelProps) {
  return (
    <section className="card execution-card">
      <div className="execution-copy">
        <span className="eyebrow">Bước 3</span>
        <h2>Thực thi thay đổi</h2>
        <p>
          Backend sẽ chuẩn hóa command và gửi sang Permission Engine. Quyền chỉ có hiệu lực khi thay đổi group membership trong AD thành công.
        </p>
      </div>

      {result ? (
        <div className="result-grid">
          <ResultStat icon={<Check />} label="Thêm" value={result.addCount} tone="blue" />
          <ResultStat icon={<PencilLine />} label="Cập nhật" value={result.updateCount} tone="green" />
          <ResultStat icon={<CircleMinus />} label="Gỡ" value={result.removeCount} tone="orange" />
          <ResultStat icon={<ArrowRight />} label="Bỏ qua" value={result.skipCount} />
          <ResultStat icon={<XCircle />} label="Thất bại" value={result.failedCount} tone="red" />
        </div>
      ) : (
        <div className="execution-action">
          <span className="engine-note"><ShieldCheck size={18} /> Không ghi DB thay cho quyền Windows thực tế</span>
          <button className="primary-button execute-button" disabled={!job.executable || executing} onClick={onExecute}>
            {executing ? <><span className="spinner" /> Đang gửi Engine</> : <><Play size={17} /> Thực thi phân quyền</>}
          </button>
        </div>
      )}
    </section>
  )
}

function ResultStat({ icon, label, value, tone = '' }: { icon: React.ReactNode; label: string; value: number; tone?: string }) {
  return <div className={`result-stat ${tone}`}><span>{icon}</span><small>{label}</small><strong>{value.toLocaleString('vi-VN')}</strong></div>
}
