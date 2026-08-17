import { FormEvent, useEffect, useMemo, useReducer, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import ShoppingBag from 'lucide-react/dist/esm/icons/shopping-bag'
import Utensils from 'lucide-react/dist/esm/icons/utensils'
import ClipboardList from 'lucide-react/dist/esm/icons/clipboard-list'
import Settings from 'lucide-react/dist/esm/icons/settings'
import LogOut from 'lucide-react/dist/esm/icons/log-out'
import Search from 'lucide-react/dist/esm/icons/search'
import Plus from 'lucide-react/dist/esm/icons/plus'
import Minus from 'lucide-react/dist/esm/icons/minus'
import X from 'lucide-react/dist/esm/icons/x'
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right'
import ShieldCheck from 'lucide-react/dist/esm/icons/shield-check'
import SlidersHorizontal from 'lucide-react/dist/esm/icons/sliders-horizontal'
import CalendarDays from 'lucide-react/dist/esm/icons/calendar-days'
import Check from 'lucide-react/dist/esm/icons/check'
import { api, ApiError } from './api'
import { approveAllergenRisk, CART_STORAGE_KEY, cartKey, cartQuantity, cartReducer, cartTotalCents, loadCart, type CartLine } from './cart-model'
import type { Dish, DishOptionGroup, MealPeriod, Preference, User } from './types'

const CATEGORIES = ['Chinese', 'Western', 'Japanese', 'Light Meal', 'Korean', 'Southeast Asian']
const ALLERGENS = ['Peanuts', 'Dairy', 'Egg', 'Gluten', 'Soy', 'Fish', 'Shellfish']
const EMPTY_PREFERENCE: Preference = { allergens: [], cuisines: [], spiceLevel: '', tasteIntensity: '', budgetMin: null, budgetMax: null }

function localDate() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function moneyFromCents(cents: number) { return `¥${(cents / 100).toFixed(2)}` }
function money(value: string | number) { return `¥${Number(value).toFixed(2)}` }
function errorMessage(error: unknown) { return error instanceof ApiError ? error.body.message : 'Something went wrong. Please try again.' }

export default function App() {
  const queryClient = useQueryClient()
  const auth = useQuery({ queryKey: ['me'], queryFn: api.me, retry: false })
  const [cart, dispatch] = useReducer(cartReducer, undefined, loadCart)

  useEffect(() => {
    try { localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(cart)) } catch { /* cart remains usable in memory */ }
  }, [cart])

  if (auth.isLoading) return <LoadingScreen />
  if (!auth.data) return <AuthScreen onAuthenticated={user => queryClient.setQueryData(['me'], user)} />

  return <AppShell user={auth.data} cart={cart} dispatch={dispatch} onLogout={async () => {
    await api.logout()
    queryClient.setQueryData(['me'], null)
    queryClient.removeQueries({ predicate: query => query.queryKey[0] !== 'me' })
  }} />
}

function LoadingScreen() {
  return <main className="loading-screen"><div className="brand-mark">W</div><p>Preparing today’s menu…</p></main>
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: (user: User) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('employee@webox.local')
  const [password, setPassword] = useState('Lunch123')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try { onAuthenticated(await (mode === 'login' ? api.login(email, password) : api.register(email, password))) }
    catch (caught) { setError(errorMessage(caught)) }
    finally { setBusy(false) }
  }

  return <main className="auth-layout">
    <section className="auth-story">
      <div className="brand-lockup"><span className="brand-mark">W</span><span>WeBox</span></div>
      <div className="auth-copy">
        <p className="eyebrow">WORKDAY DINING</p>
        <h1>Lunch, sorted.</h1>
        <p>Fresh choices from your workplace kitchen, ordered in a few focused minutes.</p>
      </div>
      <div className="auth-food-strip" aria-hidden="true">
        <img src="/images/dish-12.jpg" alt="" />
        <img src="/images/dish-10.jpg" alt="" />
        <img src="/images/dish-18.jpg" alt="" />
      </div>
    </section>
    <section className="auth-form-wrap">
      <form className="auth-form" onSubmit={submit}>
        <p className="eyebrow">WELCOME TO WEBOX</p>
        <h2>{mode === 'login' ? 'Sign in for today’s menu' : 'Create your account'}</h2>
        <div className="segmented" aria-label="Account action">
          <button type="button" className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Sign in</button>
          <button type="button" className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Register</button>
        </div>
        <label>Email<input type="email" maxLength={200} value={email} onChange={event => setEmail(event.target.value)} required /></label>
        <label>Password<input type="password" minLength={8} maxLength={72} value={password} onChange={event => setPassword(event.target.value)} required /></label>
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        <button className="primary wide" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}</button>
        <div className="demo-note"><ShieldCheck size={16} /><span>Admin demo: admin@webox.local / Admin123</span></div>
      </form>
    </section>
  </main>
}

function AppShell({ user, cart, dispatch, onLogout }: {
  user: User
  cart: CartLine[]
  dispatch: React.Dispatch<Parameters<typeof cartReducer>[1]>
  onLogout: () => Promise<void>
}) {
  const isAdmin = user.role === 'ADMIN'
  return <div className="app-shell">
    <header className="topbar">
      <NavLink to={isAdmin ? '/console' : '/menu'} className="brand-lockup"><span className="brand-mark">W</span><span>WeBox</span></NavLink>
      <nav aria-label="Primary navigation">
        {isAdmin ? <NavLink to="/console"><SlidersHorizontal size={18} />Console</NavLink> : <>
          <NavLink to="/menu"><Utensils size={18} />Menu</NavLink>
          <NavLink to="/orders"><ClipboardList size={18} />Orders</NavLink>
          <NavLink to="/preferences"><Settings size={18} />Preferences</NavLink>
        </>}
      </nav>
      <div className="account-actions"><span>{user.email}</span><button className="icon-button" onClick={onLogout} title="Sign out" aria-label="Sign out"><LogOut size={18} /></button></div>
    </header>
    <Routes>
      {isAdmin ? <>
        <Route path="/console" element={<ConsolePage />} />
        <Route path="*" element={<Navigate to="/console" replace />} />
      </> : <>
        <Route path="/menu" element={<MenuPage cart={cart} dispatch={dispatch} />} />
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/preferences" element={<PreferencesPage />} />
        <Route path="*" element={<Navigate to="/menu" replace />} />
      </>}
    </Routes>
  </div>
}

function MenuPage({ cart, dispatch }: { cart: CartLine[]; dispatch: React.Dispatch<Parameters<typeof cartReducer>[1]> }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [categories, setCategories] = useState<string[]>([])
  const [selectedDish, setSelectedDish] = useState<Dish | null>(null)
  const [forYou, setForYou] = useState(false)
  const date = localDate()
  const preference = useQuery({ queryKey: ['preferences'], queryFn: api.preferences })
  const menu = useQuery({ queryKey: ['menu', date, search, categories], queryFn: () => api.menu(date, search, categories) })

  const dishes = useMemo(() => {
    const source = menu.data ?? []
    if (!forYou || !preference.data) return source
    const preferred = new Set(preference.data.cuisines)
    return source.toSorted((a, b) => Number(preferred.has(b.category)) - Number(preferred.has(a.category)))
  }, [menu.data, forYou, preference.data])

  function toggleCategory(category: string) {
    setCategories(current => current.includes(category) ? current.filter(item => item !== category) : [...current, category])
  }

  function addLine(dish: Dish, selectedIds: string[], selectedLabels: string[], unitPriceCents: number) {
    if (!approveAllergenRisk(dish.allergens, preference.data?.allergens ?? [], message => window.confirm(message))) return
    dispatch({ type: 'add', line: { key: cartKey(dish.id, selectedIds), dish, selectedOptionIds: selectedIds, selectedOptionLabels: selectedLabels, unitPriceCents } })
    setSelectedDish(null)
  }

  return <main className="menu-page">
    <section className="menu-content">
      <header className="page-intro">
        <div><p className="eyebrow">TODAY · {new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' }).toUpperCase()}</p><h1>What sounds good?</h1></div>
        <label className="recommend-toggle"><input type="checkbox" checked={forYou} onChange={event => setForYou(event.target.checked)} /><span>For You</span></label>
      </header>
      <div className="menu-tools">
        <label className="search-field"><Search size={18} /><input value={search} maxLength={50} onChange={event => setSearch(event.target.value)} placeholder="Search dishes or ingredients" /></label>
        <div className="filter-row" aria-label="Cuisine filters">{CATEGORIES.map(category => <button key={category} className={categories.includes(category) ? 'filter active' : 'filter'} onClick={() => toggleCategory(category)}>{category}</button>)}</div>
      </div>
      {menu.isLoading ? <p className="state-message">Loading today’s kitchen…</p> : menu.isError ? <p className="state-message error">{errorMessage(menu.error)}</p> : dishes.length === 0 ? <p className="state-message">No dishes match these filters.</p> :
        <div className="dish-grid">{dishes.map((dish, index) => <DishCard key={dish.id} dish={dish} index={index} onOpen={() => dish.optionGroups.length ? setSelectedDish(dish) : addLine(dish, [], [], Math.round(Number(dish.price) * 100))} />)}</div>}
    </section>
    <CartPanel cart={cart} dispatch={dispatch} preference={preference.data ?? EMPTY_PREFERENCE} onOrdered={() => {
      queryClient.invalidateQueries({ queryKey: ['menu'] })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      navigate('/orders')
    }} />
    {selectedDish ? <DishDialog dish={selectedDish} onClose={() => setSelectedDish(null)} onAdd={addLine} /> : null}
  </main>
}

function DishCard({ dish, index, onOpen }: { dish: Dish; index: number; onOpen: () => void }) {
  const soldOut = dish.remainingStock === 0
  return <article className="dish-card" style={{ animationDelay: `${Math.min(index, 8) * 45}ms` }}>
    <button className="dish-image-button" onClick={onOpen} disabled={soldOut} aria-label={`View ${dish.name}`}>
      <img src={dish.imageUrl} alt={dish.name} />
      <span className="category-tag">{dish.category}</span>
      {dish.remainingStock !== undefined && dish.remainingStock <= 3 ? <span className={soldOut ? 'stock-tag sold-out' : 'stock-tag'}>{soldOut ? 'Sold out' : `${dish.remainingStock} left`}</span> : null}
    </button>
    <div className="dish-copy"><div><h2>{dish.name}</h2><p>{dish.description}</p></div><div className="dish-footer"><strong>{money(dish.price)}</strong><button className="add-button" onClick={onOpen} disabled={soldOut} title={`Add ${dish.name}`} aria-label={`Add ${dish.name}`}><Plus size={20} /></button></div></div>
  </article>
}

function DishDialog({ dish, onClose, onAdd }: { dish: Dish; onClose: () => void; onAdd: (dish: Dish, ids: string[], labels: string[], cents: number) => void }) {
  const [selected, setSelected] = useState<Record<string, string[]>>({})
  const selectedIds = Object.values(selected).flat()
  const allOptions = dish.optionGroups.flatMap(group => group.options)
  const selectedOptions = allOptions.filter(option => selectedIds.includes(option.id))
  const cents = Math.round(Number(dish.price) * 100) + selectedOptions.reduce((sum, option) => sum + Math.round(Number(option.extraPrice) * 100), 0)
  const missingRequired = dish.optionGroups.some(group => group.required && !(selected[group.id]?.length))

  function choose(group: DishOptionGroup, optionId: string, checked: boolean) {
    setSelected(current => ({ ...current, [group.id]: group.multiple
      ? checked ? [...(current[group.id] ?? []), optionId] : (current[group.id] ?? []).filter(id => id !== optionId)
      : [optionId] }))
  }

  return <div className="modal-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}>
    <section className="dish-dialog" role="dialog" aria-modal="true" aria-labelledby="dish-dialog-title">
      <button className="icon-button dialog-close" onClick={onClose} aria-label="Close"><X size={20} /></button>
      <img className="dialog-image" src={dish.imageUrl} alt={dish.name} />
      <div className="dialog-content"><p className="eyebrow">{dish.category} · {dish.spiceLevel === 'None' ? 'NOT SPICY' : `${dish.spiceLevel.toUpperCase()} SPICE`}</p><h2 id="dish-dialog-title">{dish.name}</h2><p>{dish.description}</p>
        {dish.optionGroups.map(group => <fieldset key={group.id}><legend>{group.label}<span>{group.required ? 'Required' : 'Optional'}</span></legend>{group.options.map(option => <label className="option-row" key={option.id}><input type={group.multiple ? 'checkbox' : 'radio'} name={group.id} checked={(selected[group.id] ?? []).includes(option.id)} onChange={event => choose(group, option.id, event.target.checked)} /><span>{option.label}</span><strong>{Number(option.extraPrice) ? `+${money(option.extraPrice)}` : 'Included'}</strong></label>)}</fieldset>)}
        <button className="primary wide add-configured" disabled={missingRequired} onClick={() => onAdd(dish, selectedIds, selectedOptions.map(option => option.label), cents)}><ShoppingBag size={18} />Add to cart · {moneyFromCents(cents)}</button>
      </div>
    </section>
  </div>
}

function CartPanel({ cart, dispatch, preference, onOrdered }: { cart: CartLine[]; dispatch: React.Dispatch<Parameters<typeof cartReducer>[1]>; preference: Preference; onOrdered: () => void }) {
  const navigate = useNavigate()
  const [date, setDate] = useState(localDate())
  const [mealPeriod, setMealPeriod] = useState<MealPeriod>('Lunch')
  const [address, setAddress] = useState('Building A, Floor 3')
  const totalCents = cartTotalCents(cart)
  const quantity = cartQuantity(cart)
  const overBudget = preference.budgetMax !== null && totalCents > preference.budgetMax * 100
  const activeOrder = useQuery({
    queryKey: ['active-order', date, mealPeriod],
    queryFn: () => api.activeOrder(date, mealPeriod),
    enabled: cart.length > 0,
  })
  const place = useMutation({ mutationFn: () => api.placeOrder({ deliveryDate: date, mealPeriod, deliveryAddress: address, items: cart.map(line => ({ dishId: line.dish.id, quantity: line.quantity, selectedOptionIds: line.selectedOptionIds })) }, crypto.randomUUID()), onSuccess: () => { dispatch({ type: 'clear' }); onOrdered() } })

  return <aside className="cart-panel" aria-label="Your order">
    <header><div><p className="eyebrow">YOUR ORDER</p><h2>{quantity ? `${quantity} of 5 items` : 'Cart is empty'}</h2></div><ShoppingBag size={22} /></header>
    <div className="cart-lines">{cart.length === 0 ? <div className="empty-cart"><span>01</span><p>Choose a dish to start your order.</p></div> : cart.map(line => <div className="cart-line" key={line.key}><img src={line.dish.imageUrl} alt="" /><div><strong>{line.dish.name}</strong><small>{line.selectedOptionLabels.join(' · ') || line.dish.category}</small><span>{moneyFromCents(line.unitPriceCents * line.quantity)}</span></div><div className="quantity-control"><button onClick={() => dispatch({ type: 'decrement', key: line.key })} aria-label={`Decrease ${line.dish.name}`}><Minus size={14} /></button><span>{line.quantity}</span><button disabled={quantity >= 5} onClick={() => dispatch({ type: 'increment', key: line.key })} aria-label={`Increase ${line.dish.name}`}><Plus size={14} /></button></div></div>)}</div>
    {cart.length ? <div className="checkout-fields">
      <div className="two-fields"><label>Date<input type="date" min={localDate()} value={date} onChange={event => setDate(event.target.value)} /></label><label>Meal<select value={mealPeriod} onChange={event => setMealPeriod(event.target.value as MealPeriod)}><option>Lunch</option><option>Dinner</option></select></label></div>
      <label>Delivery address<input maxLength={200} value={address} onChange={event => setAddress(event.target.value)} /></label>
      {overBudget ? <p className="budget-warning">This order is above your {money(preference.budgetMax!)} meal budget.</p> : null}
      {place.error ? <p className="form-error">{errorMessage(place.error)}</p> : null}
      <div className="cart-total"><span>Total</span><strong>{moneyFromCents(totalCents)}</strong></div>
      {activeOrder.data ? <button className="primary wide" onClick={() => navigate('/orders')}>View existing order<ChevronRight size={18} /></button>
        : <button className="primary wide" disabled={place.isPending || activeOrder.isLoading || !address.trim()} onClick={() => place.mutate()}>{place.isPending ? 'Placing order…' : 'Place order'}<ChevronRight size={18} /></button>}
      <p className="cutoff-note">Lunch closes at 10:00 · Dinner at 15:00</p>
    </div> : null}
  </aside>
}

function OrdersPage() {
  const queryClient = useQueryClient()
  const orders = useQuery({ queryKey: ['orders'], queryFn: api.orders })
  const cancel = useMutation({ mutationFn: api.cancelOrder, onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['orders'] })
    queryClient.invalidateQueries({ queryKey: ['menu'] })
    queryClient.invalidateQueries({ queryKey: ['active-order'] })
  } })
  return <main className="standard-page"><header className="page-intro"><div><p className="eyebrow">ORDER HISTORY</p><h1>Your meals</h1></div></header>
    {orders.isLoading ? <p className="state-message">Loading orders…</p> : orders.data?.length ? <div className="orders-list">{orders.data.map(order => <article className="order-row" key={order.id}><div className="order-date"><strong>{new Date(`${order.deliveryDate}T12:00:00`).toLocaleDateString('en-US', { month: 'short', day: '2-digit' })}</strong><span>{order.mealPeriod}</span></div><div className="order-main"><div><span className={`status ${order.status.toLowerCase()}`}>{order.status}</span><h2>{order.items.map(item => `${item.quantity}× ${item.dishName}`).join(', ')}</h2><p>{order.deliveryAddress} · {order.orderNumber}</p></div><strong>{money(order.totalAmount)}</strong></div>{order.status === 'Pending' ? <button className="secondary" disabled={cancel.isPending} onClick={() => { if (window.confirm('Cancel this pending order?')) cancel.mutate(order.id) }}>Cancel order</button> : null}</article>)}</div> : <p className="state-message">No orders yet. Today’s menu is ready when you are.</p>}
    {cancel.error ? <p className="form-error">{errorMessage(cancel.error)}</p> : null}
  </main>
}

function PreferencesPage() {
  const queryClient = useQueryClient()
  const preference = useQuery({ queryKey: ['preferences'], queryFn: api.preferences })
  const [form, setForm] = useState<Preference>(EMPTY_PREFERENCE)
  useEffect(() => { if (preference.data) setForm(preference.data) }, [preference.data])
  const save = useMutation({ mutationFn: api.savePreferences, onSuccess: data => queryClient.setQueryData(['preferences'], data) })
  function toggle(field: 'allergens' | 'cuisines', value: string) { setForm(current => ({ ...current, [field]: current[field].includes(value) ? current[field].filter(item => item !== value) : [...current[field], value] })) }
  return <main className="standard-page preferences-page"><header className="page-intro"><div><p className="eyebrow">PERSONAL PREFERENCES</p><h1>Make the menu yours</h1><p>We will warn you about allergens and bring preferred cuisines forward.</p></div></header>
    <section className="preference-section"><div><span className="section-number">01</span><h2>Allergens</h2><p>You will always choose whether to continue.</p></div><div className="choice-grid">{ALLERGENS.map(item => <label className={form.allergens.includes(item) ? 'choice active' : 'choice'} key={item}><input type="checkbox" checked={form.allergens.includes(item)} onChange={() => toggle('allergens', item)} />{item}{form.allergens.includes(item) ? <Check size={16} /> : null}</label>)}</div></section>
    <section className="preference-section"><div><span className="section-number">02</span><h2>Favorite cuisines</h2><p>Used by the For You menu order.</p></div><div className="choice-grid">{CATEGORIES.map(item => <label className={form.cuisines.includes(item) ? 'choice active' : 'choice'} key={item}><input type="checkbox" checked={form.cuisines.includes(item)} onChange={() => toggle('cuisines', item)} />{item}{form.cuisines.includes(item) ? <Check size={16} /> : null}</label>)}</div></section>
    <section className="preference-section"><div><span className="section-number">03</span><h2>Taste & budget</h2><p>Budget is advisory and never blocks checkout.</p></div><div className="preference-form"><label>Spice preference<select value={form.spiceLevel} onChange={event => setForm(current => ({ ...current, spiceLevel: event.target.value }))}><option value="">No preference</option>{['None', 'Mild', 'Medium', 'Hot'].map(item => <option key={item}>{item}</option>)}</select></label><label>Taste<select value={form.tasteIntensity} onChange={event => setForm(current => ({ ...current, tasteIntensity: event.target.value }))}><option value="">No preference</option>{['Light', 'Balanced', 'Rich'].map(item => <option key={item}>{item}</option>)}</select></label><label>Minimum budget<input type="number" min="0" step="0.01" value={form.budgetMin ?? ''} onChange={event => setForm(current => ({ ...current, budgetMin: event.target.value ? Number(event.target.value) : null }))} /></label><label>Maximum budget<input type="number" min="0" step="0.01" value={form.budgetMax ?? ''} onChange={event => setForm(current => ({ ...current, budgetMax: event.target.value ? Number(event.target.value) : null }))} /></label></div></section>
    {save.error ? <p className="form-error">{errorMessage(save.error)}</p> : null}<button className="primary save-preferences" disabled={save.isPending} onClick={() => save.mutate(form)}>{save.isPending ? 'Saving…' : 'Save preferences'}</button>
  </main>
}

function ConsolePage() {
  const queryClient = useQueryClient()
  const [date, setDate] = useState(localDate())
  const [editing, setEditing] = useState<Dish | 'new' | null>(null)
  const [stock, setStock] = useState<Record<number, number>>({})
  const dishes = useQuery({ queryKey: ['admin-dishes'], queryFn: api.adminDishes })
  const menu = useQuery({ queryKey: ['admin-menu', date], queryFn: () => api.adminMenu(date) })
  useEffect(() => {
    if (dishes.data && menu.data) {
      const current = new Map(menu.data.map(item => [item.dishId, item.initialStock]))
      setStock(Object.fromEntries(dishes.data.map(dish => [dish.id, current.get(dish.id) ?? 0])))
    }
  }, [dishes.data, menu.data])
  const status = useMutation({ mutationFn: ({ id, published }: { id: number; published: boolean }) => api.setDishStatus(id, published), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-dishes'] }) })
  const saveMenu = useMutation({ mutationFn: () => api.saveAdminMenu(date, Object.entries(stock).filter(([, value]) => value > 0).map(([dishId, value]) => ({ dishId: Number(dishId), stock: value }))), onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-menu', date] }) })
  return <main className="console-page"><aside className="console-sidebar"><p className="eyebrow">WEBOX CONSOLE</p><h1>Kitchen operations</h1><div className="console-stat"><span>Published dishes</span><strong>{dishes.data?.filter(dish => dish.published).length ?? '—'}</strong></div><div className="console-stat"><span>Menu date</span><strong>{date}</strong></div></aside>
    <section className="console-content"><header className="console-toolbar"><div><p className="eyebrow">DISHES & DAILY SUPPLY</p><h2>Menu control</h2></div><div><label className="date-control"><CalendarDays size={17} /><input type="date" min={localDate()} value={date} onChange={event => setDate(event.target.value)} /></label><button className="primary" onClick={() => setEditing('new')}><Plus size={18} />New dish</button></div></header>
      <div className="table-wrap"><table><thead><tr><th>Dish</th><th>Category</th><th>Price</th><th>Daily stock</th><th>Published</th><th></th></tr></thead><tbody>{dishes.data?.map(dish => <tr key={dish.id}><td><div className="table-dish"><img src={dish.imageUrl} alt="" /><div><strong>{dish.name}</strong><span>{dish.protein}</span></div></div></td><td>{dish.category}</td><td>{money(dish.price)}</td><td><input className="stock-input" type="number" min="0" value={stock[dish.id] ?? 0} onChange={event => setStock(current => ({ ...current, [dish.id]: Number(event.target.value) }))} /></td><td><button className={dish.published ? 'status published' : 'status cancelled'} onClick={() => status.mutate({ id: dish.id, published: !dish.published })}>{dish.published ? 'Published' : 'Hidden'}</button></td><td><button className="secondary compact" onClick={() => setEditing(dish)}>Edit</button></td></tr>)}</tbody></table></div>
      <footer className="console-save"><span>Stock of 0 removes a dish from this date.</span><button className="primary" disabled={saveMenu.isPending} onClick={() => saveMenu.mutate()}>{saveMenu.isPending ? 'Saving…' : 'Save daily menu'}</button></footer>
    </section>
    {editing ? <DishEditor dish={editing === 'new' ? null : editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); queryClient.invalidateQueries({ queryKey: ['admin-dishes'] }) }} /> : null}
  </main>
}

function DishEditor({ dish, onClose, onSaved }: { dish: Dish | null; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState({ name: dish?.name ?? '', description: dish?.description ?? '', price: dish?.price ?? '20.00', category: dish?.category ?? 'Chinese', protein: dish?.protein ?? 'None', allergens: dish?.allergens ?? [], spiceLevel: dish?.spiceLevel ?? 'None', optionGroups: dish?.optionGroups ?? [], imageUrl: dish?.imageUrl ?? '/images/dish-12.jpg' })
  const save = useMutation({ mutationFn: () => dish ? api.updateDish(dish.id, form) : api.createDish(form), onSuccess: onSaved })
  return <div className="modal-backdrop" role="presentation"><form className="dish-editor" onSubmit={event => { event.preventDefault(); save.mutate() }}><header><div><p className="eyebrow">{dish ? 'EDIT DISH' : 'NEW DISH'}</p><h2>{dish?.name ?? 'Create a menu item'}</h2></div><button type="button" className="icon-button" onClick={onClose} aria-label="Close"><X size={20} /></button></header><div className="editor-grid"><label>Name<input required maxLength={120} value={form.name} onChange={event => setForm(current => ({ ...current, name: event.target.value }))} /></label><label>Price<input required type="number" min="0.01" step="0.01" value={form.price} onChange={event => setForm(current => ({ ...current, price: event.target.value }))} /></label><label className="span-two">Description<textarea required maxLength={500} value={form.description} onChange={event => setForm(current => ({ ...current, description: event.target.value }))} /></label><label>Category<select value={form.category} onChange={event => setForm(current => ({ ...current, category: event.target.value }))}>{CATEGORIES.map(item => <option key={item}>{item}</option>)}</select></label><label>Protein<input required value={form.protein} onChange={event => setForm(current => ({ ...current, protein: event.target.value }))} /></label><label>Spice level<select value={form.spiceLevel} onChange={event => setForm(current => ({ ...current, spiceLevel: event.target.value }))}>{['None', 'Mild', 'Medium', 'Hot'].map(item => <option key={item}>{item}</option>)}</select></label><label>Image path<input required value={form.imageUrl} onChange={event => setForm(current => ({ ...current, imageUrl: event.target.value }))} /></label></div>{save.error ? <p className="form-error">{errorMessage(save.error)}</p> : null}<footer><button type="button" className="secondary" onClick={onClose}>Cancel</button><button className="primary" disabled={save.isPending}>{save.isPending ? 'Saving…' : 'Save dish'}</button></footer></form></div>
}
