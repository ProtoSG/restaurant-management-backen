# Restaurant Management — Backend

API REST para sistema de gestión de restaurante peruano.

## Stack

- **Java 21**
- **Spring Boot 3.5**
- **Spring Security** + JWT
- **PostgreSQL**
- **Lombok**
- **Maven**

## Módulos

- **Auth** — registro, login, JWT
- **Orders** — CRUD de pedidos, items, cambio de tipo/mesa, cancelación
- **Tables** — gestión de mesas, estado (libre/ocupada), pedido activo por mesa
- **Menu** — productos y categorías
- **Analytics** — métricas de ventas, balance, productos top, transacciones
- **Thermal Printer** — impresión de precuenta en impresora térmica

## Roles

| Rol | Permisos |
|-----|---------|
| `ADMIN` | Acceso total |
| `CASHIER` | Pedidos, mesas, pagos |
| `WAITER` | Pedidos, mesas (sin pago) |
| `CHEF` | Ver pedidos, marcar como listo |

## Flujo de estado de pedidos

```
CREATED → IN_PROGRESS → READY → PAID
                              ↘ PARTIALLY_PAID → PAID
         CANCELLED
```

- `CREATED` → orden sin items
- `IN_PROGRESS` → automático al agregar primer item
- `READY` → marcado por ADMIN o CHEF (`POST /orders/{id}/ready`)
- Solo se puede pagar en estado `CREATED`, `READY` o `PARTIALLY_PAID`

## Instalación

```bash
./mvnw spring-boot:run
```

Requiere PostgreSQL corriendo. Configurar credenciales en `application-dev.properties`.

## Variables de entorno principales

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/restaurant_db
spring.datasource.username=...
spring.datasource.password=...
jwt.secret=...
```

## Endpoints principales

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/auth/login` | Login |
| `GET` | `/orders/active` | Pedidos activos |
| `POST` | `/orders/{id}/items` | Agregar item |
| `POST` | `/orders/{id}/ready` | Marcar como listo |
| `POST` | `/orders/{id}/pay/{method}` | Pagar total |
| `POST` | `/orders/{id}/pay-partial` | Pago parcial |
| `GET` | `/tables` | Lista de mesas |
| `GET` | `/analytics/dashboard` | Métricas generales |
