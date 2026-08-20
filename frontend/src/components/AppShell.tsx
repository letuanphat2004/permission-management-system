import type { ReactNode } from 'react'
import {
  Bell,
  ChevronDown,
  ClipboardList,
  FileUp,
  Gauge,
  Settings,
  ShieldCheck,
  Users,
  UsersRound,
} from 'lucide-react'

const menu = [
  { label: 'Tổng quan', icon: Gauge },
  { label: 'Người dùng', icon: Users },
  { label: 'Nhóm nghiệp vụ', icon: UsersRound },
  { label: 'Quản lý quyền', icon: ShieldCheck },
  { label: 'Import quyền', icon: FileUp, active: true },
  { label: 'Nhật ký hệ thống', icon: ClipboardList },
  { label: 'Cài đặt', icon: Settings },
]

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark"><ShieldCheck size={19} /></span>
          <span>Permission Management</span>
        </div>
        <div className="account-actions">
          <button className="icon-button" aria-label="Thông báo">
            <Bell size={19} />
            <span className="notification-dot">3</span>
          </button>
          <span className="avatar">AD</span>
          <span className="account-name">admin</span>
          <ChevronDown size={15} />
        </div>
      </header>

      <aside className="sidebar">
        <nav>
          {menu.map(({ label, icon: Icon, active }) => (
            <button className={`nav-item${active ? ' active' : ''}`} key={label}>
              <Icon size={18} />
              <span>{label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-profile">
          <span className="avatar large">AD</span>
          <span><strong>admin</strong><small>Quản trị viên</small></span>
          <ChevronDown size={15} />
        </div>
      </aside>

      <main className="main-content">{children}</main>
    </div>
  )
}
