module com.auction.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.logging;
    requires com.auction.shared;

    opens com.auction.client.controller to javafx.fxml;
    opens com.auction.client to javafx.fxml;

    exports com.auction.client;
}
