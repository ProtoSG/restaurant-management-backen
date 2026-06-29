ALTER TABLE order_items
    ADD COLUMN kitchen_printed_quantity INT NOT NULL DEFAULT 0;

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_kitchen_printed CHECK (kitchen_printed_quantity >= 0);
