ALTER TABLE orders
    ADD COLUMN created_by varchar(255) NOT NULL DEFAULT 'system';
ALTER TABLE orders
    ADD COLUMN last_modified_by varchar(255);