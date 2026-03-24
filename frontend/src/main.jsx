import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router'
import { Toaster } from 'react-hot-toast'
import { QueryProvider } from './providers/QueryProvider'
import { AuthProvider } from './context/AuthContext'
import { SSEProvider } from './components/SSEProvider'
import { router } from './routes/router'
import './index.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <QueryProvider>
      <AuthProvider>
        <SSEProvider>
          <RouterProvider router={router} />
          <Toaster position="top-right" toastOptions={{ duration: 3000 }} />
        </SSEProvider>
      </AuthProvider>
    </QueryProvider>
  </StrictMode>,
)
