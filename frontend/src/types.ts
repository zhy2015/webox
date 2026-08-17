export type Role = 'EMPLOYEE' | 'ADMIN'
export type MealPeriod = 'Lunch' | 'Dinner'
export type OrderStatus = 'Pending' | 'Confirmed' | 'Completed' | 'Cancelled'

export interface User {
  id: number
  email: string
  role: Role
}

export interface DishOption {
  id: string
  label: string
  extraPrice: number
}

export interface DishOptionGroup {
  id: string
  label: string
  required: boolean
  multiple: boolean
  options: DishOption[]
}

export interface Dish {
  id: number
  name: string
  description: string
  price: string
  category: string
  protein: string
  allergens: string[]
  spiceLevel: string
  optionGroups: DishOptionGroup[]
  imageUrl: string
  published: boolean
  remainingStock?: number
}

export interface Preference {
  allergens: string[]
  cuisines: string[]
  spiceLevel: string
  tasteIntensity: string
  budgetMin: number | null
  budgetMax: number | null
}

export interface OrderLine {
  dishId: number
  dishName: string
  unitPrice: string
  quantity: number
  selectedOptions: string[]
  lineTotal: string
}

export interface Order {
  id: number
  orderNumber: string
  deliveryDate: string
  mealPeriod: MealPeriod
  status: OrderStatus
  deliveryAddress: string
  totalAmount: string
  createdAt: string
  items: OrderLine[]
}

export interface ApiErrorBody {
  code: string
  message: string
  fieldErrors?: Array<{ field: string; message: string }>
}
