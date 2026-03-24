import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { ShoppingCart, User, Menu, X, LogOut, Package, CreditCard, Shield } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { useCartSummary } from '../../hooks/useCart'
import { Logo } from '../ui/Logo'
import { ADMIN_ROLES } from '../../utils/constants'
import { HeaderSearch } from './HeaderSearch'
import { NotificationBell } from './NotificationBell'

export function Header() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)

  const { data: cartSummary } = useCartSummary()
  const isAdmin = user && ADMIN_ROLES.includes(user.role)
  const cartCount = cartSummary?.totalItems || 0

  async function handleLogout() {
    setUserMenuOpen(false)
    await logout()
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-50 border-b border-gray-200 bg-white">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4">
        <Logo />

        <div className="hidden flex-1 md:block">
          <HeaderSearch />
        </div>

        <nav className="hidden items-center gap-3 md:flex">
          {isAuthenticated ? (
            <>
              <NotificationBell />
              <Link to="/cart" className="relative p-2 text-gray-600 hover:text-gray-900">
                <ShoppingCart className="h-5 w-5" />
                {cartCount > 0 && (
                  <span className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white">
                    {cartCount > 99 ? '99+' : cartCount}
                  </span>
                )}
              </Link>

              <div className="relative">
                <button
                  onClick={() => setUserMenuOpen(!userMenuOpen)}
                  className="flex items-center gap-1 rounded-md p-2 text-gray-600 hover:text-gray-900"
                >
                  <User className="h-5 w-5" />
                  <span className="text-sm">{user.firstName || user.username}</span>
                </button>

                {userMenuOpen && (
                  <UserDropdown
                    isAdmin={isAdmin}
                    onLogout={handleLogout}
                    onClose={() => setUserMenuOpen(false)}
                  />
                )}
              </div>
            </>
          ) : (
            <>
              <Link to="/login" className="text-sm text-gray-600 hover:text-gray-900">
                Sign in
              </Link>
              <Link
                to="/register"
                className="rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700"
              >
                Register
              </Link>
            </>
          )}
        </nav>

        <button
          onClick={() => setMobileOpen(!mobileOpen)}
          className="p-2 text-gray-600 md:hidden"
        >
          {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {mobileOpen && (
        <MobileNav
          isAuthenticated={isAuthenticated}
          isAdmin={isAdmin}
          user={user}
          onLogout={handleLogout}
          onClose={() => setMobileOpen(false)}
        />
      )}
    </header>
  )
}

function UserDropdown({ isAdmin, onLogout, onClose }) {
  return (
    <div className="absolute right-0 top-full mt-1 w-48 rounded-md border border-gray-200 bg-white py-1 shadow-lg">
      <DropdownLink to="/orders" icon={Package} onClick={onClose}>My Orders</DropdownLink>
      <DropdownLink to="/payments" icon={CreditCard} onClick={onClose}>My Payments</DropdownLink>
      <DropdownLink to="/profile" icon={User} onClick={onClose}>Profile</DropdownLink>
      {isAdmin && (
        <DropdownLink to="/admin" icon={Shield} onClick={onClose}>Admin Panel</DropdownLink>
      )}
      <hr className="my-1 border-gray-100" />
      <button
        onClick={onLogout}
        className="flex w-full items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
      >
        <LogOut className="h-4 w-4" />
        Logout
      </button>
    </div>
  )
}

function DropdownLink({ to, icon: Icon, onClick, children }) {
  return (
    <Link
      to={to}
      onClick={onClick}
      className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
    >
      <Icon className="h-4 w-4" />
      {children}
    </Link>
  )
}

function MobileNav({ isAuthenticated, isAdmin, user, onLogout, onClose }) {
  return (
    <div className="border-t border-gray-200 bg-white px-4 py-3 md:hidden">
      <div className="mb-3">
        <HeaderSearch onSearch={onClose} />
      </div>
      {isAuthenticated ? (
        <div className="space-y-1">
          <p className="px-2 text-sm font-medium text-gray-900">
            {user.firstName || user.username}
          </p>
          <MobileLink to="/cart" onClick={onClose}>Cart</MobileLink>
          <MobileLink to="/orders" onClick={onClose}>My Orders</MobileLink>
          <MobileLink to="/payments" onClick={onClose}>My Payments</MobileLink>
          <MobileLink to="/profile" onClick={onClose}>Profile</MobileLink>
          {isAdmin && <MobileLink to="/admin" onClick={onClose}>Admin Panel</MobileLink>}
          <button
            onClick={onLogout}
            className="block w-full rounded-md px-2 py-2 text-left text-sm text-red-600 hover:bg-gray-50"
          >
            Logout
          </button>
        </div>
      ) : (
        <div className="flex gap-2">
          <Link to="/login" onClick={onClose} className="flex-1 rounded-md border px-4 py-2 text-center text-sm">
            Sign in
          </Link>
          <Link to="/register" onClick={onClose} className="flex-1 rounded-md bg-blue-600 px-4 py-2 text-center text-sm text-white">
            Register
          </Link>
        </div>
      )}
    </div>
  )
}

function MobileLink({ to, onClick, children }) {
  return (
    <Link to={to} onClick={onClick} className="block rounded-md px-2 py-2 text-sm text-gray-700 hover:bg-gray-50">
      {children}
    </Link>
  )
}
