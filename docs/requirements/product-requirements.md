# WeBox Requirements Baseline

## 1. Purpose and source

This document converts `参考资料/AI Vibe Coding V3.0 - PRD.md` into an implementation baseline. The source PRD remains authoritative if wording conflicts. The HTML file is a reading copy and adds no product requirements.

WeBox serves approximately 200 employees on mobile and desktop, with a concentrated ordering peak from 09:30 to 10:00. The initial catalog is approximately 50 dishes and is expected to grow.

## 2. Non-negotiable constraints

- Deliver a working frontend and backend together.
- Use Java 17 and Spring Boot for the backend.
- Use a separately running MySQL instance; embedded databases are not accepted.
- Render every user-facing string and every stored/displayed menu value in English.
- Persist passwords only as strong adaptive hashes.
- Preserve raw AI coding conversation exports under `ai-conversations/`.
- Treat server time in the configured business timezone (`Asia/Shanghai` by default).
- Represent currency as CNY with decimal-safe server and client types.

## 3. Roles and authorization

### Employee

An employee can register and sign in, browse the daily menu, search and filter dishes, inspect and customize a dish, manage a cart, place an order, view order history and details, cancel a pending order, and update dietary preferences.

### Administrator

An administrator signs into the same system but enters the Console shell. The administrator can search, create, edit, publish/unpublish dishes, upload dish images, select dishes for a date, and configure daily supply. Employee accounts must be denied all Console APIs as well as Console routes.

The source introduction mentions “processing orders,” but Tier 2 only specifies dish and daily-menu management. Admin order processing is therefore an explicitly tracked extension, not a hidden baseline requirement.

## 4. Functional baseline

### 4.1 Authentication

- Registration accepts a valid email from any domain and rejects duplicate emails.
- Passwords contain at least eight characters, including a letter and a digit.
- Email and address inputs are at most 200 characters; search input is at most 50 characters.
- Authentication survives a browser refresh and expires predictably.
- Errors such as duplicate email and invalid credentials are explicit without leaking account-sensitive detail.

### 4.2 Daily menu and dish details

- The default authenticated employee route displays the menu for the effective order date.
- Cards show image, English name, exact CNY price, and English category.
- Employees can multi-select categories and search English name or description.
- Dish details show description, protein source, allergens, spice level, and customization groups.
- Required option groups block adding until selected. Optional add-ons may change price.
- The displayed configured price updates immediately and is recalculated authoritatively by the server at order time.
- Unpublished dishes never appear to employees.

### 4.3 Cart

- Repeatedly adding the same dish with the same options increments quantity.
- Different option combinations remain distinct cart lines.
- Quantity can increase, decrease, or be removed, and total price updates immediately.
- Total quantity across all lines cannot exceed five.
- Add controls become disabled with an English reason at the five-item limit.
- Adding a dish containing a flagged allergen requires explicit confirmation; it is not silently filtered.

Cart persistence across browser refresh is a product-quality default for this implementation, while the backend remains authoritative at checkout.

### 4.4 Checkout and orders

- Checkout shows immutable-looking line summaries, selected options, quantities, subtotals, and total.
- An employee selects delivery date, `Lunch` or `Dinner`, and an existing or new address.
- Lunch cutoff is 10:00; dinner cutoff is 15:00 in the business timezone.
- An unavailable selection is normalized to same-day dinner if still open, otherwise next-day lunch.
- One user may have only one effective (`Pending` or `Confirmed`) order for a date and meal slot.
- Repeated submissions with the same idempotency key create exactly one order and return the original result.
- Order prices and dish descriptions are snapshotted so history does not change after catalog edits.
- Successful submission returns an order number, date, meal period, and address.
- Employees can list and inspect their own orders only.
- Only a `Pending` order can be cancelled. Cancellation is transactional and, when inventory is enabled, restores stock exactly once.

### 4.5 Preferences and recommendation ordering

- Allergens use the fixed set `Peanuts`, `Dairy`, `Egg`, `Gluten`, `Soy`, `Fish`, and `Shellfish`.
- Cuisine preferences use the catalog category enumeration.
- Spice preferences map to `None`, `Mild`, `Medium`, and `Hot`.
- Taste-intensity preference is stored for AI personalization and need not hard-sort the menu.
- A meal budget has a minimum and maximum. Checkout warns above the maximum but does not block.
- The `For You` toggle prioritizes and highlights preference matches; off restores deterministic default order.

### 4.6 Console dish and daily-menu management

- The dish table supports search and category filter.
- Create and edit validate name, description, exact price, category, allergen tags, and image.
- Publish status controls employee visibility.
- Daily-menu setup defaults to tomorrow but permits today for demonstration and operational correction.
- Supply quantity is recorded in Tier 2 even if live employee stock display and transactional deduction are deferred to Tier 3.

## 5. Tier 3 extensions

### Inventory and live updates

Daily inventory cannot become negative under concurrent checkout. Submission locks and revalidates all requested stock in one database transaction. Insufficient items are returned explicitly. Remaining stock is pushed to active menu clients, sold-out items cannot be ordered, and quantities from one to three receive a low-stock treatment.

### AI recommendations

The assistant accepts English natural language, combines preferences, allergens, in-stock menu data, and the previous seven days of orders, and returns dish-card recommendations with English reasons. Allergen matches and sold-out dishes are removed before prompting and revalidated after model output. The response streams progressively and the feature fails gracefully when no provider is configured.

### Operations dashboard

Console metrics come from real orders: today’s totals and statuses, top ten dishes, lunch versus dinner, seven-day order/revenue trends, and inventory from zero to three. Data can auto-refresh or expose a clear manual refresh action.

## 6. Cross-cutting requirements

- Mobile and desktop layouts support the complete employee flow; Console remains usable at common laptop widths.
- Menu rendering and filtering remain responsive at and beyond 50 items.
- Search uses parameterized persistence APIs and normalized query handling.
- Rate limits and UI request coalescing protect high-frequency actions without losing intentional quantity changes.
- APIs return stable error codes plus English user-safe messages.
- Logs include correlation identifiers and exclude credentials, password material, and sensitive tokens.
- Health checks distinguish application liveness from MySQL readiness.
- Core flows are covered by automated unit, integration, and end-to-end tests.

## 7. Clarifications fixed by this baseline

These decisions remove implementation ambiguity without changing the requested outcome:

- The configured business timezone controls cutoff calculations; client clocks are informational only.
- The server always recalculates price, eligibility, duplicate-slot state, and inventory.
- Product images require an explicit asset manifest; numeric filenames do not imply a dish mapping.
- Historical orders use snapshots rather than live catalog joins for displayed commercial facts.
- Redis is not part of the baseline. It may be introduced later only with a measured need and documented consistency rules.
- AI is isolated behind a provider interface and feature flag so missing credentials do not break Tier 1 or Tier 2.

## 8. Open product decisions

- Whether employees may choose any future date or only today/next available meal.
- Whether a `Confirmed` order can be cancelled by an administrator.
- Which administrator order-processing states and actions are required beyond dish management.
- Whether uploaded images use local persistent storage, an S3-compatible service, or a production object store.
- Session duration, account recovery, and email verification policy.

Until clarified, implementations must use conservative defaults documented in the relevant ADR and keep these behaviors configurable where inexpensive.
