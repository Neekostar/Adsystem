package com.neekostar.adsystem.specification;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.neekostar.adsystem.model.Ad;
import com.neekostar.adsystem.model.Category;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

public class AdSpecification {

    public static @NotNull Specification<Ad> hasCity(String city) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("city"), city);
    }

    public static @NotNull Specification<Ad> hasCategory(Category category) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category"), category);
    }

    public static @NotNull Specification<Ad> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("price"), minPrice, maxPrice);
    }

    public static @NotNull Specification<Ad> containsKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
            Predicate descriptionPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern);
            return criteriaBuilder.or(titlePredicate, descriptionPredicate);
        };
    }

    public static @NotNull Specification<Ad> combineSpecifications(String city,
                                                                   Category category,
                                                                   BigDecimal minPrice,
                                                                   BigDecimal maxPrice,
                                                                   String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (city != null && !city.trim().isEmpty()) {
                predicates.add(hasCity(city).toPredicate(root, query, criteriaBuilder));
            }

            if (category != null) {
                predicates.add(hasCategory(category).toPredicate(root, query, criteriaBuilder));
            }

            if (minPrice != null && maxPrice != null) {
                predicates.add(priceBetween(minPrice, maxPrice).toPredicate(root, query, criteriaBuilder));
            } else if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            } else if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(containsKeyword(keyword).toPredicate(root, query, criteriaBuilder));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
