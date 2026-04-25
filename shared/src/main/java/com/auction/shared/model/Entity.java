package com.auction.shared.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor// Cần thiết cho các thư viện Hibernate/JSON sau này
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;

    // Viết tay constructor này để các class con gọi super(id) không bị lỗi
    public Entity(String id) {
        this.id = id;
    }
}