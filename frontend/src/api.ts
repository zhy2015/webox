import type { ApiErrorBody, Dish, MealPeriod, Order, Preference, User } from './types'

export class ApiError extends Error {
  constructor(public status: number, public body: ApiErrorBody) {
    super(body.message)
  }
}

let csrfToken: string | null = null

async function ensureCsrf() {
  if (csrfToken) return csrfToken
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'include' })
  const data = (await response.json()) as { token: string }
  csrfToken = data.token
  return csrfToken
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body) headers.set('Content-Type', 'application/json')
  if (init.method && init.method !== 'GET') headers.set('X-XSRF-TOKEN', await ensureCsrf())
  const response = await fetch(path, { ...init, headers, credentials: 'include' })
  if (!response.ok) {
    let body: ApiErrorBody = { code: 'REQUEST_FAILED', message: 'The request could not be completed.' }
    try { body = await response.json() as ApiErrorBody } catch { /* keep safe fallback */ }
    throw new ApiError(response.status, body)
  }
  if (response.status === 204 || response.headers.get('content-length') === '0') return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  me: () => request<User>('/api/v1/auth/me'),
  login: (email: string, password: string) => request<User>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  register: (email: string, password: string) => request<User>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  logout: () => request<void>('/api/v1/auth/logout', { method: 'POST' }),
  menu: (date: string, search: string, categories: string[]) => {
    const query = new URLSearchParams()
    if (search) query.set('search', search)
    if (categories.length) query.set('categories', categories.join(','))
    return request<Dish[]>(`/api/v1/menus/${date}?${query}`)
  },
  preferences: () => request<Preference>('/api/v1/me/preferences'),
  savePreferences: (preference: Preference) => request<Preference>('/api/v1/me/preferences', { method: 'PUT', body: JSON.stringify(preference) }),
  orders: () => request<Order[]>('/api/v1/orders'),
  activeOrder: (deliveryDate: string, mealPeriod: MealPeriod) =>
    request<Order | undefined>(`/api/v1/orders/active?${new URLSearchParams({ deliveryDate, mealPeriod })}`),
  placeOrder: (input: { deliveryDate: string; mealPeriod: MealPeriod; deliveryAddress: string; items: Array<{ dishId: number; quantity: number; selectedOptionIds: string[] }> }, key: string) =>
    request<Order>('/api/v1/orders', { method: 'POST', headers: { 'Idempotency-Key': key }, body: JSON.stringify(input) }),
  cancelOrder: (id: number) => request<Order>(`/api/v1/orders/${id}/cancel`, { method: 'POST' }),
  adminDishes: () => request<Dish[]>('/api/v1/console/dishes'),
  createDish: (dish: Omit<Dish, 'id' | 'published' | 'remainingStock'>) => request<Dish>('/api/v1/console/dishes', { method: 'POST', body: JSON.stringify(dish) }),
  updateDish: (id: number, dish: Omit<Dish, 'id' | 'published' | 'remainingStock'>) => request<Dish>(`/api/v1/console/dishes/${id}`, { method: 'PUT', body: JSON.stringify(dish) }),
  setDishStatus: (id: number, published: boolean) => request<Dish>(`/api/v1/console/dishes/${id}/status`, { method: 'PATCH', body: JSON.stringify({ published }) }),
  adminMenu: (date: string) => request<Array<{ dishId: number; dishName: string; initialStock: number; remainingStock: number }>>(`/api/v1/console/menus/${date}`),
  saveAdminMenu: (date: string, items: Array<{ dishId: number; stock: number }>) => request(`/api/v1/console/menus/${date}`, { method: 'PUT', body: JSON.stringify(items) }),
}
