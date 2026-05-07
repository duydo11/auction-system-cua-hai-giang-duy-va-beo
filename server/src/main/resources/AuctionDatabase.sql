-- Tạo Database (Nếu chưa có)
CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- 1. BẢNG CHA: Users
CREATE TABLE users (
                       id VARCHAR(255) PRIMARY KEY,
                       username VARCHAR(255) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL
);

-- 2. BẢNG CON CỦA USERS: Bidders & Sellers
CREATE TABLE bidders (
                         user_id VARCHAR(255) PRIMARY KEY,
                         account_balance DOUBLE,
                         FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE sellers (
                         user_id VARCHAR(255) PRIMARY KEY,
                         rating DOUBLE,
                         FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE admins (
                        user_id VARCHAR(255) PRIMARY KEY,
                        access_level VARCHAR(50),
                        FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 3. BẢNG CHA: Items
CREATE TABLE items (
                       id VARCHAR(255) PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       description TEXT,
                       seller_id VARCHAR(255),
                       FOREIGN KEY (seller_id) REFERENCES users(id)
);

-- 4. BẢNG CON CỦA ITEMS: Vehicles, Electronics, Arts
CREATE TABLE vehicles (
                          item_id VARCHAR(255) PRIMARY KEY,
                          brand VARCHAR(255),
                          FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE electronics (
                             item_id VARCHAR(255) PRIMARY KEY,
                             warranty_months INT,
                             FOREIGN KEY (item_id) REFERENCES items(id)
);

CREATE TABLE arts (
                      item_id VARCHAR(255) PRIMARY KEY,
                      author VARCHAR(255),
                      FOREIGN KEY (item_id) REFERENCES items(id)
);

-- 5. BẢNG GIAO DỊCH: Auction Sessions
CREATE TABLE auction_sessions (
                                  id VARCHAR(255) PRIMARY KEY,
                                  item_id VARCHAR(255) NOT NULL,
                                  seller_id VARCHAR(255) NOT NULL,
                                  winner_id VARCHAR(255),  -- Có thể NULL vì lúc mở phiên chưa có người thắng
                                  starting_price DOUBLE NOT NULL,
                                  current_price DOUBLE NOT NULL,
                                  start_time DATETIME NOT NULL,
                                  end_time DATETIME NOT NULL,
                                  FOREIGN KEY (item_id) REFERENCES items(id),
                                  FOREIGN KEY (seller_id) REFERENCES users(id),
                                  FOREIGN KEY (winner_id) REFERENCES users(id)
);

-- 6. BẢNG CHI TIẾT: Bids (Lịch sử trả giá)
CREATE TABLE bids (
                      id VARCHAR(255) PRIMARY KEY,
                      bidder_id VARCHAR(255) NOT NULL,
                      auction_session_id VARCHAR(255) NOT NULL,
                      amount DOUBLE NOT NULL,
                      time DATETIME NOT NULL,
                      FOREIGN KEY (bidder_id) REFERENCES users(id),
                      FOREIGN KEY (auction_session_id) REFERENCES auction_sessions(id)
);