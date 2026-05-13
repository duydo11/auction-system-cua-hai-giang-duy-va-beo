module com.auction.server {
    requires transitive com.auction.shared;
    requires java.logging;
    requires java.sql;

    exports com.auction.server;
    exports com.auction.server.service;
    exports com.auction.server.network;
    exports com.auction.server.dao;
}
