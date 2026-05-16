-- Schema H2 in-memory dùng cho test (tương thích Java code dùng INT id)
-- Khác với db.sql (dùng varchar(255) cho id)

CREATE TABLE IF NOT EXISTS users (
    id INT NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS bidders (
    user_id INT NOT NULL,
    account_balance DOUBLE DEFAULT 0,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS sellers (
    user_id INT NOT NULL,
    rating DOUBLE DEFAULT 0,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS admins (
    user_id INT NOT NULL,
    access_level VARCHAR(50) DEFAULT NULL,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS items (
    id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    seller_id INT DEFAULT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS electronics (
    item_id INT NOT NULL,
    warranty_months INT DEFAULT NULL,
    PRIMARY KEY (item_id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS arts (
    item_id INT NOT NULL,
    author VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (item_id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS vehicles (
    item_id INT NOT NULL,
    brand VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (item_id),
    FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE IF NOT EXISTS auction_sessions (
    id INT NOT NULL,
    item_id INT NOT NULL,
    seller_id INT NOT NULL,
    winner_id INT DEFAULT NULL,
    starting_price DOUBLE NOT NULL,
    current_price DOUBLE NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (winner_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS bids (
    id INT NOT NULL,
    bidder_id INT NOT NULL,
    auction_session_id INT NOT NULL,
    amount DOUBLE NOT NULL,
    time DATETIME NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (bidder_id) REFERENCES users(id),
    FOREIGN KEY (auction_session_id) REFERENCES auction_sessions(id)
);
