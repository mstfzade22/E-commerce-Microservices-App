import { Outlet } from 'react-router'
import { Header } from './Header'
import { Footer } from './Footer'
import { ErrorBoundary } from '../ErrorBoundary'

export function MainLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      <Header />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6">
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>
      <Footer />
    </div>
  )
}
