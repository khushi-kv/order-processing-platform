CREATE TABLE products (
    id         BIGSERIAL      PRIMARY KEY,
    name       VARCHAR(150)   NOT NULL,
    sku        VARCHAR(64)    NOT NULL UNIQUE,
    -- NUMERIC, not floating point: money must be exact
    price      NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    -- The database refuses negative stock even if the code has a bug
    stock      INTEGER        NOT NULL CHECK (stock >= 0),
    -- Optimistic locking (@Version): an update fails if another order changed the row first
    version    BIGINT         NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    -- Soft delete: NULL means active; set when an admin deletes the product.
    -- The row stays because reservations and orders still point to it.
    deleted_at TIMESTAMPTZ
);

-- What each order holds, so a cancelled order gives back exactly what it took
CREATE TABLE stock_reservations (
    id         BIGSERIAL      PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    product_id BIGINT         NOT NULL REFERENCES products (id),
    quantity   INTEGER        NOT NULL CHECK (quantity > 0),
    -- Price frozen at reservation time; later price changes do not affect this order
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price > 0),
    status     VARCHAR(20)    NOT NULL CHECK (status IN ('RESERVED', 'RELEASED')),
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    -- One line per product per order; also the index for looking up an order's lines
    UNIQUE (order_id, product_id)
);
