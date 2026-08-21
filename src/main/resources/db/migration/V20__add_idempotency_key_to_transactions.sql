ALTER TABLE transactions ADD COLUMN idempotency_key VARCHAR(64);

-- Postgres unique constraints treat NULLs as distinct, so existing rows and any
-- transaction-creation path that doesn't send an idempotency key are unaffected.
-- Scoped per order: the same key may only be reused for a different order.
ALTER TABLE transactions
  ADD CONSTRAINT uq_transactions_order_idempotency_key UNIQUE (order_id, idempotency_key);
