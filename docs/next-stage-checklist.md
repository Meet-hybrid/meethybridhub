# MeethybridHub — Next Stage Checklist

This checklist is for continuing development when the computer is available.
Work through it in small stages because the development PC has limited
resources.

## 1. Store owners

- [ ] MeethybridHub owner email
- [ ] DivinezSignature owner email
- [ ] Decide whether both stores share one owner account or use separate accounts

## 2. Store details

- [ ] Confirm the official store names
- [ ] Confirm unique shop slugs
- [ ] Connect domains or subdomains
- [ ] Add contact email and WhatsApp number
- [ ] Add Lagos delivery information

## 3. Real products

For every product, collect:

- [ ] Product name
- [ ] Description
- [ ] Price
- [ ] Category
- [ ] Variants: size, color, material, length, or finish
- [ ] Stock quantity
- [ ] Product images and image descriptions
- [ ] Care and fit information where relevant

### DivinezSignature catalog

- [ ] Bags
- [ ] Anklets
- [ ] Bracelets
- [ ] Waist beads
- [ ] Necklaces
- [ ] Sets and gifts

### MeethybridHub catalog

- [ ] Clothing
- [ ] Shoes
- [ ] Sneakers
- [ ] Accessories
- [ ] New arrivals
- [ ] Essentials

## 4. Branding and content

For each store:

- [ ] Logo
- [ ] Favicon
- [ ] Primary, secondary, accent, background, and text colors
- [ ] Font and visual style
- [ ] Tagline
- [ ] Home-page copy
- [ ] About-page copy
- [ ] Contact and support details
- [ ] Delivery policy
- [ ] Return and exchange policy
- [ ] Privacy policy
- [ ] Terms and conditions

## 5. Payments and delivery

- [ ] Korapay account and API credentials
- [ ] Confirm supported payment methods
- [ ] Decide whether installments are enabled per store
- [ ] Decide allowed installment periods
- [ ] Define payment, refund, cancellation, and exchange rules
- [ ] Define delivery areas, fees, and estimated delivery times

## 6. Implementation work

- [ ] Create the two store records with unique shop IDs
- [ ] Add categories, products, variants, images, and inventory
- [ ] Connect public catalog APIs
- [ ] Connect the store-aware cart
- [ ] Connect customer registration and login
- [ ] Connect checkout and full payment
- [ ] Connect installment plans and payment schedules
- [ ] Connect order confirmation and order tracking
- [ ] Connect customer account and order history
- [ ] Connect reviews, favorites, and custom orders
- [ ] Correct and verify dashboard API routes
- [ ] Verify that one store cannot access another store's data

## 7. Local development setup

The isolated project database uses:

```text
Host: 127.0.0.1
Port: 55432
Database: meethybridhub
Username: postgres
Password: postgres
```

Start only what is needed to conserve resources:

```bash
# Database
./scripts/db.sh start

# Backend
DB_URL=jdbc:postgresql://127.0.0.1:55432/meethybridhub \
DB_USERNAME=postgres DB_PASSWORD=postgres \
./mvnw spring-boot:run

# Storefront, in a separate terminal when needed
cd storefront && npm run dev
```

Use the storefront at `http://localhost:3000` or with the local store hosts:

- `http://meethybridhub.localhost:3000`
- `http://divinezsignature.localhost:3000`

Do not delete the existing PostgreSQL data folders. Work in small stages and
stop services with `Ctrl+C` when they are not needed.

## Definition of completion

Both stores should independently support brand presentation, catalog browsing,
cart, checkout, full payment, installment payment where enabled, customer
accounts, order tracking, support, and store-owner management. Adding another
store must not change the data or appearance of an existing store.
