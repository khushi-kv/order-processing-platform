-- Runs once, when the Postgres volume is first created.
-- One database per service; notification-service has none.
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
