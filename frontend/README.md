# Frontend

This directory contains one responsive React and TypeScript SPA with two role-aware shells:

- Employee: menu, dish details, cart, checkout, orders, preferences, and optional AI assistant.
- Console: dish management, daily-menu scheduling, and optional operations dashboard.

The frontend owns presentation, accessible interaction, local cart state, server-state synchronization, form validation feedback, and responsive behavior. The backend remains authoritative for permissions, prices, cut-off rules, duplicate-order protection, and inventory.

Unit and component tests live beside source files. All rendered product copy is English.

```bash
npm ci
npm run typecheck
npm test
npm run build
npm run dev -- --host 127.0.0.1
```
