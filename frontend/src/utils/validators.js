import { z } from 'zod'

export const loginSchema = z.object({
  username: z
    .string()
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must be at most 50 characters'),
  password: z
    .string()
    .min(1, 'Password is required'),
})

export const registerSchema = z.object({
  username: z
    .string()
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must be at most 50 characters'),
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Email must be valid'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters'),
  confirmPassword: z
    .string()
    .min(1, 'Please confirm your password'),
  firstName: z
    .string()
    .optional(),
  lastName: z
    .string()
    .optional(),
  role: z
    .enum(['CUSTOMER', 'STORE'], { message: 'Please select a role' }),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

export const changePasswordSchema = z.object({
  currentPassword: z
    .string()
    .min(1, 'Current password is required'),
  newPassword: z
    .string()
    .min(6, 'New password must be at least 6 characters'),
  confirmPassword: z
    .string()
    .min(1, 'Please confirm your new password'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

export const shippingAddressSchema = z.object({
  shippingAddressLine1: z
    .string()
    .min(1, 'Address line 1 is required'),
  shippingAddressLine2: z
    .string()
    .optional(),
  shippingCity: z
    .string()
    .min(1, 'City is required'),
  shippingState: z
    .string()
    .optional(),
  shippingPostalCode: z
    .string()
    .min(1, 'Postal code is required'),
  shippingCountry: z
    .string()
    .min(1, 'Country is required'),
  notes: z
    .string()
    .optional(),
})

export const profileSchema = z.object({
  firstName: z
    .string()
    .optional(),
  lastName: z
    .string()
    .optional(),
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Email must be valid'),
})

export const addToCartSchema = z.object({
  productId: z.number(),
  quantity: z
    .number()
    .min(1, 'Quantity must be at least 1')
    .max(99, 'Quantity cannot exceed 99'),
})

export const stockUpdateSchema = z.object({
  quantity: z
    .number()
    .min(0, 'Quantity cannot be negative'),
  lowStockThreshold: z
    .number()
    .min(0, 'Threshold cannot be negative')
    .optional(),
})
