import { createBrowserRouter } from 'react-router'
import { MainLayout } from '../components/layout/MainLayout'
import { AuthLayout } from '../components/layout/AuthLayout'
import { AdminLayout } from '../components/layout/AdminLayout'
import { ProtectedRoute } from './guards/ProtectedRoute'
import { GuestRoute } from './guards/GuestRoute'
import { RoleGuard } from './guards/RoleGuard'
import { ADMIN_ROLES } from '../utils/constants'
import { HomePage } from '../pages/HomePage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { LoginPage } from '../pages/auth/LoginPage'
import { RegisterPage } from '../pages/auth/RegisterPage'
import { ProductListPage } from '../pages/ProductListPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { CategoryPage } from '../pages/CategoryPage'
import { SearchPage } from '../pages/SearchPage'
import { CartPage } from '../pages/CartPage'
import { CheckoutPage } from '../pages/CheckoutPage'
import { OrdersPage } from '../pages/OrdersPage'
import { OrderDetailPage } from '../pages/OrderDetailPage'
import { PaymentsPage } from '../pages/PaymentsPage'
import { ProfilePage } from '../pages/ProfilePage'
import { PaymentSuccessPage } from '../pages/payment/PaymentSuccessPage'
import { PaymentCancelPage } from '../pages/payment/PaymentCancelPage'
import { PaymentDeclinePage } from '../pages/payment/PaymentDeclinePage'
import { DashboardPage } from '../pages/admin/DashboardPage'
import { ProductsManagePage } from '../pages/admin/ProductsManagePage'
import { ProductEditPage } from '../pages/admin/ProductEditPage'
import { CategoriesManagePage } from '../pages/admin/CategoriesManagePage'
import { OrdersManagePage } from '../pages/admin/OrdersManagePage'
import { OrderManageDetailPage } from '../pages/admin/OrderManageDetailPage'
import { InventoryPage } from '../pages/admin/InventoryPage'
import { PaymentsManagePage } from '../pages/admin/PaymentsManagePage'
import { NotificationsPage } from '../pages/admin/NotificationsPage'
import { UsersManagePage } from '../pages/admin/UsersManagePage'

export const router = createBrowserRouter([
  {
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'products', element: <ProductListPage /> },
      { path: 'products/:slug', element: <ProductDetailPage /> },
      { path: 'category/:slug', element: <CategoryPage /> },
      { path: 'search', element: <SearchPage /> },

      { path: 'payment/success', element: <PaymentSuccessPage /> },
      { path: 'payment/cancel', element: <PaymentCancelPage /> },
      { path: 'payment/decline', element: <PaymentDeclinePage /> },

      {
        element: <ProtectedRoute />,
        children: [
          { path: 'cart', element: <CartPage /> },
          { path: 'checkout', element: <CheckoutPage /> },
          { path: 'orders', element: <OrdersPage /> },
          { path: 'orders/:orderNumber', element: <OrderDetailPage /> },
          { path: 'payments', element: <PaymentsPage /> },
          { path: 'profile', element: <ProfilePage /> },
        ],
      },
    ],
  },

  {
    element: <RoleGuard allowedRoles={ADMIN_ROLES} />,
    children: [
      {
        element: <AdminLayout />,
        children: [
          { path: 'admin', element: <DashboardPage /> },
          { path: 'admin/products', element: <ProductsManagePage /> },
          { path: 'admin/products/new', element: <ProductEditPage /> },
          { path: 'admin/products/:id/edit', element: <ProductEditPage /> },
          { path: 'admin/categories', element: <CategoriesManagePage /> },
          { path: 'admin/orders', element: <OrdersManagePage /> },
          { path: 'admin/orders/:orderNumber', element: <OrderManageDetailPage /> },
          { path: 'admin/inventory', element: <InventoryPage /> },
          { path: 'admin/payments', element: <PaymentsManagePage /> },
          { path: 'admin/notifications', element: <NotificationsPage /> },
          { path: 'admin/users', element: <UsersManagePage /> },
        ],
      },
    ],
  },

  {
    element: <GuestRoute />,
    children: [
      {
        element: <AuthLayout />,
        children: [
          { path: 'login', element: <LoginPage /> },
          { path: 'register', element: <RegisterPage /> },
        ],
      },
    ],
  },

  { path: '*', element: <NotFoundPage /> },
])
