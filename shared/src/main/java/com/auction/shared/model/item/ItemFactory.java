package com.auction.shared.model.item;

import com.auction.shared.model.user.User;

/**
 * Factory Method pattern để tạo Item.
 * 
 * Sử dụng:
 * <pre>
 * Item item = ItemFactory.create("electronics", 1, "Laptop", "Gaming laptop", seller, 24);
 * </pre>
 */
public final class ItemFactory {

    private ItemFactory() {
        // Prevent instantiation
    }

    /**
     * Tạo Item theo type.
     * 
     * @param type "electronics", "art", hoặc "vehicle"
     * @param id ID của item
     * @param name Tên sản phẩm
     * @param description Mô tả
     * @param seller Người bán
     * @param extraParam Tham số bổ sung (warrantyMonths cho Electronics, author cho Art, brand cho Vehicle)
     * @return Item instance phù hợp
     * @throws IllegalArgumentException nếu type không hợp lệ
     */
    public static Item create(String type, int id, String name, String description, User seller, Object extraParam) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Item type cannot be null or blank");
        }

        String normalizedType = type.trim().toLowerCase();

        return switch (normalizedType) {
            case "electronics", "electronic" -> {
                int warrantyMonths = extraParam instanceof Integer ? (Integer) extraParam : 12;
                yield new Electronics(id, name, description, seller, warrantyMonths);
            }
            case "art", "arts" -> {
                String author = extraParam instanceof String ? (String) extraParam : "Unknown";
                yield new Art(id, name, description, seller, author);
            }
            case "vehicle", "vehicles" -> {
                String brand = extraParam instanceof String ? (String) extraParam : "Unknown";
                yield new Vehicle(id, name, description, seller, brand);
            }
            case "other", "others" -> {
                // UI có category Others; dùng Electronics mặc định để vẫn lưu được item thay vì báo lỗi type.
                yield new Electronics(id, name, description, seller, 12);
            }
            default -> throw new IllegalArgumentException("Unknown item type: " + type + 
                    ". Supported types: electronics, art, vehicle, other");
        };
    }

    /**
     * Tạo Item không có extraParam (dùng giá trị default).
     */
    public static Item create(String type, int id, String name, String description, User seller) {
        return create(type, id, name, description, seller, null);
    }

    /**
     * Parse từ chuỗi type + trả về type chuẩn.
     */
    public static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Item type cannot be null or blank");
        }
        return type.trim().toLowerCase();
    }
}
