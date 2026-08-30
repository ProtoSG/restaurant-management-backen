-- PIN login: an alternative to password login for WAITER/CASHIER on a shared
-- tablet (pick your name from a list, enter a 4-digit PIN — role enforcement
-- and lockout live in AuthServiceImpl, not here). pin_hash is bcrypt, same
-- encoder as password; null means the user has no PIN configured, so PIN
-- login is opt-in per user, set explicitly by an ADMIN via PATCH /users/{id}/pin.
ALTER TABLE users ADD COLUMN pin_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN failed_pin_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN pin_locked_until TIMESTAMP;
