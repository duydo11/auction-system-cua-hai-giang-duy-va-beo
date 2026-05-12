module com.auction.shared {
    // Thêm dòng này để cho phép dùng các annotation của Lombok (@Data, @NoArgsConstructor,...)
    requires static lombok;

    exports com.auction.shared.constant;
    exports com.auction.shared.model;
    exports com.auction.shared.model.auction;
    exports com.auction.shared.model.item;
    exports com.auction.shared.model.user;
    exports com.auction.shared.protocol;
}