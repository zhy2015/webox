import type { Dish } from './types'

export interface CartLine {
  key: string
  dish: Dish
  selectedOptionIds: string[]
  selectedOptionLabels: string[]
  unitPriceCents: number
  quantity: number
}

export type CartAction =
  | { type: 'add'; line: Omit<CartLine, 'quantity'> }
  | { type: 'increment'; key: string }
  | { type: 'decrement'; key: string }
  | { type: 'remove'; key: string }
  | { type: 'clear' }

export const CART_STORAGE_KEY = 'webox:cart:v1'

export function cartKey(dishId: number, optionIds: string[]) {
  return `${dishId}:${optionIds.toSorted().join(',')}`
}

export function cartQuantity(lines: CartLine[]) {
  return lines.reduce((sum, line) => sum + line.quantity, 0)
}

export function cartTotalCents(lines: CartLine[]) {
  return lines.reduce((sum, line) => sum + line.unitPriceCents * line.quantity, 0)
}

export function approveAllergenRisk(
  dishAllergens: string[],
  flaggedAllergens: string[],
  confirmRisk: (message: string) => boolean,
) {
  const conflicts = dishAllergens.filter(allergen => flaggedAllergens.includes(allergen))
  return conflicts.length === 0 || confirmRisk(`This dish contains an allergen you flagged: ${conflicts.join(', ')}. Add anyway?`)
}

export function cartReducer(lines: CartLine[], action: CartAction): CartLine[] {
  if (action.type === 'clear') return []
  if (action.type === 'remove') return lines.filter(line => line.key !== action.key)
  if (action.type === 'decrement') {
    return lines.flatMap(line => line.key !== action.key ? [line] : line.quantity <= 1 ? [] : [{ ...line, quantity: line.quantity - 1 }])
  }
  if (action.type === 'increment') {
    if (cartQuantity(lines) >= 5) return lines
    return lines.map(line => line.key === action.key ? { ...line, quantity: line.quantity + 1 } : line)
  }
  if (cartQuantity(lines) >= 5) return lines
  const existing = lines.find(line => line.key === action.line.key)
  if (existing) return lines.map(line => line.key === existing.key ? { ...line, quantity: line.quantity + 1 } : line)
  return [...lines, { ...action.line, quantity: 1 }]
}

export function loadCart(): CartLine[] {
  try {
    const raw = localStorage.getItem(CART_STORAGE_KEY)
    return raw ? JSON.parse(raw) as CartLine[] : []
  } catch { return [] }
}
