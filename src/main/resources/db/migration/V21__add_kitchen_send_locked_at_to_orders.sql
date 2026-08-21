-- Advisory lock timestamp to prevent two concurrent "send to kitchen" attempts
-- on the same order from both printing the same pending delta. See
-- OrderServiceImpl#getKitchenPending / #confirmKitchen: the lock is set when a
-- print attempt starts and cleared when it's confirmed, with a short TTL so a
-- failed/abandoned attempt self-heals without a manual unlock endpoint.
ALTER TABLE orders ADD COLUMN kitchen_send_locked_at TIMESTAMP;
