import { describe, expect, it } from 'vitest'
import { approveAllergenRisk, cartKey, cartQuantity, cartReducer, cartTotalCents, type CartLine } from './cart-model'
import type { Dish } from './types'

const dish: Dish = {
  id: 9,
  name: 'Classic Beef Burger',
  description: 'A burger',
  price: '38.00',
  category: 'Western',
  protein: 'Beef',
  allergens: ['Gluten'],
  spiceLevel: 'None',
  optionGroups: [],
  imageUrl: '/images/dish-19.jpg',
  published: true,
}

function line(options: string[] = []): Omit<CartLine, 'quantity'> {
  return { key: cartKey(dish.id, options), dish, selectedOptionIds: options, selectedOptionLabels: options, unitPriceCents: 3800 }
}

describe('cart model', () => {
  it('merges the same configuration and separates different configurations', () => {
    const first = cartReducer([], { type: 'add', line: line(['whole-wheat']) })
    const merged = cartReducer(first, { type: 'add', line: line(['whole-wheat']) })
    const separate = cartReducer(merged, { type: 'add', line: line(['plain']) })
    expect(separate).toHaveLength(2)
    expect(separate[0].quantity).toBe(2)
  })

  it('never exceeds five total items', () => {
    let cart: CartLine[] = [{ ...line(), quantity: 5 }]
    cart = cartReducer(cart, { type: 'increment', key: cart[0].key })
    cart = cartReducer(cart, { type: 'add', line: line(['extra']) })
    expect(cartQuantity(cart)).toBe(5)
  })

  it('calculates totals in integer cents', () => {
    const cart: CartLine[] = [{ ...line(), unitPriceCents: 3880, quantity: 3 }]
    expect(cartTotalCents(cart)).toBe(11640)
  })

  it('requires explicit approval for a flagged allergen', () => {
    let prompt = ''
    const rejected = approveAllergenRisk(['Peanuts', 'Soy'], ['Peanuts'], message => {
      prompt = message
      return false
    })
    expect(rejected).toBe(false)
    expect(prompt).toContain('Peanuts')

    const accepted = approveAllergenRisk(['Peanuts'], ['Peanuts'], () => true)
    expect(accepted).toBe(true)
  })
})
