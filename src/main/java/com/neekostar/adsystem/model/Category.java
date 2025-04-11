package com.neekostar.adsystem.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    ELECTRONICS,
    FURNITURE,
    CLOTHING,
    BOOKS,
    SPORTS,
    TOYS,
    VEHICLES,
    SERVICES,
    JOBS,
    REAL_ESTATE,
    OTHER;

    @JsonCreator
    public static Category fromString(String key) {
        if (key == null) {
            return null;
        }
        for (Category category : Category.values()) {
            if (category.name().equalsIgnoreCase(key)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Invalid category: " + key);
    }

    @JsonValue
    public String getValue() {
        return this.name();
    }
}
