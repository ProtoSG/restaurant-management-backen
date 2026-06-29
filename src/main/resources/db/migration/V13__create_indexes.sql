-- orders: queries más frecuentes son por status, fecha y mesa
CREATE INDEX idx_orders_status       ON orders(status);
CREATE INDEX idx_orders_type         ON orders(type);
CREATE INDEX idx_orders_table_id     ON orders(table_id);
CREATE INDEX idx_orders_waiter_id    ON orders(waiter_id);
CREATE INDEX idx_orders_date_created ON orders(date_created);
CREATE INDEX idx_orders_closed_at    ON orders(closed_at);

-- order_items: siempre se accede por order y product
CREATE INDEX idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- transactions: analytics filtra por fecha y status COMPLETED
CREATE INDEX idx_transactions_order_id          ON transactions(order_id);
CREATE INDEX idx_transactions_transaction_date  ON transactions(transaction_date);
CREATE INDEX idx_transactions_status            ON transactions(status);
CREATE INDEX idx_transactions_payment_method    ON transactions(payment_method);

-- products: acceso por category y disponibilidad
CREATE INDEX idx_products_category_id  ON products(category_id);
CREATE INDEX idx_products_is_available ON products(is_available);

-- table_transfer_audit
CREATE INDEX idx_table_transfer_order_id    ON table_transfer_audit(order_id);
CREATE INDEX idx_table_transfer_date        ON table_transfer_audit(transfer_date);

-- refresh_tokens: búsqueda por user en revocación
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
