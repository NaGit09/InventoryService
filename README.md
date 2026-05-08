# Inventory Service

The Inventory Service is responsible for managing product stock, handling reservations during the order lifecycle, and tracking stock movements through transactions. It integrates with other services via Kafka events to ensure inventory consistency across the microservices architecture.

## Tech Stack
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL / MySQL (JPA/Hibernate)
- **Messaging**: Apache Kafka
- **Documentation**: SpringDoc OpenAPI (Swagger)

---

## API Endpoints

### Admin Stock Management
Endpoints for administrative operations. Base path: `/admin/stock`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/create` | Create a new stock record for a variant. |
| `PUT` | `/update` | Manually update stock (IN/OUT). |
| `DELETE` | `/delete/{stockId}` | Delete a stock record (only if quantity is 0). |
| `GET` | `/{sku}` | Get full stock details for a specific SKU. |
| `GET` | `/all` | List all stocks (paginated). |
| `GET` | `/statistic` | Get overall inventory statistics (Total, Reserved, Low Stock). |
| `GET` | `/low-stock` | List items currently below their low stock threshold. |
| `GET` | `/transactions` | Retrieve stock transaction logs (filtered by SKU). |

### Public Stock Information
Read-only endpoints for the storefront. Base path: `/public/stock`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/available/{sku}` | Get only the available quantity for a SKU. |
| `GET` | `/details/{sku}` | Get detailed stock information for a SKU. |

---

## Kafka Events

### Subscribed Topics (Consumers)
The service listens for these events to manage the reservation lifecycle.

| Topic | Source | Action |
| :--- | :--- | :--- |
| `order.created` | Order Service | Reserves stock for items in the order. |
| `payment.completed` | Payment Service | Commits reservations and deducts total inventory. |
| `order.cancelled` | Order Service | Releases reserved stock back to available pool. |
| `inventory.restock` | Admin/Warehouse | Increases stock levels from new shipments. |
| `inventory.adjustment` | Admin/Warehouse | Manual corrections for shrinkage or audits. |
| `reservation.expiry-check` | Scheduler | Triggers cleanup of stale PENDING reservations. |

### Published Topics (Producers)
Events emitted by this service to notify other parts of the system.

| Topic | Destination | Description |
| :--- | :--- | :--- |
| `inventory.reserved` | Order Service | Outcome of reservation attempt (`SUCCESS` or `FAILED`). |
| `inventory.low-stock` | Notification Service | Alert when stock levels fall below threshold. |
| `inventory.reservation-expired` | Order Service | Notification that an order's stock reservation has timed out. |

---

## Stock Model
- **Total Quantity**: Total units physically present in the warehouse.
- **Reserved Quantity**: Units held for pending orders (not available for new sales).
- **Available Quantity**: Units ready for sale (`Total - Reserved`).
- **Low Stock Threshold**: User-defined limit that triggers a `low-stock` alert.

---

## Transaction Types
All stock movements are recorded as transactions:
- `IN`: Initial stock entry or manual addition.
- `OUT`: Manual reduction.
- `SALE`: Stock deducted after successful payment.
- `RESTOCK`: Arrival of new goods.
- `ADJUST`: Audit corrections.
- `RETURN`: (Future) Customer returns.
